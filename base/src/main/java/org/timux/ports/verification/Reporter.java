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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

class Reporter {

    private final static Diagnostic.Kind DIAGNOSTIC_MESSAGE_KIND = Diagnostic.Kind.ERROR;

    private ProcessingEnvironment processingEnv;

    Reporter(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    void reportIssue(Element element, String message, Object... args) {
        processingEnv.getMessager().printMessage(
                DIAGNOSTIC_MESSAGE_KIND,
                String.format(message, args),
                element);
    }

    void reportIssue(Element element, AnnotationMirror mirror, String message, Object... args) {
        processingEnv.getMessager().printMessage(
                DIAGNOSTIC_MESSAGE_KIND,
                String.format(message, args),
                element,
                mirror);
    }
}
