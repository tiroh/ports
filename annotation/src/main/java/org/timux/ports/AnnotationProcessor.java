package org.timux.ports;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
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
            String elementName = element.getSimpleName().toString();

            if (element.getParameters().size() != 1) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("port '%s' must have exactly 1 parameter", elementName),
                        element);
            }

            HashSet<String> names = inPortClassNames.get(element.getEnclosingElement());

            if (names == null) {
                names = new HashSet<>();
                inPortClassNames.put(element.getEnclosingElement(), names);
                portsClassNames.add(element.getEnclosingElement().toString());
            }

            if (names.contains(elementName)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("duplicate IN port '%s'", elementName),
                        element);
            }

            names.add(elementName);

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

    private final static String EVENT_TYPE = Event.class.getName();
    private final static String REQUEST_TYPE = Request.class.getName();

    private final static String STACK_TYPE = Stack.class.getName();
    private final static String QUEUE_TYPE = Queue.class.getName();

    private final MethodCheckerVisitor methodCheckerVisitor = new MethodCheckerVisitor();

    private final Map<Element, HashSet<String>> inPortClassNames = new HashMap<>();

    private final Set<String> portsClassNames = new HashSet<>();

    private final Set<String> unmodifiableSupportedAnnotationTypes;

    {
        Set<String> supportedAnnotationTypes = new HashSet<>();

        supportedAnnotationTypes.add(In.class.getName());
        supportedAnnotationTypes.add(Out.class.getName());

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

        for (Element element : roundEnvironment.getElementsAnnotatedWith(Out.class)) {
            if (element.getModifiers().contains(Modifier.STATIC)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("OUT port '%s' must not be static", element.getSimpleName().toString()),
                        element);
            }

            String elementType = element.asType().toString();

            if (!elementType.startsWith(EVENT_TYPE + "<")
                    && !elementType.equals(EVENT_TYPE)
                    && !elementType.startsWith(REQUEST_TYPE + "<")
                    && !elementType.equals(REQUEST_TYPE))
            {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("type '%s' is not a valid OUT port type", elementType),
                        element);
            }

            portsClassNames.add(element.getEnclosingElement().toString());
        }

        for (Element element : roundEnvironment.getElementsAnnotatedWith(In.class)) {
            if (element.getModifiers().contains(Modifier.STATIC)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("IN port '%s' must not be static", element.getSimpleName().toString()),
                        element);
            }

            if (!element.getKind().isField()) {
                element.accept(methodCheckerVisitor, null);
                continue;
            }

            String elementType = element.asType().toString();

            if (!elementType.startsWith(STACK_TYPE + "<")
                    && !elementType.equals(STACK_TYPE)
                    && !elementType.startsWith(QUEUE_TYPE + "<")
                    && !elementType.equals(QUEUE_TYPE))
            {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        String.format("type '%s' is not a valid IN port type", elementType),
                        element);
            }
        }

//        portsClassNames.forEach(name -> writeClassFile(name));

        return true;
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
}
