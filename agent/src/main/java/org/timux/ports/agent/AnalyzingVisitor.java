/*
 * Copyright 2018 Tim Rohlfs
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
