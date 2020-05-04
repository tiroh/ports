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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

class Task implements Runnable {

    private Consumer eventPort;
    private Function requestPort;
    private Object payload;
    private Object response;
    private boolean hasReturned = false;

    public Task(Consumer eventPort, Object payload) {
        this.eventPort = eventPort;
        this.payload = payload;
    }

    public Task(Function requestPort, Object payload) {
        this.requestPort = requestPort;
        this.payload = payload;
    }

    @Override
    public void run() {
        if (eventPort != null) {
            eventPort.accept(payload);
        } else {
            response = requestPort.apply(payload);
        }

        hasReturned = true;

        synchronized (this) {
            notify();
        }
    }

    public boolean hasReturned() {
        return hasReturned;
    }

    public synchronized Object waitForResponse() {
        while (!hasReturned) {
            try {
                wait();
            } catch (InterruptedException e) {
                //
            }
        }

        return response;
    }

    public synchronized Object waitForResponse(long timeout, TimeUnit unit) throws TimeoutException {
        long waitMillis = unit.toMillis(timeout);

        while (!hasReturned) {
            long startMillis = System.currentTimeMillis();

            try {
                wait(waitMillis);
            } catch (InterruptedException e) {
                //
            }

            if (!hasReturned) {
                long passedMillis = System.currentTimeMillis() - startMillis;

                if (passedMillis >= timeout - 1) {
                    throw new TimeoutException();
                }

                waitMillis -= passedMillis;
            }
        }

        return response;
    }
}