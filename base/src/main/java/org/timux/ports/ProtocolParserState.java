/*
 * Copyright 2018-2022 Tim Rohlfs
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

import org.timux.ports.types.Either;
import org.timux.ports.types.Either3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

class ProtocolParserState {

    final String protocolIdentifier;
    String currentConditionMessageType = null;
    String currentWithRequestType = null;
    String currentWithResponseType = null;
    Object currentWithOwner = null;

    private Protocol.ConditionalActions currentConditionalActions = null;
    private List<Action> currentActions = null;

    ProtocolParserState(String protocolIdentifier) {
        this.protocolIdentifier = protocolIdentifier;
    }

    void reset() {
        currentConditionMessageType = null;
        currentWithRequestType = null;
        currentWithResponseType = null;
        currentWithOwner = null;
    }

    void registerMessageType(String messageType) {
        currentConditionMessageType = messageType;
    }

    void registerWithMessageTypeAndOwner(String requestType, String responseType, Object owner) {
        currentWithRequestType = requestType;
        currentWithResponseType = responseType;
        currentWithOwner = owner;
    }

    void registerWithMessageTypeAndOwner(String requestType, String responseTypeA, String responseTypeB, Object owner) {
        currentWithRequestType = requestType;
        currentWithResponseType = Either.class.getName() + "<" + responseTypeA + ", " + responseTypeB + ">";
        currentWithOwner = owner;
    }

    void registerWithMessageTypeAndOwner(String requestType, String responseTypeA, String responseTypeB, String responseTypeC, Object owner) {
        currentWithRequestType = requestType;
        currentWithResponseType = Either3.class.getName() + "<" + responseTypeA + ", " + responseTypeB + ", " + responseTypeC + ">";
        currentWithOwner = owner;
    }

    synchronized void registerConditionMessageType(Map<String, Protocol.ConditionalActions> conditions) {
        currentConditionalActions = conditions.get(currentConditionMessageType);

        if (currentConditionalActions == null) {
            currentConditionalActions = new Protocol.ConditionalActions();
            conditions.put(currentConditionMessageType, currentConditionalActions);
        }
    }

    <T> void registerCondition(Predicate<T> predicate) {
        currentActions = new ArrayList<>();
        currentConditionalActions.actions.add(new Protocol.ConditionalActionsTriple(protocolIdentifier, predicate, currentActions));
    }

    void registerAction(Action action) {
        currentActions.add(action);
    }

    Field extractOutPortField(Class<?> outPortType) {
        return extractOutPortField(outPortType, currentWithRequestType, currentWithResponseType);
    }

    private Field extractOutPortField(Class<?> outPortType, String requestType, String responseType) {
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
}
