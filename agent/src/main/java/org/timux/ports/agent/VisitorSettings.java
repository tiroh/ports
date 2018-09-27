package org.timux.ports.agent;

import org.objectweb.asm.Opcodes;
import org.timux.ports.In;
import org.timux.ports.Out;

class VisitorSettings {

    public final static int OPCODE_VERSION = Opcodes.ASM5;

    public final static String OUT_DESCRIPTOR = getDescriptor(Out.class);
    public final static String IN_DESCRIPTOR = getDescriptor(In.class);

    public static String getDescriptor(Class clazz) {
        if (clazz.isArray()) {
            throw new IllegalArgumentException("Ports types must not be arrays");
        }

        return "L" + clazz.getName().replace('.', '/') + ";";
    }
}
