/*
 * Copyright 2018-2021 Tim Rohlfs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.timux.ports.verification;

import org.timux.ports.Event;
import org.timux.ports.In;
import org.timux.ports.Out;
import org.timux.ports.Pure;
import org.timux.ports.QueuePort;
import org.timux.ports.Request;
import org.timux.ports.Response;
import org.timux.ports.Responses;
import org.timux.ports.StackPort;
import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class AnnotationProcessor extends AbstractProcessor {

    private final static String EVENT_TYPE = Event.class.getName();
    private final static String REQUEST_TYPE = Request.class.getName();

    private final static String STACK_TYPE = StackPort.class.getName();
    private final static String QUEUE_TYPE = QueuePort.class.getName();

    private Reporter reporter;
    private VerificationModel verificationModel;
    private MethodCheckerVisitor methodCheckerVisitor;

    private final Set<String> unmodifiableSupportedAnnotationTypes;

    {
        Set<String> supportedAnnotationTypes = new HashSet<>();

        supportedAnnotationTypes.add(In.class.getName());
        supportedAnnotationTypes.add(Out.class.getName());
        supportedAnnotationTypes.add(Response.class.getName());
        supportedAnnotationTypes.add(Pure.class.getName());

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
                reporter.reportIssue(element, "OUT port [%s] has inadmissible response type (%s)", portName, returnType);
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

                reporter.reportIssue(element,
                        "'%s' is not a valid event type%s", messageType, commandNote);
            } else {
                if (portType.startsWith(EVENT_TYPE) && (messageType.endsWith("Event") || messageType.endsWith("Exception"))) {
                    String correctName = PortNamer.toOutPortName(messageType);

                    if (!portName.equals(correctName)) {
                        reporter.reportIssue(element,
                                "'%s' is not a valid OUT port name (should be '%s')", portName, correctName);
                    }
                }
            }

            if (portType.startsWith(REQUEST_TYPE) && !(messageType.endsWith("Request") || messageType.endsWith("Command"))) {
                reporter.reportIssue(element, "'%s' is not a valid request type", messageType);
            } else {
                if (portType.startsWith(REQUEST_TYPE) && (messageType.endsWith("Request") || messageType.endsWith("Command"))) {
                    String correctName = PortNamer.toOutPortName(messageType);

                    if (!portName.equals(correctName)) {
                        reporter.reportIssue(element,
                                "'%s' is not a valid OUT port name (should be '%s')", portName, correctName);
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
        forEachAnnotatedElementDo(roundEnvironment, Responses.class, this::processMultipleResponsesAnnotatedElement);
        forEachAnnotatedElementDo(roundEnvironment, Response.class, this::processSingleResponseAnnotatedElement);
        forEachAnnotatedElementDo(roundEnvironment, Pure.class, this::processPureAnnotatedElement);
    }

    private void processMultipleResponsesAnnotatedElement(Element element, AnnotationMirror mirror) {
        String responsesString = getMirrorValue(mirror);

        String messageType = element.toString();
        String[] parts = responsesString.split("Response\\(");
        List<String> responseTypes = new ArrayList<>();

        Arrays.stream(parts)
                .skip(1)
                .forEach(part -> responseTypes.add(part.substring(0, part.indexOf(".class)"))));

        for (String responseType : responseTypes) {
            if (responseType.equals(Void.class.getName())) {
                reporter.reportIssue(element, mirror,
                        "message type '%s' has inadmissible response type (%s)", messageType, responseType);
            }
        }

        if (responseTypes.size() < 2) {
            reporter.reportIssue(element, mirror,
                    "too few response types for message type '%s' (min. 2 required)", messageType);
            return;
        }

        if (responseTypes.size() > 3) {
            reporter.reportIssue(element, mirror,
                    "too many response types for message type '%s' (max. 3 allowed)", messageType);
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

    private void processSingleResponseAnnotatedElement(Element element, AnnotationMirror mirror) {
        String mirrorValue = getMirrorValue(mirror);

        String messageType = element.toString();
        String responseType = mirrorValue.substring(0, mirrorValue.lastIndexOf('.'));

        if (responseType.equals(Void.class.getName())) {
            reporter.reportIssue(element, mirror, "message type '%s' has inadmissible response type (%s)", messageType, responseType);
            return;
        }

        verificationModel.verifyAndRegisterResponseType(messageType, responseType, element, mirror);
    }

    private void processPureAnnotatedElement(Element element, AnnotationMirror mirror) {
        String messageType = element.toString();

        if (!messageType.endsWith("Request")) {
            reporter.reportIssue(element, mirror, "message type '%s' cannot be pure", messageType);
        }

        String cacheValue = getMirrorValue("cache", mirror);
        boolean isCacheEnabled = cacheValue == null || Boolean.parseBoolean(cacheValue);

        if (isCacheEnabled) {
            boolean foundState = false;
            boolean foundEquals = false;
            boolean foundHashCode = false;

            for (Element e : element.getEnclosedElements()) {
                Set<Modifier> modifiers = e.getModifiers();

                foundState |= e.getKind().isField() && !(modifiers.contains(Modifier.STATIC) && modifiers.contains(Modifier.FINAL));
                foundEquals |= e.getSimpleName().toString().equals("equals") && e.asType().toString().equals("(java.lang.Object)boolean");
                foundHashCode |= e.getSimpleName().toString().equals("hashCode") && e.asType().toString().equals("()int");
            }

            if (foundState && !(foundEquals && foundHashCode)) {
                reporter.reportIssue(element, mirror,
                        "message type '%s' is stateful and declared pure but does not implement both equals and hashCode",
                        messageType);
            }
        }

        String clearCacheOnValue = getMirrorValue("clearCacheOn", mirror);
        List<String> clearCacheOnTypes = splitArrayMirrorValue(clearCacheOnValue);

        clearCacheOnTypes.forEach(type -> {
            if (!(type.endsWith("Event.class")
                    || type.endsWith("Request.class")
                    || type.endsWith("Command.class")
                    || type.endsWith("Exception.class")))
            {
                reporter.reportIssue(element, mirror,
                        "invalid type '%s': all provided types must be message types (events, requests, commands, or exceptions)",
                        type.substring(0, type.length() - ".class".length()));
            }
        });
    }

    private void forEachAnnotatedElementDo(
            RoundEnvironment roundEnvironment, Class<? extends Annotation> annotation, BiConsumer<Element, AnnotationMirror> action)
    {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(annotation)) {
            for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                if (mirror.getAnnotationType().toString().equals(annotation.getName())) {
                    action.accept(element, mirror);
                }
            }
        }
    }

    private String getMirrorValue(AnnotationMirror mirror) {
        String value = getMirrorValue("value", mirror);

        if (value == null) {
            throw new IllegalStateException("annotation value must not be empty (" + mirror.getAnnotationType().toString() + ")");
        }

        return value;
    }

    private String getMirrorValue(String name, AnnotationMirror mirror) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : mirror.getElementValues().entrySet()) {
            if (name.equals(e.getKey().getSimpleName().toString())) {
                return e.getValue().toString();
            }
        }

        return null;
    }

    private static List<String> splitArrayMirrorValue(String arrayMirrorValue) {
        if (arrayMirrorValue == null) {
            return Collections.emptyList();
        }

        int start = arrayMirrorValue.indexOf('{') + 1;
        int end = arrayMirrorValue.lastIndexOf('}');

        return Arrays.stream(arrayMirrorValue.substring(start, end).split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private static String extractTypeParameter(String type, String _default) {
        int genericStart = type.indexOf('<') + 1;
        int genericEnd = type.lastIndexOf('>');

        return genericStart <= 0
                ? _default
                : type.substring(genericStart, genericEnd);
    }
}
