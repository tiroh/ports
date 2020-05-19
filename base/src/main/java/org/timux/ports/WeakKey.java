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

class WeakKey {

    final WeakReference<?> componentRef;
    final int hashCode;

    WeakKey(Object component) {
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

        WeakKey key = (WeakKey) o;

        return hashCode == key.hashCode && componentRef.get() == key.componentRef.get();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        Object component = componentRef.get();
        return "WeakKey{" + component + "}";
    }
}
