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

package ratpack.handling;

/**
 * Intercepts the processing of the request, primarily for traceability and recording metrics.
 * <p>
 * Interceptors are automatically detected and initialised when they are added to the context registry
 * (via {@link Context#next(ratpack.registry.Registry)} or {@link Context#insert(ratpack.registry.Registry, Handler...)}).
 * When they are detected, their {@link #init(Context)} method is called with the context they were registered with.
 * Immediately after, their {@link #intercept(ratpack.handling.ProcessingInterceptor.Type, Context, Runnable)} method will be called
 * (with a {@code type} of {@link ratpack.handling.ProcessingInterceptor.Type#FOREGROUND FOREGROUND}).
 * <p>
 * The interception methods <i>wrap</i> the rest of the execution.
 * They receive a <i>continuation</i> (as a {@link Runnable}) that <b>must</b> be called in order for processing to proceed.
 * <p>
 * The following example (in Groovy) demonstrates using a processing interceptor to time processing.
 * <pre class="tested">
 * import ratpack.launch.LaunchConfig
 * import ratpack.launch.LaunchConfigBuilder
 * import ratpack.handling.Context
 * import ratpack.handling.ProcessingInterceptor
 * import ratpack.test.embed.LaunchConfigEmbeddedApplication
 *
 * import static ratpack.registry.Registries.registry
 * import static ratpack.groovy.Groovy.chain
 * import static ratpack.groovy.test.TestHttpClients.testHttpClient
 *
 * import java.util.concurrent.atomic.AtomicLong
 *
 * class Timer {
 *   private final AtomicLong totalForeground = new AtomicLong()
 *   private final AtomicLong totalBackground = new AtomicLong()
 *
 *   private boolean background
 *
 *   private final ThreadLocal&lt;Long&gt; startedAt = new ThreadLocal() {
 *     protected Long initialValue() { 0 }
 *   }
 *
 *   void start(boolean background) {
 *     this.background = background
 *     startedAt.set(System.currentTimeMillis())
 *   }
 *
 *   void stop() {
 *     def startedAtTime = startedAt.get()
 *     startedAt.remove()
 *     def counter = background ? totalBackground : totalForeground
 *     counter.addAndGet(startedAtTime > 0 ? System.currentTimeMillis() - startedAtTime : 0)
 *   }
 *
 *   long getBackgroundTime() {
 *     totalBackground.get()
 *   }
 *
 *   long getForegroundTime() {
 *     totalForeground.get()
 *   }
 * }
 *
 * class ProcessingTimingInterceptor implements ProcessingInterceptor {
 *   void init(Context context) {
 *     context.request.register(new Timer())
 *   }
 *
 *   void intercept(ProcessingInterceptor.Type type, Context context, Runnable continuation) {
 *     context.request.get(Timer).with {
 *       start(type == ProcessingInterceptor.Type.BACKGROUND)
 *       continuation.run()
 *       stop()
 *     }
 *   }
 * }
 *
 * def interceptor = new ProcessingTimingInterceptor()
 * def application = new LaunchConfigEmbeddedApplication() {
 *   protected LaunchConfig createLaunchConfig() {
 *     LaunchConfigBuilder.
 *       baseDir(new File("some/path")).
 *       build { LaunchConfig config ->
 *         chain(config) {
 *           handler {
 *             next(registry(interceptor))
 *           }
 *           handler {
 *             sleep 100
 *             next()
 *           }
 *           get {
 *             sleep 100
 *             background {
 *               sleep 100
 *             } then {
 *               def timer = request.get(Timer)
 *               timer.stop()
 *               render "$timer.foregroundTime:$timer.backgroundTime"
 *             }
 *           }
 *         }
 *       }
 *   }
 * }
 *
 * def client = testHttpClient(application)
 *
 * def times = client.getText().split(":")*.toInteger()
 * int foregroundTime = times[0]
 * int backgroundTime = times[1]
 *
 * assert foregroundTime >= 200
 * assert backgroundTime >= 100
 *
 * application.close()
 * </pre>
 */
public interface ProcessingInterceptor {

  /**
   * The processing type (i.e. type of thread).
   */
  enum Type {
    /**
     * A thread from the background (i.e. blocking operation) pool.
     */
    BACKGROUND,

    /**
     * A thread from the foreground (i.e. computation/request handling) pool.
     */
    FOREGROUND
  }

  /**
   * Called once per request when the interceptor is registered with the context.
   * <p>
   * A typical thing to do in this method is register some kind of object with the request registry
   * that is retrieved later in {@link #intercept(ratpack.handling.ProcessingInterceptor.Type, Context, Runnable)}.
   * <p>
   * The {@link #intercept(ratpack.handling.ProcessingInterceptor.Type, Context, Runnable)} method will be called immediately
   * after this method so the rest of the processing can be intercepted.
   * <p>
   * All exceptions thrown by this method will be forwarded to {@link Context#error(Exception)} and will effectively halt processing.
   *
   * @param context The context at the time when the interceptor is registered.
   */
  void init(Context context);

  /**
   * Wraps the “rest” of the processing on the current thread.
   * <p>
   * The given {@code Runnable} argument represents the rest of the processing to occur on this thread.
   * This does not necessarily mean the rest of the processing until the rest of the response is determined or sent.
   * Request processing may involve multiple parallel (but not concurrent) threads of execution because of {@link ratpack.handling.Background} processing.
   * Moreover, the continuation includes all of the code that is executed after the response is determined (which ideally is just unravelling the call stack).
   * As such, when intercepting foreground execution that generates a response, the continuation may be returning <b>after</b> the response has been sent.
   * <p>
   * All exceptions thrown by this method will be <b>ignored</b>.
   *
   * @param type indicates whether this is a foreground (i.e. request handling) or background (i.e. blocking operation) thread
   * @param context the context at the time of interception
   * @param continuation the “rest” of the processing
   */
  void intercept(Type type, Context context, Runnable continuation);

}
