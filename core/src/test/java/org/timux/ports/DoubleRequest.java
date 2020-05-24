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

@Response(Double.class)
public class DoubleRequest {

    private final Double data;
    private final Object sender;

    public DoubleRequest(Double data) {
        this.data = data;
        this.sender = null;
    }

    public DoubleRequest(Double data, Object sender) {
        this.data = data;
        this.sender = sender;
    }

    public DoubleRequest(int data) {
        this.data = Double.valueOf(data);
        this.sender = null;
    }

    public DoubleRequest(int data, Object sender) {
        this.data = Double.valueOf(data);
        this.sender = sender;
    }

    public Double getData() {
        return data;
    }

    public Object getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return Double.toString(data);
    }
}
