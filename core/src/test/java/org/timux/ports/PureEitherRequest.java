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

import org.timux.ports.types.Failure;

import java.util.Objects;

@Response(Integer.class)
@Response(Failure.class)
@Pure(clearCacheOn = ClearEvent.class)
public class PureEitherRequest {

    private final int arg;

    public PureEitherRequest(int arg) {
        this.arg = arg;
    }

    public int getArg() {
        return arg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PureEitherRequest that = (PureEitherRequest) o;
        return arg == that.arg;
    }

    @Override
    public int hashCode() {
        return Objects.hash(arg);
    }
}
