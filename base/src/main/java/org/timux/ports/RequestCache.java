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

import org.timux.ports.types.Pair;
import org.timux.ports.types.Tuple;

import java.lang.ref.SoftReference;

class RequestCache<I, O> {

    private final Pair<SoftReference<I>, SoftReference<O>>[] data;

    private int startIdx = 0;
    private int size = 0;

    private final boolean isStateless;

    /**
     * @param capacity Must be a power of two.
     */
    public RequestCache(int capacity, Class<?> requestType) {
        this.data = new Pair[capacity];
        this.isStateless = requestType.getDeclaredFields().length == 0;
    }

    public synchronized O get(I payload) {
        for (int k = 0; k < size; k++) {
            int i = (startIdx + k) & (data.length - 1);

            Pair<SoftReference<I>, SoftReference<O>> p = data[i];

            if (p == null) {
                continue;
            }

            I input = p.getA().get();

            if (input == null) {
                data[i] = null;
                return null;
            }

            O output = p.getB().get();

            if (output == null) {
                data[i] = null;
                return null;
            }

            if (isStateless || input.equals(payload)) {
                int newIndex = (startIdx + (k >> 1)) & (data.length - 1);

                if (newIndex != i) {
                    int oneBelow = (newIndex + 1) & (data.length - 1);

                    Pair<SoftReference<I>, SoftReference<O>> newIndexObj = data[newIndex];
                    Pair<SoftReference<I>, SoftReference<O>> oneBelowObj = data[oneBelow];

                    data[i] = oneBelowObj;
                    data[oneBelow] = newIndexObj;
                    data[newIndex] = p;
                }

                return output;
            }
        }

        return null;
    }

    public synchronized void put(I input, O output) {
        if (size < data.length) {
            size++;
        }

        startIdx = (startIdx + data.length - 1) & (data.length - 1);
        data[startIdx] = Tuple.of(new SoftReference<>(input), new SoftReference<>(output));
    }

    public synchronized void clear() {
        for (int i = 0; i < data.length; i++) {
            data[i] = null;
        }

        startIdx = 0;
        size = 0;
    }
}
