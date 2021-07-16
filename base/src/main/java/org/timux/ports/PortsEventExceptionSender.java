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

class PortsEventExceptionSender {

    @Out
    private Event<PortsEventException> portsEventException;

    public void trigger(Throwable throwable) {
        StackTraceElement[] stack = throwable.getStackTrace();

        for (int i = 0; i < stack.length; i++) {
            if (stack[i].getMethodName().equals("onPortsEventException")) {
                Ports.printWarning(String.format("In order to prevent an error loop, the following exception will "
                        + "not be forwarded to the %s handler:", PortsEventException.class.getName()));
                throwable.printStackTrace();
                return;
            }
        }

        if (portsEventException != null && portsEventException.isConnected()) {
            portsEventException.trigger(new PortsEventException(throwable));
        } else {
            /*
             * We can forward this to the protocols. It doesn't matter whether they are active or not
             * since this is a 'with' statement, not a 'when' statement. The global protocol state
             * will not be affected. The protocols will also take care of printing the stacktrace
             * to stderr in case there is no receiver for the event.
             */
            Ports.protocol()
                .with(PortsEventException.class)
                    .trigger(new PortsEventException(throwable));
        }
    }

    public void disconnect() {
        if (portsEventException != null) {
            portsEventException.disconnect();
        }
    }
}
