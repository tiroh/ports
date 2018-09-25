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

class FieldInfo {

    private String name;

    private String type;

    private String descriptor;

    private String ownerClassName;

    private Class typeClass;

    public FieldInfo(
            String name,
            String type,
            Class typeClass,
            String descriptor,
            String parentClassName)
    {
        this.name = name;
        this.type = type;
        this.typeClass = typeClass;
        this.descriptor = descriptor;
        this.ownerClassName = parentClassName;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public String getOwnerClassName() {
        return ownerClassName;
    }

    public Class getTypeClass() {
        return typeClass;
    }

    @Override
    public String toString() {
        return String.format("(%s,%s,%s,%s,%s)", name, type, typeClass.getName(), descriptor, ownerClassName);
    }
}
