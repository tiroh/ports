package org.timux.ports.spring;

import com.vaadin.flow.component.UI;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

class Scope {

    private String name;

    private Map<Object, Scope> childScopes = new HashMap<>();

    private Scope parentScope = null;

    private Map<Object, String> beans = new HashMap<>();

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
        return childScopes.get(key);
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

    public void removeChildScope(Object scopeKey) {
        childScopes.remove(scopeKey);
    }

    public void removeBeans() {
        beans.clear();
    }

    public void removeChildScopes() {
        childScopes.clear();
    }

    public Scope getParentScope() {
        return parentScope;
    }

    public void addBean(Object bean, String beanName) {
        beans.put(bean, beanName);
    }

    public void removeBean(Object bean) {
        beans.remove(bean);
    }

    public Iterable<Scope> getChildScopes() {
        return childScopes.values();
    }

    public Iterable<Map.Entry<Object, String>> getBeans() {
        return beans.entrySet();
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
