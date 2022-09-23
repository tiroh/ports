/*
 * Copyright 2022 Tim Rohlfs
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

package org.timux.ports.typescript;

import org.timux.ports.Message;
import org.timux.ports.Response;
import org.timux.ports.verification.Reporter;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TypeScriptAnnoProcessor extends AbstractProcessor {

  private Reporter reporter;
  private MessageConverterVisitor messageConverterVisitor;

  private final Set<String> unmodifiableSupportedAnnotationTypes;

  {
    Set<String> supportedAnnotationTypes = new HashSet<>();

    supportedAnnotationTypes.add(Message.class.getName());

    unmodifiableSupportedAnnotationTypes = Collections.unmodifiableSet(supportedAnnotationTypes);
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return unmodifiableSupportedAnnotationTypes;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    this.reporter = new Reporter(processingEnv);
    this.messageConverterVisitor = new MessageConverterVisitor(reporter);
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (annotations.isEmpty()) {
      return false;
    }

    for (Element element : roundEnv.getElementsAnnotatedWith(Message.class)) {
      element.accept(messageConverterVisitor, null);
    }

    return false;
  }
}
