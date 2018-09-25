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

package org.timux.ports.agent;

class HandlerInfo {

    private String name;

    private String ownerClassName;

    private String descriptor;

    public HandlerInfo(
            String name,
            String descriptor,
            String ownerClassName)
    {
        this.name = name;
        this.descriptor = descriptor;
        this.ownerClassName = ownerClassName;
    }

    public String getName() {
        return name;
    }

    public String getHandlerDescriptor() {
        return descriptor;
    }

    public String getFieldDescriptor() {
        return descriptor.substring(1, descriptor.lastIndexOf(')'));
    }

    public String getOwnerClassName() {
        return ownerClassName;
    }

    public boolean hasNonVoidReturnType() {
        return !descriptor.endsWith("V");
    }

    @Override
    public String toString() {
        return String.format("(%s,%s,%s,%s)", name, descriptor, getFieldDescriptor(), ownerClassName);
    }
}
