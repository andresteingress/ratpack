/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ratpackframework.path.internal;

import org.ratpackframework.path.PathBinding;
import org.ratpackframework.path.PathContext;
import org.ratpackframework.routing.Exchange;
import org.ratpackframework.routing.Handler;

public class PathHandler implements Handler {

  private final PathBinding binding;
  private final Handler delegate;

  public PathHandler(PathBinding binding, Handler delegate) {
    this.binding = binding;
    this.delegate = delegate;
  }

  @Override
  public void handle(Exchange exchange) {
    PathContext childContext = binding.bind(exchange.getRequest().getPath(), exchange.maybeGet(PathContext.class));
    if (childContext != null) {
      exchange.nextWithContext(childContext, delegate);
    } else {
      exchange.next();
    }
  }
}