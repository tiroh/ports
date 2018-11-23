package org.timux.ports.spring;

import java.util.*;

class Scope {

    private String name;

    private Map<Object, Scope> childScopes = new HashMap<>();

    private Scope parentScope = null;

    private Map<Object, String> beans = new HashMap<>();

    public Scope(String name) {
        this(name, null);
    }

    public Scope(String name, Scope parentScope) {
        this.name = name;
        this.parentScope = parentScope;
    }

    public Scope getChildScope(Object scopeKey) {
        if (!childScopes.containsKey(scopeKey)) {
            addChildScope(scopeKey);
        }

        return childScopes.get(scopeKey);
    }

    public Scope addChildScope(Object scopeKey) {
        Scope newScope = new Scope(scopeKey.toString(), this);
        childScopes.put(scopeKey, newScope);
        return newScope;
    }

    public Scope getParentScope() {
        return parentScope;
    }

    public void addBean(Object bean, String beanName) {
        beans.put(bean, beanName);
    }

    public Iterable<Scope> getChildScopes() {
        return childScopes.values();
    }

    public Iterable<Map.Entry<Object, String>> getBeans() {
        return beans.entrySet();
    }
}
