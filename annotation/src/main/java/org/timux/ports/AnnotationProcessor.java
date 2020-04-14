package org.timux.ports;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class AnnotationProcessor extends AbstractProcessor {

    private class MethodCheckerVisitor implements ElementVisitor<Void, Void> {

        @Override
        public Void visit(Element e, Void aVoid) {
            return null;
        }

        @Override
        public Void visit(Element e) {
            return null;
        }

        @Override
        public Void visitPackage(PackageElement e, Void aVoid) {
            return null;
        }

        @Override
        public Void visitType(TypeElement e, Void aVoid) {
            return null;
        }

        @Override
        public Void visitVariable(VariableElement e, Void aVoid) {
            return null;
        }

        @Override
        public Void visitExecutable(ExecutableElement element, Void nothing) {
            String portName = element.getSimpleName().toString();

            if (element.getParameters().size() != 1) {
                processingEnv.getMessager().printMessage(
                        DIAGNOSTIC_MESSAGE_KIND,
                        String.format("IN port [%s] must have exactly 1 parameter", portName),
                        element);

                if (element.getParameters().isEmpty()) {
                    return null;
                }
            }

            String messageType = element.getParameters().get(0).asType().toString();
            String returnType = element.getReturnType().toString();

            if (!messageType.endsWith("Event") && !messageType.endsWith("Exception") && !messageType.endsWith("Request") && !messageType.endsWith("Command")) {
                processingEnv.getMessager().printMessage(
                        DIAGNOSTIC_MESSAGE_KIND,
                        String.format("type '%s' is not a valid event type", messageType),
                        element);

                return null;
            }

            String correctName = toInPortName(messageType);

            if (!portName.equals(correctName)) {
                processingEnv.getMessager().printMessage(
                        DIAGNOSTIC_MESSAGE_KIND,
                        String.format("'%s' is not a valid IN port name (should be '%s')", portName, correctName),
                        element);
            }

            if (messageType.endsWith("Event") || messageType.endsWith("Exception")) {
                if (!returnType.equals("void")) {
                    processingEnv.getMessager().printMessage(
                            DIAGNOSTIC_MESSAGE_KIND,
                            String.format("IN port [%s] must not return a value", portName),
                            element);
                }
            }

            if (messageType.endsWith("Request") || messageType.endsWith("Command")) {
                String registeredReturnType = portSignatures.get(messageType);

                String registeredReturnTypeInfo = registeredReturnType != null
                        ? " (" + registeredReturnType + ")"
                        : "";

                if (returnType.equals("void")) {
                    processingEnv.getMessager().printMessage(
                            DIAGNOSTIC_MESSAGE_KIND,
                            String.format("IN port [%s] must return a value%s", portName, registeredReturnTypeInfo),
                            element);
                } else if (returnType.equals(Void.class.getName())) {
                    processingEnv.getMessager().printMessage(
                            DIAGNOSTIC_MESSAGE_KIND,
                            String.format("IN port [%s] has inadmissible return type (%s)", portName, returnType),
                            element);
                } else {
                    if (registeredReturnType == null) {
                        portSignatures.put(messageType, returnType);
                    } else {
                        if (!registeredReturnType.equals(returnType)) {
                            processingEnv.getMessager().printMessage(
                                    DIAGNOSTIC_MESSAGE_KIND,
                                    String.format("port signatures do not match for IN port [%s]: message type '%s' cannot be mapped to both '%s' and '%s'",
                                            portName, messageType, registeredReturnType, returnType),
                                    element);
                        }
                    }
                }
            }

            HashSet<String> names = inPortClassNames.get(element.getEnclosingElement());

            if (names == null) {
                names = new HashSet<>();
                inPortClassNames.put(element.getEnclosingElement(), names);
                portsClassNames.add(element.getEnclosingElement().toString());
            }

            if (names.contains(portName)) {
                processingEnv.getMessager().printMessage(
                        DIAGNOSTIC_MESSAGE_KIND,
                        String.format("duplicate IN port [%s]", portName),
                        element);
            }

            names.add(portName);

            return null;
        }

        @Override
        public Void visitTypeParameter(TypeParameterElement e, Void aVoid) {
            return null;
        }

        @Override
        public Void visitUnknown(Element e, Void aVoid) {
            return null;
        }
    }

    private final static Diagnostic.Kind DIAGNOSTIC_MESSAGE_KIND = Diagnostic.Kind.ERROR;

    private final static String EVENT_TYPE = Event.class.getName();
    private final static String REQUEST_TYPE = Request.class.getName();

    private final static String STACK_TYPE = Stack.class.getName();
    private final static String QUEUE_TYPE = Queue.class.getName();

    private final MethodCheckerVisitor methodCheckerVisitor = new MethodCheckerVisitor();

    private final Map<Element, HashSet<String>> inPortClassNames = new HashMap<>();
    private final Set<String> portsClassNames = new HashSet<>();
    private final Map<String, String> portSignatures = new HashMap<>();

    private final Set<String> unmodifiableSupportedAnnotationTypes;

    {
        Set<String> supportedAnnotationTypes = new HashSet<>();

        supportedAnnotationTypes.add(In.class.getName());
        supportedAnnotationTypes.add(Out.class.getName());
        supportedAnnotationTypes.add(Response.class.getName());

        unmodifiableSupportedAnnotationTypes = Collections.unmodifiableSet(supportedAnnotationTypes);
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) {
            return true;
        }

        inPortClassNames.clear();
        portsClassNames.clear();
        portSignatures.clear();

        checkOutPort(roundEnvironment);
        checkInPorts(roundEnvironment);
        checkRequestTypes(roundEnvironment);

//        portsClassNames.forEach(name -> writeClassFile(name));

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
                processingEnv.getMessager().printMessage(
                        DIAGNOSTIC_MESSAGE_KIND,
                        String.format("OUT port [%s] must not be static", portName),
                        element);
            }

            if (!portType.startsWith(EVENT_TYPE + "<")
                    && !portType.equals(EVENT_TYPE)
                    && !portType.startsWith(REQUEST_TYPE + "<")
                    && !portType.equals(REQUEST_TYPE))
            {
                processingEnv.getMessager().printMessage(
                        DIAGNOSTIC_MESSAGE_KIND,
                        String.format("type '%s' is not a valid OUT port type", portType),
                        element);

                return;
            }

            if (returnType.equals(Void.class.getName())) {
                processingEnv.getMessager().printMessage(
                        DIAGNOSTIC_MESSAGE_KIND,
                        String.format("OUT port [%s] has inadmissible return type (%s)", portName, returnType),
                        element);
            } else {
                String registeredReturnType = portSignatures.get(messageType);

                if (registeredReturnType == null) {
                    portSignatures.put(messageType, returnType);
                } else {
                    if (!registeredReturnType.equals(returnType)) {
                        processingEnv.getMessager().printMessage(
                                DIAGNOSTIC_MESSAGE_KIND,
                                String.format("port signatures do not match for OUT port [%s]: message type '%s' cannot be mapped to both '%s' and '%s'",
                                        portName, messageType, registeredReturnType, returnType),
                                element);
                    }
                }
            }

            if (messageType.equals(Object.class.getName())) {
                processingEnv.getMessager().printMessage(
                        DIAGNOSTIC_MESSAGE_KIND,
                        String.format("no message type specified for OUT port [%s]", portName),
                        element);
            }

            if (!messageType.equals(Object.class.getName())
                    && portType.startsWith(EVENT_TYPE)
                    && !messageType.endsWith("Event")
                    && !messageType.endsWith("Exception"))
            {
                String commandNote = messageType.endsWith("Command")
                        ? " (commands should be implemented via request ports)"
                        : "";

                processingEnv.getMessager().printMessage(
                        DIAGNOSTIC_MESSAGE_KIND,
                        String.format("'%s' is not a valid event type%s", messageType, commandNote),
                        element);
            } else {
                if (portType.startsWith(EVENT_TYPE) && (messageType.endsWith("Event") || messageType.endsWith("Exception"))) {
                    String correctName = toOutPortName(messageType);

                    if (!portName.equals(correctName)) {
                        processingEnv.getMessager().printMessage(
                                DIAGNOSTIC_MESSAGE_KIND,
                                String.format("'%s' is not a valid OUT port name (should be '%s')", portName, correctName),
                                element);
                    }
                }
            }

            if (!messageType.equals(Object.class.getName()) && portType.startsWith(REQUEST_TYPE) && !(messageType.endsWith("Request") || messageType.endsWith("Command"))) {
                processingEnv.getMessager().printMessage(
                        DIAGNOSTIC_MESSAGE_KIND,
                        String.format("'%s' is not a valid request type", messageType),
                        element);
            } else {
                if (portType.startsWith(REQUEST_TYPE) && (messageType.endsWith("Request") || messageType.endsWith("Command"))) {
                    String correctName = toOutPortName(messageType);

                    if (!portName.equals(correctName)) {
                        processingEnv.getMessager().printMessage(
                                DIAGNOSTIC_MESSAGE_KIND,
                                String.format("'%s' is not a valid OUT port name (should be '%s')", portName, correctName),
                                element);
                    }
                }
            }

            portsClassNames.add(element.getEnclosingElement().toString());

            try (FileWriter fileWriter = new FileWriter("/home/tim/ports.txt")){
                portSignatures.forEach((k, v) -> {
                    try {
                        fileWriter.write(String.format("%s -> %s\n", k, v));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {

            }
        }
    }

    private void checkInPorts(RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(In.class)) {
            String portType = element.asType().toString();
            String portName = element.getSimpleName().toString();
            String portSignature = extractTypeParameter(portType, "");

            String messageType = portSignature.indexOf(',') >= 0
                    ? portSignature.substring(0, portSignature.indexOf(','))
                    : portSignature;

            if (element.getModifiers().contains(Modifier.STATIC)) {
                processingEnv.getMessager().printMessage(
                        DIAGNOSTIC_MESSAGE_KIND,
                        String.format("IN port [%s] must not be static", element.getSimpleName().toString()),
                        element);
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
                processingEnv.getMessager().printMessage(
                        DIAGNOSTIC_MESSAGE_KIND,
                        String.format("type '%s' is not a valid IN port type", portType),
                        element);
            }
        }
    }

    private void checkRequestTypes(RoundEnvironment roundEnvironment) {
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Response.class)) {
            Response responseAnno = element.getAnnotation(Response.class);

            for (AnnotationMirror mirror : element.getAnnotationMirrors()) {
                if (mirror.getAnnotationType().toString().equals(Response.class.getName())) {
                    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : mirror.getElementValues().entrySet()) {
                        if ("value".equals(e.getKey().getSimpleName().toString())) {
                            String messageType = element.toString();
                            String returnType = e.getValue().toString().substring(0, e.getValue().toString().lastIndexOf('.'));

                            if (returnType.equals(Void.class.getName())) {
                                processingEnv.getMessager().printMessage(
                                        DIAGNOSTIC_MESSAGE_KIND,
                                        String.format("message type '%s' has inadmissible return type (%s)", messageType, returnType),
                                        element);

                                continue;
                            }

                            String registeredReturnType = portSignatures.get(messageType);

                            if (registeredReturnType == null) {
                                portSignatures.put(messageType, returnType);
                            } else {
                                if (!registeredReturnType.equals(returnType)) {
                                    processingEnv.getMessager().printMessage(
                                            DIAGNOSTIC_MESSAGE_KIND,
                                            String.format("message type '%s' cannot be mapped to both '%s' and '%s'",
                                                    messageType, registeredReturnType, returnType),
                                            element);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String toOutPortName(String messageType) {
        String suffix = messageType.substring(messageType.lastIndexOf('.') + 1)
                + (messageType.endsWith("Exception") ? "Event" : "");

        return Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1);
    }

    private String toInPortName(String messageType) {
        String suffix = messageType.substring(messageType.lastIndexOf('.') + 1);

        if (messageType.endsWith("Event")) {
            return "on" + suffix.substring(0, suffix.length() - "Event".length());
        } else {
            return "on" + suffix;
        }
    }

    private void writeClassFile(String className) {
        JavaFileObject jfo = null;
        try {
            String processedName = className.toString().replace('.', '_');
            jfo = processingEnv.getFiler().createSourceFile("org.timux.ports.spec." + processedName);

            Writer writer = jfo.openWriter();
            writer.write("package org.timux.ports.spec;\nimport javax.lang.model.element.*;\nimport java.util.*;\n");
            writer.write("public class " + processedName + " {\npublic final static List<String> elementNames = new ArrayList<>();\n");
            writer.write("static {\n");

            writer.write("elementNames.add(\"" + className + "\");\n");
            writer.write("}\n}\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return unmodifiableSupportedAnnotationTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    private static String extractTypeParameter(String type, String _default) {
        int genericStart = type.indexOf('<');
        int genericEnd = type.lastIndexOf('>');

        return genericStart < 0
                ? _default
                : type.substring(genericStart + 1, genericEnd);
    }
}
