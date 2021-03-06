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

package ratpack.handling;

import com.google.common.collect.ImmutableList;
import ratpack.handling.internal.Extractions;
import ratpack.registry.Registries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static ratpack.util.ExceptionUtils.uncheck;

/**
 * A super class that removes the boiler plate of retrieving objects from the context registry by injecting them based on a method signature.
 * <p>
 * Subclasses must implement exactly one method named {@code "handle"} that accepts a {@link Context} as the first parameter,
 * and at least one other parameter of any type.
 * <p>
 * The {@code handle(Context)} method of this class will delegate to the subclass handle method, supplying values for each parameter
 * by retrieving objects from the context and request (which are registries).
 * The context takes precedence over the request.
 * That is, if the context provides a value for the requested type it will be used regardless of whether the request also provides this type.
 * <p>
 * The following two handlers are functionally equivalent:
 * <pre class="tested">
 * import ratpack.handling.*;
 * import ratpack.file.FileSystemBinding;
 *
 * public class VerboseHandler implements Handler {
 *   public void handle(Context context) {
 *     FileSystemBinding fileSystemBinding = context.get(FileSystemBinding.class);
 *     context.render(fileSystemBinding.getFile().toString());
 *   }
 * }
 *
 * public class SuccinctHandler extends InjectionHandler {
 *   public void handle(Context context, FileSystemBinding fileSystemBinding) {
 *     context.render(fileSystemBinding.getFile().toString());
 *   }
 * }
 * </pre>
 * <p>
 * If the parameters cannot be satisfied, a {@link ratpack.registry.NotInRegistryException} will be thrown.
 * <p>
 * If there is no suitable {@code handle(Context, ...)} method, a {@link NoSuitableHandleMethodException} will be thrown at construction time.
 */
public abstract class InjectionHandler implements Handler {

  private final List<Class<?>> types;
  private final Method handleMethod;

  /**
   * Constructor.
   *
   * @throws NoSuitableHandleMethodException if this class doesn't provide a suitable handle method.
   */
  protected InjectionHandler() throws NoSuitableHandleMethodException {
    Class<?> thisClass = this.getClass();

    Method handleMethod = null;
    for (Method method : thisClass.getDeclaredMethods()) {
      if (!method.getName().equals("handle")) {
        continue;
      }

      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length < 2) {
        continue;
      }

      if (!parameterTypes[0].equals(Context.class)) {
        continue;
      }

      handleMethod = method;
      break;
    }

    if (handleMethod == null) {
      throw new NoSuitableHandleMethodException(thisClass);
    }

    this.handleMethod = handleMethod;
    Class<?>[] parameterTypes = handleMethod.getParameterTypes();
    this.types = ImmutableList.copyOf(Arrays.asList(parameterTypes).subList(1, parameterTypes.length));
  }

  /**
   * Invokes the custom "handle" method, extracting necessary parameters from the context to satisfy the call.
   *
   * @param context The context to handle
   */
  public final void handle(Context context) {
    Object[] args = new Object[types.size() + 1];
    args[0] = context;
    Extractions.extract(types, Registries.join(context.getRequest(), context), args, 1);
    try {
      handleMethod.invoke(this, args);
    } catch (IllegalAccessException e) {
      throw uncheck(e);
    } catch (InvocationTargetException e) {
      Throwable root = e.getTargetException();
      throw uncheck(root);
    }
  }

  /**
   * Exception thrown if the subclass doesn't provide a valid handle method.
   */
  public static class NoSuitableHandleMethodException extends RuntimeException {
    private static final long serialVersionUID = 0;

    private NoSuitableHandleMethodException(Class<?> clazz) {
      super("No injectable handle method found for " + clazz.getName());
    }
  }

}
