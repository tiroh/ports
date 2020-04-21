package org.timux.ports.verification;

import javax.lang.model.element.*;

class MethodCheckerVisitor implements ElementVisitor<Void, Void> {

    private final Reporter reporter;
    private final VerificationModel verificationModel;

    MethodCheckerVisitor(Reporter reporter, VerificationModel verificationModel) {
        this.reporter = reporter;
        this.verificationModel = verificationModel;
    }

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
            reporter.reportIssue(element, "IN port [%s] must have exactly 1 parameter", portName);

            if (element.getParameters().isEmpty()) {
                return null;
            }
        }

        String parameterName = element.getParameters().get(0).getSimpleName().toString();
        String messageType = element.getParameters().get(0).asType().toString();
        String responseType = element.getReturnType().toString();

        if (!messageType.endsWith("Event") && !messageType.endsWith("Exception") && !messageType.endsWith("Request") && !messageType.endsWith("Command")) {
            reporter.reportIssue(element, "type '%s' is not a valid event type", messageType);
            return null;
        }

        String correctName = PortNamer.toInPortName(messageType);

        if (!portName.equals(correctName)) {
            reporter.reportIssue(element, "'%s' is not a valid IN port name (should be '%s')", portName, correctName);
        }

        if (messageType.endsWith("Event") && !parameterName.equals("event")) {
            reporter.reportIssue(element, "'%s' is not a valid parameter name for IN port [%s] (should be 'event')", parameterName, portName);
        }

        if (messageType.endsWith("Exception") && !parameterName.equals("exception")) {
            reporter.reportIssue(element, "'%s' is not a valid parameter name for IN port [%s] (should be 'exception')", parameterName, portName);
        }

        if (messageType.endsWith("Request") && !parameterName.equals("request")) {
            reporter.reportIssue(element, "'%s' is not a valid parameter name for IN port [%s] (should be 'request')", parameterName, portName);
        }

        if (messageType.endsWith("Command") && !parameterName.equals("command")) {
            reporter.reportIssue(element, "'%s' is not a valid parameter name for IN port [%s] (should be 'command')", parameterName, portName);
        }

        if (messageType.endsWith("Event") || messageType.endsWith("Exception")) {
            if (!responseType.equals("void")) {
                reporter.reportIssue(element, "IN port [%s] must not return a value", portName);
            }
        }

        if (messageType.endsWith("Request") || messageType.endsWith("Command")) {
            verificationModel.verifyAndRegisterInPortResponseType(messageType, responseType, portName, element);
        }

        verificationModel.verifyAndRegisterInPortName(portName, element);

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