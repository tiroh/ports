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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class CacheManager {

    private static final Map<Class<?>, ArrayList<WeakReference<Request<?, ?>>>> cachesToBeCleared = new HashMap<>();

    synchronized static void registerRequestPort(Request<?, ?> requestPort, Pure pureAnno) {
        Class<?>[] clearCacheOn = pureAnno.clearCacheOn();

        for (Class<?> eventType : clearCacheOn) {
            ArrayList<WeakReference<Request<?, ?>>> requestPorts = cachesToBeCleared.get(eventType);

            if (requestPorts == null) {
                requestPorts = new ArrayList<>();
                cachesToBeCleared.put(eventType, requestPorts);
            }

            for (int j = 0; j < requestPorts.size(); ) {
                if (requestPorts.get(j).get() == null) {
                    requestPorts.remove(j);
                } else {
                    j++;
                }
            }

            requestPorts.add(new WeakReference<>(requestPort));
        }
    }

    synchronized static void trigger(Class<?> eventType) {
        ArrayList<WeakReference<Request<?, ?>>> requestPorts = cachesToBeCleared.get(eventType);

        if (requestPorts == null) {
            return;
        }

        for (WeakReference<Request<?, ?>> ref : requestPorts) {
            Request<?, ?> requestPort = ref.get();

            if (requestPort == null) {
                continue;
            }

            requestPort.clearCache();
        }
    }

    synchronized static void clear() {
        cachesToBeCleared.forEach((eventType, requestPorts) -> {
            requestPorts.forEach(ref -> {
                Request<?, ?> requestPort = ref.get();

                if (requestPort != null) {
                    requestPort.clearCache();
                }
            });
        });
    }

    synchronized static void reset() {
        clear();
        cachesToBeCleared.clear();
    }
}
