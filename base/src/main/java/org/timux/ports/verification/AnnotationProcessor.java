package org.timux.ports.verification;

import org.timux.ports.Queue;
import org.timux.ports.*;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import java.util.*;

public class AnnotationProcessor extends AbstractProcessor {

    private final static String EVENT_TYPE = Event.class.getName();
    private final static String REQUEST_TYPE = Request.class.getName();

    private final static String STACK_TYPE = org.timux.ports.Stack.class.getName();
    private final static String QUEUE_TYPE = Queue.class.getName();

    private Reporter reporter;
    private VerificationModel verificationModel;
    private MethodCheckerVisitor methodCheckerVisitor;

    private final Set<String> unmodifiableSupportedAnnotationTypes;

    {
        Set<String> supportedAnnotationTypes = new HashSet<>();

        supportedAnnotationTypes.add(In.class.getName());
        supportedAnnotationTypes.add(Out.class.getName());
        supportedAnnotationTypes.add(Response.class.getName());

        unmodifiableSupportedAnnotationTypes = Collections.unmodifiableSet(supportedAnnotationTypes);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return unmodifiableSupportedAnnotationTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.reporter = new Reporter(processingEnv);
        this.verificationModel = new VerificationModel(reporter);
        this.methodCheckerVisitor = new MethodCheckerVisitor(reporter, verificationModel);
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) {
            return true;
        }

        checkOutPort(roundEnvironment);
        checkInPorts(roundEnvironment);
        checkRequestTypes(roundEnvironment);

        return true;
    }

    private void checkOutPort(RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Out.class)) {
            String portType = element.asType().toString();
            String portName = element.getSimpleName().toString();
            String portSignature = extractTypeParameter(portType, Object.class.getName());

            String messageType = portSignature.indexOf(',') >= 0
                    ? portSignature.substring(0, portSignature.indexOf(','))
                    : portSignature;

            String returnType = portSignature.indexOf(',') >= 0
                    ? portSignature.substring(portSignature.indexOf(',') + 1)
                    : void.class.getName();

            if (element.getModifiers().contains(Modifier.STATIC)) {
                reporter.reportIssue(element, "OUT port [%s] must not be static", portName);
            }

            if (!portType.startsWith(EVENT_TYPE + "<")
                    && !portType.equals(EVENT_TYPE)
                    && !portType.startsWith(REQUEST_TYPE + "<")
                    && !portType.equals(REQUEST_TYPE))
            {
                reporter.reportIssue(element, "type '%s' is not a valid OUT port type", portType);
                return;
            }

            if (returnType.equals(Void.class.getName())) {
                reporter.reportIssue(element, "OUT port [%s] has inadmissible return type (%s)", portName, returnType);
            } else {
                verificationModel.verifyAndRegisterResponseType(messageType, returnType, portName, element);
            }

            if (!messageType.equals(Object.class.getName())
                    && portType.startsWith(EVENT_TYPE)
                    && !messageType.endsWith("Event")
                    && !messageType.endsWith("Exception"))
            {
                String commandNote = messageType.endsWith("Command")
                        ? " (commands should be implemented via request ports)"
                        : "";

                reporter.reportIssue(element, "'%s' is not a valid event type%s", messageType, commandNote);
            } else {
                if (portType.startsWith(EVENT_TYPE) && (messageType.endsWith("Event") || messageType.endsWith("Exception"))) {
                    String correctName = PortNamer.toOutPortName(messageType);

                    if (!portName.equals(correctName)) {
                        reporter.reportIssue(element, "'%s' is not a valid OUT port name (should be '%s')", portName, correctName);
                    }
                }
            }

            if (portType.startsWith(REQUEST_TYPE) && !(messageType.endsWith("Request") || messageType.endsWith("Command"))) {
                reporter.reportIssue(element, "'%s' is not a valid request type", messageType);
            } else {
                if (portType.startsWith(REQUEST_TYPE) && (messageType.endsWith("Request") || messageType.endsWith("Command"))) {
                    String correctName = PortNamer.toOutPortName(messageType);

                    if (!portName.equals(correctName)) {
                        reporter.reportIssue(element, "'%s' is not a valid OUT port name (should be '%s')", portName, correctName);
                    }
                }
            }
        }
    }

    private void checkInPorts(RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(In.class)) {
            String portType = element.asType().toString();
            String portName = element.getSimpleName().toString();

            if (element.getModifiers().contains(Modifier.STATIC)) {
                reporter.reportIssue(element, "IN port [%s] must not be static", portName);
            }

            if (!element.getKind().isField()) {
                element.accept(methodCheckerVisitor, null);
                continue;
            }

            if (!portType.startsWith(STACK_TYPE + "<")
                    && !portType.equals(STACK_TYPE)
                    && !portType.startsWith(QUEUE_TYPE + "<")
                    && !portType.equals(QUEUE_TYPE))
            {
                reporter.reportIssue(element, "type '%s' is not a valid IN port type", portType);
            }
        }
    }

    private void checkRequestTypes(RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Responses.class)) {
            for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                if (mirror.getAnnotationType().toString().equals(Responses.class.getName())) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : mirror.getElementValues().entrySet()) {
                        if ("value".equals(e.getKey().getSimpleName().toString())) {
                            processResponsesString(e.getValue().toString(), element, mirror);
                        }
                    }
                }
            }
        }

        for (Element element : roundEnvironment.getElementsAnnotatedWith(Response.class)) {
            for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                if (mirror.getAnnotationType().toString().equals(Response.class.getName())) {
                    processResponseAnnotatedElement(element, mirror);
                }
            }
        }
    }

    private void processResponsesString(String responsesString, Element element, AnnotationMirror mirror) {
        String messageType = element.toString();
        String[] parts = responsesString.split("Response\\(");
        List<String> responseTypes = new ArrayList<>();

        Arrays.stream(parts)
                .skip(1)
                .forEach(part -> responseTypes.add(part.substring(0, part.indexOf(".class)"))));

        for (String responseType : responseTypes) {
            if (responseType.equals(Void.class.getName())) {
                reporter.reportIssue(element, mirror, "message type '%s' has inadmissible return type (%s)", messageType, responseType);
            }
        }

        if (responseTypes.size() > 3) {
            reporter.reportIssue(element, mirror, "too many response types for message type '%s' (max. 3 allowed)", messageType);
            return;
        }

        String eitherArguments = responseTypes.stream()
                .reduce((xs, x) -> xs + "," + x)
                .orElseThrow(IllegalStateException::new);

        String responseType = String.format("%s<%s>",
                responseTypes.size() == 2 ? Either.class.getName() : Either3.class.getName(),
                eitherArguments);

        verificationModel.verifyAndRegisterResponseType(messageType, responseType, element, mirror);
    }

    private void processResponseAnnotatedElement(Element element, AnnotationMirror mirror) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : mirror.getElementValues().entrySet()) {
            if ("value".equals(e.getKey().getSimpleName().toString())) {
                String messageType = element.toString();
                String responseType = e.getValue().toString().substring(0, e.getValue().toString().lastIndexOf('.'));

                if (responseType.equals(Void.class.getName())) {
                    reporter.reportIssue(element, mirror, "message type '%s' has inadmissible return type (%s)", messageType, responseType);
                    continue;
                }

                verificationModel.verifyAndRegisterResponseType(messageType, responseType, element, mirror);
            }
        }
    }

    private static String extractTypeParameter(String type, String _default) {
        int genericStart = type.indexOf('<');
        int genericEnd = type.lastIndexOf('>');

        return genericStart < 0
                ? _default
                : type.substring(genericStart + 1, genericEnd);
    }
}
