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

import org.timux.ports.types.NoSuchConstituentException;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;

@SuppressWarnings("unchecked")
class ConcurrentWeakHashMap<K, V> implements Iterable<K> {

    private static final int NUMBER_OF_PARTITIONS =
            (int) Math.pow(2.0, Math.ceil(Math.log(4.0 * Runtime.getRuntime().availableProcessors()) / Math.log(2.0)));

    private static final int HASH_MASK = NUMBER_OF_PARTITIONS - 1;

    private final Map<K, V>[] maps = new WeakHashMap[NUMBER_OF_PARTITIONS];
    private final Lock[] locks = new ReentrantLock[NUMBER_OF_PARTITIONS];

    public ConcurrentWeakHashMap() {
        for (int i = 0; i < maps.length; i++) {
            maps[i] = new WeakHashMap<>();
            locks[i] = new ReentrantLock(false);
        }
    }

    public void put(K key, V value) {
        int p = key.hashCode() & HASH_MASK;
        Map<K, V> map = maps[p];
        Lock lock = locks[p];

        lock.lock();

        try {
            map.put(key, value);
        } finally {
            lock.unlock();
        }
    }

    public V get(K key) {
        int p = key.hashCode() & HASH_MASK;
        Map<K, V> map = maps[p];
        Lock lock = locks[p];

        lock.lock();

        try {
            return map.get(key);
        } finally {
            lock.unlock();
        }
    }

    public V remove(K key) {
        int p = key.hashCode() & HASH_MASK;
        Map<K, V> map = maps[p];
        Lock lock = locks[p];

        lock.lock();

        try {
            return map.remove(key);
        } finally {
            lock.unlock();
        }
    }

    private class It implements Iterator<K> {

        private int currentIndex = 0;
        private Iterator<K> currentIt;

        @Override
        public boolean hasNext() {
            if (currentIndex >= maps.length) {
                return false;
            }

            if (currentIt == null) {
                for (; currentIndex < maps.length; currentIndex++) {
                    if (!maps[currentIndex].isEmpty()) {
                        currentIt = maps[currentIndex].keySet().iterator();
                        break;
                    }
                }

                if (currentIt == null) {
                    return false;
                }
            }

            return currentIt.hasNext() || (currentIndex + 1 < maps.length && !maps[currentIndex + 1].isEmpty());
        }

        @Override
        public K next() {
            K key = currentIt.next();

            if (!currentIt.hasNext()) {
                currentIt = null;
                currentIndex++;
            }

            return key;
        }
    }

    @Override
    public Iterator<K> iterator() {
        return new It();
    }

    public V compute(K key, BiFunction<K, V, V> mapper) {
        int p = key.hashCode() & HASH_MASK;
        Map<K, V> map = maps[p];
        Lock lock = locks[p];

        lock.lock();

        try {
            return map.compute(key, mapper);
        } finally {
            lock.unlock();
        }
    }

    public V computeIfAbsent(K key, Function<K, V> mapper) {
        int p = key.hashCode() & HASH_MASK;
        Map<K, V> map = maps[p];
        Lock lock = locks[p];

        lock.lock();

        try {
            return map.computeIfAbsent(key, mapper);
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        for (int i = 0; i < maps.length; i++) {
            Lock lock = locks[i];
            lock.lock();

            try {
                maps[i].clear();
            } finally {
                lock.unlock();
            }
        }
    }
}
