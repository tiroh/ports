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

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({"unchecked", "rawtypes"})
final class Protocol {

    private static class ConditionalActions {
        private final Map<Predicate, List<Action>> actions = new LinkedHashMap<>();
    }

    private static class ResponseRegistry {
        private final Map<String, Function<?, ?>> responseData = new LinkedHashMap<>();
    }

    private static class ProtocolComponent {
        Event eventPort = null;
        Request requestPort = null;
    }

    // keys = message type names
    private final static Map<String, ConditionalActions> conditionsOnSent = new HashMap<>();
    private final static Map<String, ConditionalActions> conditionsOnReceived = new HashMap<>();

    private final static Map<Object, ResponseRegistry> responseRegistries = new HashMap<>();

    private final static List<Object> componentRegistry = new ArrayList<>();

    private static String currentConditionMessageType = null;
    private static String currentWithRequestType = null;
    private static String currentWithResponseType = null;
    private static Object currentWithOwner = null;

    private static ConditionalActions currentConditionalActions = null;
    private static List<Action> currentActions = null;

    static boolean areProtocolsActive = false;

    private Protocol() {
        //
    }

    static void clear() {
        areProtocolsActive = false;
        conditionsOnSent.clear();
        conditionsOnReceived.clear();
        responseRegistries.clear();
        componentRegistry.clear();
        resetParseState();
    }

    static void resetParseState() {
        currentConditionMessageType = null;
        currentWithRequestType = null;
        currentWithResponseType = null;
        currentWithOwner = null;
    }

    static void registerComponent(Object component) {
        componentRegistry.add(component);
    }

    public static void registerMessageType(String messageType) {
        currentConditionMessageType = messageType;
    }

    public static void registerWithMessageTypeAndOwner(String requestType, String responseType, Object owner) {
        currentWithRequestType = requestType;
        currentWithResponseType = responseType;
        currentWithOwner = owner;
    }

    private static void registerConditionMessageType(Map<String, ConditionalActions> conditions, String messageType) {
        currentConditionalActions = conditions.get(messageType);

        if (currentConditionalActions == null) {
            currentConditionalActions = new ConditionalActions();
            conditions.put(messageType, currentConditionalActions);
        }
    }

    public static <T> void registerConditionOnSent(Predicate<T> predicate) {
        registerConditionMessageType(conditionsOnSent, currentConditionMessageType);
        registerCondition(predicate);
    }

    public static <T> void registerConditionOnReceived(Predicate<T> predicate) {
        registerConditionMessageType(conditionsOnReceived, currentConditionMessageType);
        registerCondition(predicate);
    }

    private static <T> void registerCondition(Predicate<T> predicate) {
        currentActions = currentConditionalActions.actions.computeIfAbsent(predicate, k -> new ArrayList<>());
    }

    public static void registerAction(Action action) {
        currentActions.add(action);
    }

    public static void registerOrExecuteTriggerOrCallActionDependingOnParseState(Object payload) {
        Action action = createOperateOutPortAction(payload);

        if (action == null) {
            System.err.println("[ports] warning: no receivers are known for event type " + currentWithRequestType);
            return;
        }

        if (currentConditionMessageType == null) {
            action.execute(null, null);
        } else {
            registerAction(action);
        }
    }

    private static Action createOperateOutPortAction(Object payload) {
        Class<?> outPortType = currentWithRequestType.endsWith("Event") || currentWithRequestType.endsWith("Exception")
                ? Event.class
                : Request.class;

        if (currentWithOwner != null) {
            return createOperateOutPortOnOwnerAction(payload, outPortType);
        }

        if (!componentRegistry.isEmpty()) {
            return createOperateOutPortOnRegisteredComponentsAction(payload, outPortType);
        }

        return null;
    }

    private static Action createOperateOutPortOnOwnerAction(Object payload, Class<?> outPortType) {
        Field outPortField = extractOutPortField(outPortType, currentWithRequestType, currentWithResponseType);

        if (outPortType == Event.class) {
            try {
                Event eventPort = (Event) outPortField.get(currentWithOwner);
                return (x, owner) -> eventPort.trigger(payload);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        if (outPortType == Request.class) {
            try {
                Request requestPort = (Request) outPortField.get(currentWithOwner);
                return (x, owner) -> requestPort.call(payload);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        throw new IllegalStateException("unhandled OUT port type: " + outPortType.getName());
    }

    private static Action createOperateOutPortOnRegisteredComponentsAction(Object payload, Class<?> outPortType) {
        String portSignature = currentWithRequestType + ", " + currentWithResponseType;

        ProtocolComponent protocolComponent = new ProtocolComponent();

        Field outPortField = null;
        Action action = null;

        if (outPortType == Event.class) {
            protocolComponent.eventPort = new Event<>(currentWithRequestType, protocolComponent);

            action = (x, owner) -> protocolComponent.eventPort.trigger(payload);

            try {
                outPortField = ProtocolComponent.class.getDeclaredField("eventPort");
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
        }

        if (outPortType == Request.class) {
            protocolComponent.requestPort = new Request<>(currentWithRequestType, "requestPort", protocolComponent);

            action = (x, owner) -> protocolComponent.requestPort.call(payload);

            try {
                outPortField = ProtocolComponent.class.getDeclaredField("requestPort");
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        if (outPortField == null) {
            throw new IllegalStateException("unhandled OUT port type: " + outPortType.getName());
        }

        outPortField.setAccessible(true);

        for (Object component : componentRegistry) {
            try {
                Ports.connectSinglePort(outPortField, portSignature, protocolComponent, component, PortsOptions.FORCE_CONNECT_ALL);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return action;
    }

    private static Field extractOutPortField(Class<?> outPortType, String requestType, String responseType) {
        String relevantFieldType = outPortType.getName() + "<" + requestType +
                (void.class.getName().equals(responseType) ? "" : ", " + responseType)
                + ">";

        for (Field field : currentWithOwner.getClass().getDeclaredFields()) {
            if (field.getGenericType().getTypeName().equals(relevantFieldType)) {
                field.setAccessible(true);
                return field;
            }
        }

        throw new PortNotFoundException(
                currentWithRequestType,
                currentWithResponseType,
                currentWithOwner.getClass().getName());
    }

    public static void registerRespondAction(Function<?, ?> response) {
        final String conditionMessageType = currentConditionMessageType;

        registerAction((x, owner) -> {
            ResponseRegistry registry = responseRegistries.computeIfAbsent(owner, k -> new ResponseRegistry());
            registry.responseData.put(conditionMessageType, response);
        });
    }

    static Function<?, ?> getResponseProviderIfAvailable(String messageType, Object owner) {
        ResponseRegistry registry = responseRegistries.computeIfAbsent(owner, k -> new ResponseRegistry());
        return registry.responseData.remove(messageType);
    }

    static void onDataSent(String messageType, Object owner, Object data) {
        onDataEvent(conditionsOnSent, messageType, owner, data);
    }

    static <O> void onDataReceived(String messageType, Object owner, O data) {
        onDataEvent(conditionsOnReceived, messageType, owner, data);
    }

    private static void onDataEvent(
            Map<String, ConditionalActions> conditionalActionsMap, String messageType, Object owner, Object data)
    {
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
