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
 
package org.timux.ports.verification;

import org.timux.ports.types.Either;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

class VerificationModel {

    private final Map<Element, HashSet<String>> inPortNames = new HashMap<>();
    private final Map<String, String> portSignatures = new HashMap<>();
    private final Map<Element, String> successResponses = new HashMap<>();
    private final Map<Element, String> failureResponses = new HashMap<>();

    private final Reporter reporter;

    VerificationModel(Reporter reporter) {
        this.reporter = reporter;
    }

    String getRegisteredResponseType(String messageType) {
        return portSignatures.get(messageType);
    }

    void verifyAndRegisterInPortName(String portName, Element element) {
        HashSet<String> names = inPortNames.computeIfAbsent(element.getEnclosingElement(), k -> new HashSet<>());

        if (names.contains(portName)) {
            reporter.reportIssue(element, "duplicate IN port [%s]", portName);
        }

        names.add(portName);
    }

    void verifyAndRegisterResponseType(String messageType, String responseType, String portName, Element element) {
        verifyAndRegisterResponseType(messageType, responseType, portName, element, null);
    }

    void verifyAndRegisterResponseType(String messageType, String responseType, Element element, AnnotationMirror mirror) {
        verifyAndRegisterResponseType(messageType, responseType, "", element, mirror);
    }

    private void verifyAndRegisterResponseType(String messageType, String responseType, String portName, Element element, AnnotationMirror mirror) {
        String registeredResponseType = getRegisteredResponseType(messageType);

        if (registeredResponseType == null) {
            portSignatures.put(messageType, responseType);
        } else {
            if (!registeredResponseType.equals(responseType)) {
                String portNotice = portName.isEmpty()
                        ? ""
                        : String.format("port signatures do not match for OUT port [%s]: ", portName);

                String notice = portNotice + "message type '%s' cannot be mapped to both '%s' and '%s'";

                if (mirror == null) {
                    reporter.reportIssue(element, notice, messageType, registeredResponseType, responseType);
                } else {
                    reporter.reportIssue(element, mirror, notice, messageType, registeredResponseType, responseType);
                }
            }
        }
    }

    void verifyAndRegisterSuccessResponseType(String messageType, String successResponseType, Element element, AnnotationMirror mirror) {
        successResponses.put(element, successResponseType);

        String failureResponseType = failureResponses.remove(element);

        if (failureResponseType != null) {
            successResponses.remove(element);
            String responseType = String.format("%s<%s,%s>", Either.class.getName(), successResponseType, failureResponseType);
            verifyAndRegisterResponseType(messageType, responseType, element, mirror);
        }
    }

    void verifyAndRegisterFailureResponseType(String messageType, String failureResponseType, Element element, AnnotationMirror mirror) {
        failureResponses.put(element, failureResponseType);

        String successResponseType = successResponses.remove(element);

        if (successResponseType != null) {
            failureResponses.remove(element);
            String responseType = String.format("%s<%s,%s>", Either.class.getName(), successResponseType, failureResponseType);
            verifyAndRegisterResponseType(messageType, responseType, element, mirror);
        }
    }

    void verifyThatNoSuccessOrFailureResponseTypesStandAlone() {
        for (Map.Entry<Element, String> e : successResponses.entrySet()) {
            Element element = e.getKey();
            reporter.reportIssue(element, "a failure response type must be provided");
        }

        for (Map.Entry<Element, String> e : failureResponses.entrySet()) {
            Element element = e.getKey();
            reporter.reportIssue(element, "a success response type must be provided");
        }
    }

    void verifyAndRegisterInPortResponseType(String messageType, String responseType, String portName, Element element) {
        String registeredResponseType = portSignatures.get(messageType);

        String registeredReturnTypeInfo = registeredResponseType != null
                ? " (" + registeredResponseType + ")"
                : "";

        if (responseType.equals("void")) {
            reporter.reportIssue(element, "IN port [%s] must return a value%s", portName, registeredReturnTypeInfo);
        } else if (responseType.equals(Void.class.getName())) {
            reporter.reportIssue(element, "IN port [%s] has inadmissible response type (%s)", portName, responseType);
        } else {
            verifyAndRegisterResponseType(messageType, responseType, portName, element);
        }
    }
}
