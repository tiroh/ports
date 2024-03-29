/*
 * Copyright 2022 Tim Rohlfs
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

package org.timux.ports.hilla;

import org.timux.ports.Ports;

public class TestApplication {

  public static void main(String[] args) {
    A a = new A();
    B b = new B();

    Ports.connect(a).and(b);

    a.trigger("test");

    System.out.println(b.data);
  }
}
