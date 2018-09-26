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

import org.objectweb.asm.*;
import org.timux.ports.*;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public class Visitor extends ClassVisitor {

    private final static int OPCODE_VERSION = Opcodes.ASM5;

    public final static String HANDLER_PREFIX = "__handler_";

    private final static String EVENT_TYPE = getDescriptor(Event.class);
    private final static String REQUEST_TYPE = getDescriptor(Request.class);

    private final static String QUEUE_TYPE = getDescriptor(Queue.class);
    private final static String STACK_TYPE = getDescriptor(Stack.class);

    private final static String OUT_DESCRIPTOR = getDescriptor(Out.class);
    private final static String IN_DESCRIPTOR = getDescriptor(In.class);

    private final String INIT_METHOD_NAME;
    private final String INITIALIZED_FLAG_NAME;

    private final String FLIMFLAM;

    private String currentClassName;

    private String lastFieldName;
    private String lastFieldDescriptor;

    private String lastMethodName;
    private String lastMethodDescriptor;

    private Set<FieldInfo> outFieldsToInitialize = new LinkedHashSet<>();
    private Set<FieldInfo> inFieldsToInitialize = new LinkedHashSet<>();

    private Set<HandlerInfo> handlers = new HashSet<>();

    private class FVisitor extends FieldVisitor {

        public FVisitor(FieldVisitor delegate) {
            super(OPCODE_VERSION, delegate);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            boolean isOutAnnotation = descriptor.startsWith(OUT_DESCRIPTOR);
            boolean isInAnnotation = descriptor.startsWith(IN_DESCRIPTOR);

            if (isOutAnnotation || isInAnnotation) {
                if (isOutAnnotation) {
                    FieldInfo fieldInfo = new FieldInfo(
                            lastFieldName,
                            lastFieldDescriptor.substring(1, lastFieldDescriptor.length() - 1),
                            lastFieldDescriptor.equals(EVENT_TYPE) ? Event.class : Request.class,
                            lastFieldDescriptor,
                            currentClassName);

                    outFieldsToInitialize.add(fieldInfo);
                }

                if (isInAnnotation) {
                    FieldInfo fieldInfo = new FieldInfo(
                            lastFieldName,
                            lastFieldDescriptor.substring(1, lastFieldDescriptor.length() - 1),
                            lastFieldDescriptor.equals(QUEUE_TYPE) ? Queue.class : Stack.class,
                            lastFieldDescriptor,
                            currentClassName);

                    inFieldsToInitialize.add(fieldInfo);
                }
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
            boolean isInAnnotation = descriptor.startsWith(IN_DESCRIPTOR);

            if (isInAnnotation) {
                handlers.add(new HandlerInfo(lastMethodName, lastMethodDescriptor, currentClassName));
            }

            return super.visitAnnotation(descriptor, visible);
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
        }

        @Override
        public void visitCode() {
            super.visitCode();
        }
    }

    private class CreateInitializerCallVisitor extends MethodVisitor {

        private boolean isFirstInvokespecial = true;

        public CreateInitializerCallVisitor(MethodVisitor delegate) {
            super(OPCODE_VERSION, delegate);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

            if (opcode == Opcodes.INVOKESPECIAL) {
                if (isFirstInvokespecial) {
                    isFirstInvokespecial = false;

                    visitVarInsn(Opcodes.ALOAD, 0);
                    visitMethodInsn(Opcodes.INVOKESPECIAL, currentClassName, INIT_METHOD_NAME, "()V", false);
                }
            }
        }
    }

    protected Visitor(ClassVisitor delegate) {
        super(OPCODE_VERSION, delegate);

        Random random = new Random();
        FLIMFLAM = String.format("%016x", random.nextLong());

        INIT_METHOD_NAME = "__init_" + FLIMFLAM;
        INITIALIZED_FLAG_NAME = "__initialized_" + FLIMFLAM;
    }

    private static String getDescriptor(Class clazz) {
        if (clazz.isArray()) {
            throw new IllegalArgumentException("Ports types must not be arrays");
        }

        return "L" + clazz.getName().replace('.', '/') + ";";
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        outFieldsToInitialize.clear();

        currentClassName = name;

        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        lastFieldName = name;
        lastFieldDescriptor = descriptor;

        return new FVisitor(super.visitField(access, name, descriptor, signature, value));
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        lastMethodName = name;
        lastMethodDescriptor = descriptor;

        MethodVisitor delegate;

        if (name.equals("<init>")) {
            delegate = new CreateInitializerCallVisitor(super.visitMethod(access, name, descriptor, signature, exceptions));
        } else {
            delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
        }

        return new MVisitor(delegate);
    }

    @Override
    public void visitEnd() {
//        if (!outFieldsToInitialize.isEmpty() || !handlers.isEmpty()) {
            insertHandlerFields();
            insertInitializeMethod(INIT_METHOD_NAME);
//        }

        super.visitEnd();
    }

    private void insertHandlerFields() {
        super.visitField(Opcodes.ACC_PRIVATE, INITIALIZED_FLAG_NAME, "Z", null, null).visitEnd();

        for (HandlerInfo handlerInfo : handlers) {
            super.visitField(
                    Opcodes.ACC_PRIVATE,
                    getHandlerField(handlerInfo.getName()),
                    handlerInfo.hasNonVoidReturnType() ? "Ljava/util/function/Function;" : "Ljava/util/function/Consumer;",
                    null,
                    null)
                    .visitEnd();
        }
    }

    private void insertInitializeMethod(String methodName) {
        MethodVisitor v = super.visitMethod(Opcodes.ACC_PRIVATE, methodName, "()V", null, null);

        Label endLabel = new Label();

        insertInitializeCheck(v, endLabel);
        insertInFieldInitialization(v);
        insertOutFieldInitialization(v);
        insertHandlerFieldInitialization(v);

        v.visitLabel(endLabel);

        v.visitInsn(Opcodes.RETURN);

        v.visitMaxs(8, 8);
        v.visitEnd();
    }

    private void insertInitializeCheck(MethodVisitor v, Label label) {
        v.visitVarInsn(Opcodes.ALOAD, 0);
        v.visitFieldInsn(Opcodes.GETFIELD, currentClassName, INITIALIZED_FLAG_NAME, "Z");
        v.visitInsn(Opcodes.ICONST_0);
        v.visitJumpInsn(Opcodes.IF_ICMPNE, label);

        v.visitVarInsn(Opcodes.ALOAD, 0);
        v.visitInsn(Opcodes.ICONST_1);
        v.visitFieldInsn(Opcodes.PUTFIELD, currentClassName, INITIALIZED_FLAG_NAME, "Z");
    }

    private void insertInFieldInitialization(MethodVisitor v) {
        for (FieldInfo fieldInfo : inFieldsToInitialize) {
            Label afterIfLabel = new Label();

            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitFieldInsn(Opcodes.GETFIELD, fieldInfo.getOwnerClassName(), fieldInfo.getName(), fieldInfo.getDescriptor());
            v.visitJumpInsn(Opcodes.IFNONNULL, afterIfLabel);

            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitTypeInsn(Opcodes.NEW, fieldInfo.getType());
            v.visitInsn(Opcodes.DUP);
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, fieldInfo.getType(), "<init>", "()V", false);
            v.visitFieldInsn(Opcodes.PUTFIELD, fieldInfo.getOwnerClassName(), fieldInfo.getName(), fieldInfo.getDescriptor());

            v.visitLabel(afterIfLabel);
        }
    }

    private void insertOutFieldInitialization(MethodVisitor v) {
        Label startTryLabel = new Label();
        Label endTryLabel = new Label();
        Label catchLabel = new Label();

        v.visitTryCatchBlock(startTryLabel, catchLabel, catchLabel, "java/lang/Exception");

        v.visitLabel(startTryLabel);

        String fieldName = "__field_" + FLIMFLAM;

        for (FieldInfo fieldInfo : outFieldsToInitialize) {
            Label afterIfLabel = new Label();

            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitFieldInsn(Opcodes.GETFIELD, fieldInfo.getOwnerClassName(), fieldInfo.getName(), fieldInfo.getDescriptor());
            v.visitJumpInsn(Opcodes.IFNONNULL, afterIfLabel);

            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitTypeInsn(Opcodes.NEW, fieldInfo.getType());
            v.visitInsn(Opcodes.DUP);
            v.visitMethodInsn(Opcodes.INVOKESPECIAL, fieldInfo.getType(), "<init>", "()V", false);
            v.visitFieldInsn(Opcodes.PUTFIELD, fieldInfo.getOwnerClassName(), fieldInfo.getName(), fieldInfo.getDescriptor());

            v.visitLabel(afterIfLabel);

            // Field f = field.getClass().getDeclaredField("owner");
            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitFieldInsn(Opcodes.GETFIELD, fieldInfo.getOwnerClassName(), fieldInfo.getName(), fieldInfo.getDescriptor());
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);

            v.visitLdcInsn("owner");
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
            v.visitVarInsn(Opcodes.ASTORE, 1);

            // f.setAccessible(true);
            v.visitVarInsn(Opcodes.ALOAD, 1);
            v.visitInsn(Opcodes.ICONST_1);
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);

            // f.set(field, this);
            v.visitVarInsn(Opcodes.ALOAD, 1);
            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitFieldInsn(Opcodes.GETFIELD, fieldInfo.getOwnerClassName(), fieldInfo.getName(), fieldInfo.getDescriptor());
            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);

            // f = field.getClass().getDeclaredField("portName");
            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitFieldInsn(Opcodes.GETFIELD, fieldInfo.getOwnerClassName(), fieldInfo.getName(), fieldInfo.getDescriptor());
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);

            v.visitLdcInsn("name");
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
            v.visitVarInsn(Opcodes.ASTORE, 1);

            // f.setAccessible(true);
            v.visitVarInsn(Opcodes.ALOAD, 1);
            v.visitInsn(Opcodes.ICONST_1);
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);

            // f.set(field, fieldInfo.getName());
            v.visitVarInsn(Opcodes.ALOAD, 1);
            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitFieldInsn(Opcodes.GETFIELD, fieldInfo.getOwnerClassName(), fieldInfo.getName(), fieldInfo.getDescriptor());
            v.visitLdcInsn(fieldInfo.getName());
            v.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Field", "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", false);
        }

        v.visitJumpInsn(Opcodes.GOTO, endTryLabel);

        v.visitLabel(catchLabel);
        v.visitVarInsn(Opcodes.ASTORE, 1);
        v.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
        v.visitInsn(Opcodes.DUP);
        v.visitVarInsn(Opcodes.ALOAD, 1);
        v.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
        v.visitInsn(Opcodes.ATHROW);

        v.visitLabel(endTryLabel);

        v.visitLocalVariable(fieldName, "Ljava/lang/reflect/Field;", null, startTryLabel, catchLabel, 1);
    }

    private void insertHandlerFieldInitialization(MethodVisitor v) {
        for (HandlerInfo handlerInfo : handlers) {
            v.visitVarInsn(Opcodes.ALOAD, 0);
            v.visitVarInsn(Opcodes.ALOAD, 0);

            Handle bootstrapMethod = new Handle(
                    Opcodes.H_INVOKESTATIC,
                    "java/lang/invoke/LambdaMetafactory",
                    "metafactory",
                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                            + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                            + "Ljava/lang/invoke/CallSite;");

            Handle handlerMethod = new Handle(
                    Opcodes.H_INVOKEVIRTUAL,
                    currentClassName,
                    handlerInfo.getName(),
                    handlerInfo.getHandlerDescriptor());

            v.visitInvokeDynamicInsn(
                    handlerInfo.hasNonVoidReturnType() ? "apply" : "accept",
                    String.format("(L%s;)L%s;", currentClassName, handlerInfo.hasNonVoidReturnType() ? "java/util/function/Function" : "java/util/function/Consumer"),
                    bootstrapMethod,
                    Type.getType("(Ljava/lang/Object;)" + (handlerInfo.hasNonVoidReturnType() ? "Ljava/lang/Object;" : "V")),
                    handlerMethod,
                    Type.getType(handlerInfo.getHandlerDescriptor()));

            v.visitFieldInsn(
                    Opcodes.PUTFIELD,
                    currentClassName,
                    getHandlerField(handlerInfo.getName()),
                    handlerInfo.hasNonVoidReturnType() ? "Ljava/util/function/Function;" : "Ljava/util/function/Consumer;");
        }
    }

    private String getHandlerField(String handlerName) {
        return String.format("%s%s_%s", HANDLER_PREFIX, handlerName, FLIMFLAM);
    }
}
