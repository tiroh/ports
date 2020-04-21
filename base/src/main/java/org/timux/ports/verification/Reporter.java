package org.timux.ports.verification;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

class Reporter {

    private final static Diagnostic.Kind DIAGNOSTIC_MESSAGE_KIND = Diagnostic.Kind.ERROR;

    private ProcessingEnvironment processingEnv;

    Reporter(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    void reportIssue(Element element, String message, Object... args) {
        processingEnv.getMessager().printMessage(
                DIAGNOSTIC_MESSAGE_KIND,
                String.format(message, args),
                element);
    }

    void reportIssue(Element element, AnnotationMirror mirror, String message, Object... args) {
        processingEnv.getMessager().printMessage(
                DIAGNOSTIC_MESSAGE_KIND,
                String.format(message, args),
                element,
                mirror);
    }
}
