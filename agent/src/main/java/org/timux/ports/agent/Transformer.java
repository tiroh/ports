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

import com.sun.org.apache.xml.internal.utils.ThreadControllerWrapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.FileOutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.*;

class Transformer implements ClassFileTransformer {

    private final List<String> packages = new ArrayList<>();

    public Transformer(String args) {
        if (args != null) {
            Arrays.stream(args.replace('.', '/').split(","))
                    .forEach(e -> packages.add(e + "/"));
        }
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer)
    {
        if (className == null) {
            return null;
        }

        try {
            if (!packages.isEmpty()) {
                boolean isClassIncluded = false;

                for (String pkg : packages) {
                    if (className.startsWith(pkg)) {
                        isClassIncluded = true;
                        break;
                    }
                }

                if (!isClassIncluded) {
                    return null;
                }
            }

            ClassReader cr = new ClassReader(classfileBuffer);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
            ClassVisitor cv = new Visitor(cw);

            try {
                cr.accept(cv, 0);
            } catch (Throwable e) {
                System.err.println(e.getMessage() + ": " + className);
                e.printStackTrace();
                return null;
            }

            final byte[] newClassBytes = cw.toByteArray();

//            if (className.contains("service")) {
//                try (FileOutputStream fos = new FileOutputStream(String.format("%s.class", className.substring(className.lastIndexOf('/') + 1)))) {
//                    fos.write(newClassBytes);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }

            return newClassBytes;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
}
