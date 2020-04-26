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
import org.timux.ports.protocol.syntax.Action;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class Protocol {

    private static class ConditionalActions {
        private final Map<Predicate, List<Action>> actions = new LinkedHashMap<>();
    }

    private static class ResponseRegistry {
        private final Map<String, Function<?, ?>> responseData = new LinkedHashMap<>();
    }

    // keys = message type names
    private final static Map<String, ConditionalActions> conditionsOnSent = new HashMap<>();
    private final static Map<String, ConditionalActions> conditionsOnReceived = new HashMap<>();

    private final static Map<Object, ResponseRegistry> responseRegistries = new HashMap<>();

    private final static List<Object> componentRegistry = new ArrayList<>();

    private static String currentConditionMessageType = null;
    private static String currentWithMessageType = null;

    private static ConditionalActions currentConditionalActions = null;
    private static List<Action> currentActions = null;

    private static Event<?> eventBroadcaster;

    public static boolean areProtocolsActive = false;

    private Protocol() {
        //
    }

    public static void clear() {
        areProtocolsActive = false;
        conditionsOnSent.clear();
        conditionsOnReceived.clear();
        responseRegistries.clear();
        componentRegistry.clear();
    }

    public static void registerComponent(Object component) {
        componentRegistry.add(component);
    }

    public static void registerMessageType(String messageType) {
        currentConditionMessageType = messageType;
    }

    public static void registerWithMessageType(String messageType) {
        currentWithMessageType = messageType;
    }

    private static void registerConditionPort(Map<Object, ConditionalActions> conditions, Object port) {
        currentConditionalActions = conditions.get(port);

        if (currentConditionalActions == null) {
            currentConditionalActions = new ConditionalActions();
            conditions.put(port, currentConditionalActions);
        }
    }

    private static void registerConditionMessageType(Map<String, ConditionalActions> conditions, String messageType) {
        currentConditionalActions = conditions.get(messageType);

        if (currentConditionalActions == null) {
            currentConditionalActions = new ConditionalActions();
            conditions.put(messageType, currentConditionalActions);
        }
    }

    public static <T> void registerConditionOnSent(Predicate<T> predicate) {
//        registerConditionPort(conditionsOnSent, currentConditionPort);
        registerConditionMessageType(conditionsOnSent, currentConditionMessageType);
        registerCondition(predicate);
    }

    public static <T> void registerConditionOnReceived(Predicate<T> predicate) {
//        registerConditionPort(conditionsOnReceived, currentConditionPort);
        registerConditionMessageType(conditionsOnReceived, currentConditionMessageType);
        registerCondition(predicate);
    }

    private static <T> void registerCondition(Predicate<T> predicate) {
        currentActions = currentConditionalActions.actions.get(predicate);

        if (currentActions == null) {
            currentActions = new ArrayList<>();
            currentConditionalActions.actions.put(predicate, currentActions);
        }
    }

    public static void registerAction(Action action) {
        currentActions.add(action);
    }

    public static void registerTriggerAction(Object payload) {
//        final String messageType = currentWithMessageType;
//        registerAction((x, owner) -> ((Event) withPort).trigger(payload));
    }

    public static void registerCallAction(Object payload) {
//        final String messageType = currentWithMessageType;
//        registerAction((x, owner) -> ((Request) withPort).call(payload));
    }

    public static void registerRespondAction(Function<?, ?> response) {
        final String conditionMessageType = currentConditionMessageType;

        registerAction((x, owner) -> {
            ResponseRegistry registry = responseRegistries.computeIfAbsent(owner, k -> new ResponseRegistry());
            registry.responseData.put(conditionMessageType, response);
        });
    }

    public static void clearResponseRegistry(String messageType, Object owner) {
        ResponseRegistry registry = responseRegistries.computeIfAbsent(owner, k -> new ResponseRegistry());
        registry.responseData.remove(messageType);
    }

    public static Object getResponseIfAvailable(String messageType, Object owner) {
        ResponseRegistry registry = responseRegistries.computeIfAbsent(owner, k -> new ResponseRegistry());
        return registry.responseData.remove(messageType);
    }

    public static void onDataSent(String messageType, Object owner, Object data) {
        onDataEvent(conditionsOnSent, messageType, owner, data);
    }

    public static <O> void onDataReceived(String messageType, Object owner, O data) {
        onDataEvent(conditionsOnReceived, messageType, owner, data);
    }

    public static void onDataEvent(Map<String, ConditionalActions> conditionalActionsMap, String messageType, Object owner, Object data) {
        ConditionalActions conditionalActions = conditionalActionsMap.get(messageType);

        if (conditionalActions == null || conditionalActions.actions.isEmpty()) {
            return;
        }

        for (Map.Entry<Predicate, List<Action>> e : conditionalActions.actions.entrySet()) {
            if (e.getKey().test(data)) {
                for (Action action : e.getValue()) {
                    action.execute(data, owner);
                }
            }
        }
    }
}
