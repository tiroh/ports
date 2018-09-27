package org.timux.ports.agent;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

import static org.timux.ports.agent.VisitorSettings.*;

class AnalyzingVisitor extends ClassVisitor {

    private boolean isPortsComponentDetected = false;

    class FVisitor extends FieldVisitor {

        public FVisitor(FieldVisitor delegate) {
            super(OPCODE_VERSION);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.equals(IN_DESCRIPTOR) || descriptor.equals(OUT_DESCRIPTOR)) {
                isPortsComponentDetected = true;
            }

            return super.visitAnnotation(descriptor, visible);
        }
    }

    private class MVisitor extends MethodVisitor {

        public MVisitor(MethodVisitor delegate) {
            super(OPCODE_VERSION, delegate);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.equals(IN_DESCRIPTOR) || descriptor.equals(OUT_DESCRIPTOR)) {
                isPortsComponentDetected = true;
            }

            return super.visitAnnotation(descriptor, visible);
        }
    }

    public AnalyzingVisitor(ClassVisitor delegate) {
        super(OPCODE_VERSION, delegate);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return new FVisitor(super.visitField(access, name, descriptor, signature, value));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        return new MVisitor(super.visitMethod(access, name, descriptor, signature, exceptions));
    }

    public void reset() {
        isPortsComponentDetected = false;
    }

    public boolean isPortsComponentDetected() {
        return isPortsComponentDetected;
    }
}
