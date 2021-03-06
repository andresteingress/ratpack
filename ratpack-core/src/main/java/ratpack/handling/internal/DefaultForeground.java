/*
 * Copyright 2014 the original author or authors.
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

package ratpack.handling.internal;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import ratpack.handling.Context;
import ratpack.handling.Foreground;
import ratpack.handling.ReadOnlyContext;
import ratpack.handling.NoBoundContextException;

public class DefaultForeground implements Foreground {

  private final ThreadLocal<Context> contextThreadLocal;
  private final ListeningScheduledExecutorService listeningScheduledExecutorService;

  public DefaultForeground(ThreadLocal<Context> contextThreadLocal, ListeningScheduledExecutorService listeningScheduledExecutorService) {
    this.contextThreadLocal = contextThreadLocal;
    this.listeningScheduledExecutorService = listeningScheduledExecutorService;
  }

  @Override
  public ReadOnlyContext getContext() throws NoBoundContextException {
    Context context = contextThreadLocal.get();
    if (context == null) {
      throw new NoBoundContextException("No context is bound to the current thread (are you calling this from the background?)");
    } else {
      return context;
    }
  }

  @Override
  public ListeningScheduledExecutorService getExecutor() {
    return listeningScheduledExecutorService;
  }
}
