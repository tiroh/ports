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

import org.timux.ports.types.Either;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

class ComponentInfo {

    static class Dummy {}

    private final Either<Object, Map<Object, Dummy>> component;

    private ComponentInfo(Object staticComponent, Map<Object, Dummy> dynamicComponentRefs) {
        component = Either.of(staticComponent, dynamicComponentRefs);
    }

    static ComponentInfo ofStatic(Object staticComponent) {
        return new ComponentInfo(staticComponent, null);
    }

    static ComponentInfo ofDynamic(Object dynamicComponent) {
        Map<Object, Dummy> refs = new WeakHashMap<>();
        refs.put(dynamicComponent, null);
        return new ComponentInfo(null, refs);
    }

    public Either<Object, Map<Object, Dummy>> getComponents() {
        return component;
    }

    @Override
    public String toString() {
        return "ComponentInfo{" +
                "component=" + component +
                '}';
    }
}
