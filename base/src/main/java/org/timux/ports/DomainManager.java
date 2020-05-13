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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class DomainManager {

    static class Key {

        final WeakReference<?> componentRef;
        final int hashCode;

        Key(Object component) {
            componentRef = new WeakReference<>(component);
            hashCode = component.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Key key = (Key) o;

            return hashCode == key.hashCode && componentRef.get() == key.componentRef.get();
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            Object component = componentRef.get();
            return "Key{" + component + "}";
        }
    }

    private static final String DEFAULT_DOMAIN_NAME = "default";
    private static final Domain DEFAULT_DOMAIN = new Domain(DEFAULT_DOMAIN_NAME, SyncPolicy.COMPONENT_SYNC, DispatchPolicy.SAME_THREAD);

    private static Map<Key, Domain> domains = new HashMap<>();

    static synchronized Domain getDomain(Object component) {
        Domain domain = domains.get(new Key(component));
        return domain != null ? domain : DEFAULT_DOMAIN;
    }

    static synchronized void register(Object component, Domain domain) {
        domains.put(new Key(component), domain);
    }

    static synchronized void gc() {
        List<Key> garbageKeys = new ArrayList<>();

        for (Map.Entry<Key, Domain> e : domains.entrySet()) {
            if (e.getKey().componentRef.get() == null) {
                garbageKeys.add(e.getKey());
            }
        }

        garbageKeys.forEach(domains::remove);
    }

    static synchronized void awaitQuiescence() {
        for (Map.Entry<Key, Domain> e : domains.entrySet()) {
            e.getValue().awaitQuiescence();
        }
    }
}
