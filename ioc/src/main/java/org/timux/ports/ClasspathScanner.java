/*
 * Copyright 2018-2021 Tim Rohlfs
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
import io.github.classgraph.ScanResult;

import java.util.HashMap;
import java.util.List;

class ClasspathScanner {

    private static final HashMap<String, Class<?>> statics = new HashMap<>();

    synchronized static void scan(List<String> componentScanPackages) {
        String[] packages = componentScanPackages.toArray(new String[componentScanPackages.size()]);

        ClassGraph classGraph = new ClassGraph()
                .acceptPackages(packages)
                .ignoreFieldVisibility()
                .ignoreMethodVisibility()
                .enableMethodInfo()
                .enableFieldInfo()
                .enableAnnotationInfo();

        try (ScanResult scanResult = classGraph.scan()) {
            scanResult.getClassesWithAnnotation(Static.class.getName()).forEach(
                    ci -> statics.put(ci.getName(), ci.loadClass())
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized static HashMap<String, Class<?>> getStatics() {
        return statics;
    }

    synchronized static void clear() {
        statics.clear();
    }
}
