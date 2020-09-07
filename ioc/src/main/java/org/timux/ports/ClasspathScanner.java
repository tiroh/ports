/*
 * Copyright 2018-2020 Tim Rohlfs
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

package org.timux.ports;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ClasspathScanner {

    public void scan(List<String> componentScanPackages) {
        String[] packages = componentScanPackages.toArray(new String[componentScanPackages.size()]);

        ClassGraph classGraph = new ClassGraph()
                .acceptPackages(packages)
                .ignoreFieldVisibility()
                .ignoreMethodVisibility()
                .enableMethodInfo()
                .enableFieldInfo()
                .enableAnnotationInfo();

//        Arrays.stream(classGraph.getClasspath().split(":")).forEach(System.out::println);
//        System.out.println();

        try (ScanResult scanResult = classGraph.scan()) {
//            scanResult.getAllClasses().forEach(System.out::println);

            Set<ClassInfo> classInfos = new HashSet<>(scanResult.getClassesWithMethodAnnotation(In.class.getName()));
            classInfos.addAll(scanResult.getClassesWithFieldAnnotation(In.class.getName()));
            classInfos.addAll(scanResult.getClassesWithFieldAnnotation(Out.class.getName()));

            classInfos.forEach(
                    classInfo -> {
                        try {
                            Object instance = classInfo.loadClass().getConstructor().newInstance();
                            PortsIoc.register(instance, Scope.SINGLETON);
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        }
    }
}
