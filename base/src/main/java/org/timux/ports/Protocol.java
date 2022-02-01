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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({"unchecked", "rawtypes"})
final class Protocol {

    static class ConditionalActionsPair {

        final Predicate predicate;
        final List<Action> actions;

        ConditionalActionsPair(Predicate predicate, List<Action> actions) {
            this.predicate = predicate;
            this.actions = actions;
        }
    }

    static class ConditionalActions {
        final List<ConditionalActionsPair> actions = new ArrayList<>();
    }

    static class ResponseRegistry {
        final Map<String, Function<?, ?>> responseData = new HashMap<>(4);
    }

    private static class ProtocolComponent {
        Event eventPort = null;
        Request requestPort = null;
    }

    // keys = message type names
    private static final Map<String, ConditionalActions> conditionsOnSent = new HashMap<>();
    private static final Map<String, ConditionalActions> conditionsOnReceived = new HashMap<>();

    private static final Map<Object, ResponseRegistry> responseRegistries = new HashMap<>();

    private static final List<Object> componentRegistry = new ArrayList<>();

    private static final AtomicInteger nextProtocolId = new AtomicInteger();

    static boolean areProtocolsActive = false;

    private Protocol() {
        //
    }

    synchronized static void clear() {
        synchronized (responseRegistries) {
            areProtocolsActive = false;
            conditionsOnSent.clear();
            conditionsOnReceived.clear();
            responseRegistries.clear();
            componentRegistry.clear();
        }

        DomainManager.invalidate();
    }

    static void registerComponent(Object component) {
        componentRegistry.add(component);
    }

    static <T> void registerConditionOnSent(Predicate<T> predicate, ProtocolParserState state) {
        state.registerConditionMessageType(conditionsOnSent);
        state.registerCondition(predicate);
    }

    static <T> void registerConditionOnReceived(Predicate<T> predicate, ProtocolParserState state) {
        state.registerConditionMessageType(conditionsOnReceived);
        state.registerCondition(predicate);
    }

    static void registerRespondAction(Function<?, ?> response, ProtocolParserState state) {
        final String conditionMessageType = state.currentConditionMessageType;

        state.registerAction((x, owner) -> {
            synchronized (responseRegistries) {
                ResponseRegistry registry = responseRegistries.computeIfAbsent(owner, k -> new ResponseRegistry());
                registry.responseData.put(conditionMessageType, response);
            }
        });
    }

    public static void registerOrExecuteTriggerOrCallActionDependingOnParseState(Object payload, ProtocolParserState state) {
        Action action = createOperateOutPortAction(payload, state);

        if (action == null) {
            System.err.println("[ports] warning: no receivers are known for message type '" + state.currentWithRequestType + "'");
            return;
        }

        if (state.currentConditionMessageType == null) {
            action.execute(null, null);
        } else {
            state.registerAction(action);
        }
    }

    private static Action createOperateOutPortAction(Object payload, ProtocolParserState state) {
        Class<?> outPortType = state.currentWithRequestType.endsWith("Event") || state.currentWithRequestType.endsWith("Exception")
                ? Event.class
                : Request.class;

        if (state.currentWithOwner != null) {
            return createOperateOutPortOnOwnerAction(payload, outPortType, state);
        }

        return createOperateOutPortOnRegisteredComponentsAction(payload, outPortType, state);
    }

    private static Action createOperateOutPortOnOwnerAction(Object payload, Class<?> outPortType, ProtocolParserState state) {
        Field outPortField = state.extractOutPortField(outPortType);

        if (outPortType == Event.class) {
            try {
                Event eventPort = (Event) outPortField.get(state.currentWithOwner);
                return (x, owner) -> eventPort.trigger(payload);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        if (outPortType == Request.class) {
            try {
                Request requestPort = (Request) outPortField.get(state.currentWithOwner);
                return (x, owner) -> requestPort.call(payload);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        throw new IllegalStateException("unhandled OUT port type: " + outPortType.getName());
    }

    private static Action createOperateOutPortOnRegisteredComponentsAction(Object payload, Class<?> outPortType, ProtocolParserState state) {
        String portSignature = state.currentWithRequestType + ", " + state.currentWithResponseType;

        ProtocolComponent protocolComponent = new ProtocolComponent();

        Ports.domain("protocol-" + nextProtocolId.getAndIncrement(), DispatchPolicy.SYNCHRONOUS, SyncPolicy.DOMAIN)
                .addInstances(protocolComponent);

        Field outPortField = null;
        Action action = null;

        if (outPortType == Event.class) {
            protocolComponent.eventPort = new Event<>(state.currentWithRequestType, protocolComponent);

            action = (x, owner) -> protocolComponent.eventPort.trigger(payload);

            try {
                outPortField = ProtocolComponent.class.getDeclaredField("eventPort");
            } catch (NoSuchFieldException e) {
                throw new IllegalStateException(e);
            }
        }

        if (outPortType == Request.class) {
            protocolComponent.requestPort = new Request<>(
                    state.currentWithRequestType, state.currentWithResponseType, "requestPort", protocolComponent);

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

    static Function<?, ?> getResponseProviderIfAvailable(String messageType, Object owner) {
        synchronized (responseRegistries) {
            ResponseRegistry registry = responseRegistries.computeIfAbsent(owner, k -> new ResponseRegistry());
            return registry.responseData.remove(messageType);
        }
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
            if (data instanceof PortsEventException) {
                printEventExceptionWarning((PortsEventException) data);
            }

            return;
        }

        boolean actionWasExecuted = false;

        for (ConditionalActionsPair pair : conditionalActions.actions) {
            if (pair.predicate.test(data)) {
                for (Action action : pair.actions) {
                    action.execute(data, owner);
                    actionWasExecuted = true;
                }
            }
        }

        if (!actionWasExecuted && data instanceof PortsEventException) {
            printEventExceptionWarning((PortsEventException) data);
        }
    }

    private static void printEventExceptionWarning(PortsEventException exception) {
        Ports.printWarning(String.format("An event port terminated with an exception which was not "
                + "caught by an %s handler. The exception was:", PortsEventException.class.getName()));
        exception.printStackTrace();
    }
}
