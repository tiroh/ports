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
 
package org.timux.ports.vaadinspring;

import com.vaadin.flow.component.UI;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

class Scope {

    private final String name;
    private final Map<Object, Scope> childScopes = new HashMap<>();
    private Scope parentScope = null;
    private final Map<Object, String> beans = new HashMap<>();
    private UI ui = null;

    public Scope(String name) {
        this(name, null);
    }

    public Scope(String name, Scope parentScope) {
        this.name = name;
        this.parentScope = parentScope;
    }

    public String getName() {
        return name;
    }

    public Scope findChildScope(Object key) {
        synchronized (childScopes) {
            return childScopes.get(key);
        }
    }

    public Scope getChildScope(Object scopeKey) {
        synchronized (childScopes) {
            if (!childScopes.containsKey(scopeKey)) {
                addChildScope(scopeKey);
            }

            return childScopes.get(scopeKey);
        }
    }

    public Scope addChildScope(Object scopeKey) {
        synchronized (childScopes) {
            Scope newScope = new Scope(scopeKey.toString(), this);
            childScopes.put(scopeKey, newScope);
            return newScope;
        }
    }

    public void removeChildScope(Object scopeKey) {
        synchronized (childScopes) {
            childScopes.remove(scopeKey);
        }
    }

    public void removeBeans() {
        synchronized (beans) {
            beans.clear();
        }
    }

    public void removeChildScopes() {
        synchronized (childScopes) {
            childScopes.clear();
        }
    }

    public Scope getParentScope() {
        return parentScope;
    }

    public void addBean(Object bean, String beanName) {
        synchronized (beans) {
            beans.put(bean, beanName);
        }
    }

    public void removeBean(Object bean) {
        synchronized (beans) {
            beans.remove(bean);
        }
    }

    public Iterable<Scope> getChildScopes() {
        synchronized (childScopes) {
            return childScopes.values();
        }
    }

    public Iterable<Map.Entry<Object, String>> getBeans() {
        synchronized (beans) {
            return beans.entrySet();
        }
    }

    public void setUi(UI ui) {
        this.ui = ui;
    }

    public UI getUi() {
        return ui;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        print(this, sb, 0);
        return sb.toString();
    }

    private static void print(Scope scope, StringBuilder sb, int level) {
        sb.append(StringUtils.repeat("|   ", level));
        sb.append("(");
        sb.append(scope.getName());
        sb.append(") ");
        scope.getBeans().forEach(e -> { sb.append(e.getValue()); sb.append(" "); });

        for (Scope childScope : scope.getChildScopes()) {
            sb.append("\n");
            print(childScope, sb, level + 1);
        }
    }
}
