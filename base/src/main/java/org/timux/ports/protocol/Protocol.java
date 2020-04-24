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

package org.timux.ports.protocol;

import org.timux.ports.Event;
import org.timux.ports.Request;

import java.util.*;
import java.util.function.Predicate;

public class Protocol {

    public static class ConditionalActions {

        private Map<Predicate, List<Action>> actions = new LinkedHashMap<>();
    }

    private static Map<Object, ConditionalActions> conditionsOnSent = new HashMap<>();
    private static Map<Object, ConditionalActions> conditionsOnReceived = new HashMap<>();

    private static Object currentConditionPort = null;
    private static Object currentWithPort = null;
    private static ConditionalActions currentConditionalActions = null;
    private static List<Action> currentActions = null;

    public static void registerConditionPort(Object port) {
        currentConditionPort = port;
    }

    public static void registerWithPort(Object port) {
        currentWithPort = port;
    }

    private static void registerConditionPort(Map<Object, ConditionalActions> conditions, Object port) {
        currentConditionalActions = conditions.get(port);

        if (currentConditionalActions == null) {
            currentConditionalActions = new ConditionalActions();
            conditions.put(port, currentConditionalActions);
        }
    }

    public static <T> void registerConditionOnSent(Predicate<T> predicate) {
        registerConditionPort(conditionsOnSent, currentConditionPort);
        registerCondition(predicate);
    }

    public static <T> void registerConditionOnReceived(Predicate<T> predicate) {
        registerConditionPort(conditionsOnReceived, currentConditionPort);
        registerCondition(predicate);
    }

    private static <T> void registerCondition( Predicate<T> predicate) {
        currentActions = currentConditionalActions.actions.get(predicate);

        if (currentActions == null) {
            currentActions = new ArrayList<>();
            currentConditionalActions.actions.put(predicate, currentActions);
        }
    }

    public static void registerAction(Action action) {
        currentActions.add(action);
    }

    public static void registerTrigger(Object payload) {
        final Object withPort = currentWithPort;
        registerAction(() -> ((Event) withPort).trigger(payload));
    }

    public static void registerCall(Object payload) {
        final Object withPort = currentWithPort;
        registerAction(() -> ((Request) withPort).call(payload));
    }

    public static void onDataSent(Object port, Object data) {
        onDataEvent(conditionsOnSent, port, data);
    }

    public static <O> void onDataReceived(Object port, O data) {
        onDataEvent(conditionsOnReceived, port, data);
    }

    public static void onDataEvent(Map<Object, ConditionalActions> conditionalActionsMap, Object port, Object data) {
        ConditionalActions conditionalActions = conditionalActionsMap.get(port);

        if (conditionalActions == null) {
            return;
        }

        for (Map.Entry<Predicate, List<Action>> e : conditionalActions.actions.entrySet()) {
            if (e.getKey().test(data)) {
                for (Action action : e.getValue()) {
                    action.execute();
                }
            }
        }
    }

    public static void executeActions() {

    }
}
