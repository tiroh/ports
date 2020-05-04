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

package org.timux.ports;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * <p>A class that represents an IN port with stack semantics. That is, incoming data does not trigger an event, but
 * is stored and can be retrieved in LIFO order.</p>
 *
 * <p>Any field of this type that is supposed to serve as an IN port must be annotated with the {@link In}
 * annotation.</p>
 *
 * <p>(If you want incoming data to trigger an event, create a handler method and annotate it with the
 * {@link In} annotation).)</p>
 *
 * @param <T> The type of data items stored in this stack.
 *
 * @see Queue
 *
 * @author Tim Rohlfs
 * @since 0.1
 */
public class Stack<T> {

    private Deque<T> deque = new ArrayDeque<>();

    public synchronized T peek() {
        return deque.peekLast();
    }

    public synchronized T pop() {
        return deque.removeLast();
    }

    public synchronized boolean isEmpty() {
        return deque.isEmpty();
    }

    public synchronized int size() {
        return deque.size();
    }

    synchronized void push(T item) {
        deque.addLast(item);
    }
}
