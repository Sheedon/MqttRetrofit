/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (C) 2020 Sheedon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sheedon.mqtt.retrofit;

import androidx.annotation.Nullable;

import org.sheedon.mqtt.OkMqttClient;
import org.sheedon.mqtt.RequestBody;
import org.sheedon.mqtt.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static java.util.Collections.unmodifiableList;

/**
 * Retrofit adapts a Java interface to MQTT calls/observables by using annotations on the declared methods to
 * define how requests are made. Create instances using {@linkplain Builder the builder} and pass
 * your interface to {@link #create} to generate an implementation.
 *
 * <p>For example,
 *
 * <pre><code>
 * Retrofit retrofit = new Retrofit.Builder()
 *     .baseUrl("https://api.example.com/")
 *     .addConverterFactory(GsonConverterFactory.create())
 *     .build();
 *
 * MyApi api = retrofit.create(MyApi.class);
 * Response&lt;User&gt; user = api.getUser().execute();
 * </code></pre>
 *
 * @Author: sheedon by Bob Lee (bob@squareup.com),Jake Wharton (jw@squareup.com)
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/22 12:25
 */
public class Retrofit {
    private final Map<Method, ServiceMethod<?>> serviceMethodCache = new ConcurrentHashMap<>();

    final org.sheedon.mqtt.CallFactory callFactory;
    final org.sheedon.mqtt.ObservableFactory observableFactory;
    final String baseTopic;
    final List<Converter.Factory> converterFactories;
    final int defaultConverterFactoriesSize;
    final List<CallAdapter.Factory> callAdapterFactories;
    final int defaultCallAdapterFactoriesSize;
    final @Nullable
    Executor callbackExecutor;
    final boolean validateEagerly;
    final int timeout;

    Retrofit(org.sheedon.mqtt.CallFactory callFactory,
             org.sheedon.mqtt.ObservableFactory observableFactory,
             String baseTopic,
             List<Converter.Factory> converterFactories,
             int defaultConverterFactoriesSize,
             List<CallAdapter.Factory> adapterFactories,
             int defaultCallAdapterFactoriesSize,
             @Nullable Executor callbackExecutor, boolean validateEagerly,
             int defaultTimeout) {
        this.callFactory = callFactory;
        this.observableFactory = observableFactory;
        this.baseTopic = baseTopic;
        this.converterFactories = converterFactories;
        this.defaultConverterFactoriesSize = defaultConverterFactoriesSize;
        this.callAdapterFactories = adapterFactories; // Defensive copy at call site.
        this.defaultCallAdapterFactoriesSize = defaultCallAdapterFactoriesSize;
        this.callbackExecutor = callbackExecutor;
        this.validateEagerly = validateEagerly;
        this.timeout = defaultTimeout;

    }

    /**
     * 创建由{@code service}接口定义的API端点的实现。
     */
    public <T> T create(final Class<T> service) {
        validateServiceInterface(service);

        return (T) Proxy.newProxyInstance(service.getClassLoader(),
                new Class<?>[]{service},
                new InvocationHandler() {
                    private final Object[] emptyArgs = new Object[0];

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args)
                            throws Throwable {

                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(this, args);
                        }
                        args = args != null ? args : emptyArgs;
                        Platform platform = Platform.get();
                        return platform.isDefaultMethod(method)
                                ? platform.invokeDefaultMethod(method, service, proxy, args)
                                : loadServiceMethod(method).invoke(args);
                    }
                });

    }

    private void validateServiceInterface(Class<?> service) {
        if (!service.isInterface()) {
            throw new IllegalArgumentException("API declarations must be interfaces.");
        }

        if (validateEagerly) {
            Platform platform = Platform.get();
            for (Method method : service.getDeclaredMethods()) {
                if (!platform.isDefaultMethod(method) && !Modifier.isStatic(method.getModifiers())) {
                    loadServiceMethod(method);
                }
            }
        }
    }

    private ServiceMethod<?> loadServiceMethod(Method method) {
        ServiceMethod<?> result = serviceMethodCache.get(method);
        if (result != null) return result;

        synchronized (serviceMethodCache) {
            result = serviceMethodCache.get(method);
            if (result == null) {
                result = ServiceMethod.parseAnnotations(this, method);
                serviceMethodCache.put(method, result);
            }
        }
        return result;
    }

    /**
     * The factory used to create {@linkplain org.sheedon.mqtt.Call OkMqtt calls} for sending a MQTT requests.
     * Typically an instance of {@link OkMqttClient}.
     */
    public org.sheedon.mqtt.CallFactory callFactory() {
        return callFactory;
    }

    /**
     * The factory used to create {@linkplain org.sheedon.mqtt.Observable OkMqtt observables} for sending a MQTT requests.
     * Typically an instance of {@link OkMqttClient}.
     */
    public org.sheedon.mqtt.ObservableFactory observableFactory() {
        return observableFactory;
    }

    /**
     * The API base Topic.
     */
    public String baseTopic() {
        return baseTopic;
    }

    /**
     * Returns a list of the factories tried when creating a {@linkplain #callAdapter(Type,
     * Annotation[])} call adapter}.
     */
    public List<CallAdapter.Factory> callAdapterFactories() {
        return callAdapterFactories;
    }

    /**
     * Returns the {@link CallAdapter} for {@code returnType} from the available {@linkplain
     * #callAdapterFactories() factories}.
     *
     * @throws IllegalArgumentException if no call adapter available for {@code type}.
     */
    CallAdapter<?, ?> callAdapter(Type returnType, Annotation[] annotations) {
        return nextCallAdapter(null, returnType, annotations);
    }

    /**
     * Returns the {@link CallAdapter} for {@code returnType} from the available {@linkplain
     * #callAdapterFactories() factories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no call adapter available for {@code type}.
     */
    private CallAdapter<?, ?> nextCallAdapter(@Nullable CallAdapter.Factory skipPast, Type returnType,
                                              Annotation[] annotations) {
        Objects.requireNonNull(returnType, "returnType == null");
        Objects.requireNonNull(annotations, "annotations == null");

        int start = callAdapterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
            CallAdapter<?, ?> adapter = callAdapterFactories.get(i).get(returnType, annotations, this);
            if (adapter != null) {
                return adapter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate call adapter for ")
                .append(returnType)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(callAdapterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = callAdapterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(callAdapterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns an unmodifiable list of the factories tried when creating a {@linkplain
     * #requestBodyConverter(Type, Annotation[], Annotation[]) request body converter}, a {@linkplain
     * #responseBodyConverter(Type, Annotation[]) response body converter}, or a {@linkplain
     * #stringConverter(Type, Annotation[]) string converter}.
     */
    public List<Converter.Factory> converterFactories() {
        return converterFactories;
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link RequestBody} from the available
     * {@linkplain #converterFactories() factories}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    <T> Converter<T, String> requestBodyConverter(
            Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations) {
        return nextRequestBodyConverter(null, type, parameterAnnotations, methodAnnotations);
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link RequestBody} from the available
     * {@linkplain #converterFactories() factories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    <T> Converter<T, String> nextRequestBodyConverter(
            @Nullable Converter.Factory skipPast,
            Type type,
            Annotation[] parameterAnnotations,
            Annotation[] methodAnnotations) {
        Objects.requireNonNull(type, "type == null");
        Objects.requireNonNull(parameterAnnotations, "parameterAnnotations == null");
        Objects.requireNonNull(methodAnnotations, "methodAnnotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter.Factory factory = converterFactories.get(i);
            Converter<?, String> converter =
                    factory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, String>) converter;
            }
        }

        StringBuilder builder =
                new StringBuilder("Could not locate RequestBody converter for ").append(type).append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns a {@link Converter} for {@link ResponseBody} to {@code type} from the available
     * {@linkplain #converterFactories() factories}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    <T> Converter<ResponseBody, T> responseBodyConverter(Type type, Annotation[] annotations) {
        return nextResponseBodyConverter(null, type, annotations);
    }

    /**
     * Returns a {@link Converter} for {@link ResponseBody} to {@code type} from the available
     * {@linkplain #converterFactories() factories} except {@code skipPast}.
     *
     * @throws IllegalArgumentException if no converter available for {@code type}.
     */
    private <T> Converter<ResponseBody, T> nextResponseBodyConverter(
            @Nullable Converter.Factory skipPast, Type type, Annotation[] annotations) {
        Objects.requireNonNull(type, "type == null");
        Objects.requireNonNull(annotations, "annotations == null");

        int start = converterFactories.indexOf(skipPast) + 1;
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            Converter<ResponseBody, ?> converter =
                    converterFactories.get(i).responseBodyConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<ResponseBody, T>) converter;
            }
        }

        StringBuilder builder = new StringBuilder("Could not locate ResponseBody converter for ")
                .append(type)
                .append(".\n");
        if (skipPast != null) {
            builder.append("  Skipped:");
            for (int i = 0; i < start; i++) {
                builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
            }
            builder.append('\n');
        }
        builder.append("  Tried:");
        for (int i = start, count = converterFactories.size(); i < count; i++) {
            builder.append("\n   * ").append(converterFactories.get(i).getClass().getName());
        }
        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Returns a {@link Converter} for {@code type} to {@link String} from the available
     * {@linkplain #converterFactories() factories}.
     */
    <T> Converter<T, String> stringConverter(Type type, Annotation[] annotations) {
        Objects.requireNonNull(type, "type == null");
        Objects.requireNonNull(annotations, "annotations == null");

        for (int i = 0, count = converterFactories.size(); i < count; i++) {
            Converter<?, String> converter =
                    converterFactories.get(i).stringConverter(type, annotations, this);
            if (converter != null) {
                //noinspection unchecked
                return (Converter<T, String>) converter;
            }
        }

        // Nothing matched. Resort to default converter which just calls toString().
        //noinspection unchecked
        return (Converter<T, String>) BuiltInConverters.ToStringConverter.INSTANCE;
    }

    /**
     * The executor used for {@link Callback} methods on a {@link Call}. This may be {@code null}, in
     * which case callbacks should be made synchronously on the background thread.
     */
    public @Nullable Executor callbackExecutor() {
        return callbackExecutor;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private @Nullable
        org.sheedon.mqtt.CallFactory callFactory;
        private @Nullable
        org.sheedon.mqtt.ObservableFactory observableFactory;
        private String baseTopic;
        private final List<Converter.Factory> converterFactories = new ArrayList<>();
        private final List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>();
        private @Nullable
        Executor callbackExecutor;
        private boolean validateEagerly;
        private int timeout;

        public Builder() {
        }

        Builder(Retrofit retrofit) {
            callFactory = retrofit.callFactory;
            observableFactory = retrofit.observableFactory;
            baseTopic = retrofit.baseTopic;

            // Do not add the default BuiltIntConverters and platform-aware converters added by build().
            for (int i = 1,
                 size = retrofit.converterFactories.size() - retrofit.defaultConverterFactoriesSize;
                 i < size;
                 i++) {
                converterFactories.add(retrofit.converterFactories.get(i));
            }

            // Do not add the default, platform-aware call adapters added by build().
            for (int i = 0,
                 size =
                 retrofit.callAdapterFactories.size() - retrofit.defaultCallAdapterFactoriesSize;
                 i < size;
                 i++) {
                callAdapterFactories.add(retrofit.callAdapterFactories.get(i));
            }

            callbackExecutor = retrofit.callbackExecutor;
            validateEagerly = retrofit.validateEagerly;
        }

        /**
         * The MQTT client used for requests.
         * <p>
         * This is a convenience method for calling {@link #callFactory}.
         */
        public Builder client(OkMqttClient client) {
            OkMqttClient mqttClient = Objects.requireNonNull(client, "client == null");
            this.timeout = client.getDefaultTimeout();
            callFactory(mqttClient );
            observableFactory(mqttClient);
            return this;
        }

        /**
         * Specify a custom call factory for creating {@link Call} instances.
         * <p>
         * Note: Calling {@link #client} automatically sets this value.
         */
        public Builder callFactory(org.sheedon.mqtt.CallFactory factory) {
            this.callFactory = Objects.requireNonNull(factory, "CallFactory == null");
            return this;
        }

        /**
         * Specify a custom call factory for creating {@link Observable} instances.
         * <p>
         * Note: Calling {@link #client} automatically sets this value.
         */
        public Builder observableFactory(org.sheedon.mqtt.ObservableFactory factory) {
            this.observableFactory = Objects.requireNonNull(factory, "ObservableFactory == null");
            return this;
        }

        /**
         * Set the API base Topic.
         */
        public Builder baseTopic(String baseTopic) {
            this.baseTopic = Objects.requireNonNull(baseTopic, "baseTopic == null");
            return this;
        }


        /**
         * Add converter factory for serialization and deserialization of objects.
         */
        public Builder addConverterFactory(Converter.Factory factory) {
            converterFactories.add(Objects.requireNonNull(factory, "factory == null"));
            return this;
        }

        /**
         * Add a call adapter factory for supporting service method return types other than {@link
         * Call}.
         */
        public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
            callAdapterFactories.add(Objects.requireNonNull(factory, "factory == null"));
            return this;
        }

        /**
         * The executor on which {@link Callback} methods are invoked when returning {@link Call} from
         * your service method.
         * <p>
         * Note: {@code executor} is not used for {@linkplain #addCallAdapterFactory custom method
         * return types}.
         */
        public Builder callbackExecutor(Executor executor) {
            this.callbackExecutor = Objects.requireNonNull(executor, "executor == null");
            return this;
        }

        /**
         * Returns a modifiable list of call adapter factories.
         */
        public List<CallAdapter.Factory> callAdapterFactories() {
            return this.callAdapterFactories;
        }

        /**
         * Returns a modifiable list of converter factories.
         */
        public List<Converter.Factory> converterFactories() {
            return this.converterFactories;
        }

        /**
         * When calling {@link #create} on the resulting {@link Retrofit} instance, eagerly validate
         * the configuration of all methods in the supplied interface.
         */
        public Builder validateEagerly(boolean validateEagerly) {
            this.validateEagerly = validateEagerly;
            return this;
        }


        public Retrofit build() {
            if (baseTopic == null) {
                baseTopic = "";
            }

            Platform platform = Platform.get();

            org.sheedon.mqtt.CallFactory callFactory = this.callFactory;
            if (callFactory == null) {
                throw new IllegalStateException("callFactory is null.");
            }

            org.sheedon.mqtt.ObservableFactory observableFactory = this.observableFactory;
            if (observableFactory == null) {
                throw new IllegalStateException("callFactory is null.");
            }

            Executor callbackExecutor = this.callbackExecutor;
            if (callbackExecutor == null) {
                callbackExecutor = platform.defaultCallbackExecutor();
            }

            // Make a defensive copy of the adapters and add the default Call adapter.
            List<CallAdapter.Factory> callAdapterFactories = new ArrayList<>(this.callAdapterFactories);
            List<? extends CallAdapter.Factory> defaultCallAdapterFactories =
                    platform.createDefaultCallAdapterFactories(callbackExecutor);
            callAdapterFactories.addAll(defaultCallAdapterFactories);

            // Make a defensive copy of the converters.
            List<? extends Converter.Factory> defaultConverterFactories =
                    platform.createDefaultConverterFactories();
            int defaultConverterFactoriesSize = defaultConverterFactories.size();
            List<Converter.Factory> converterFactories =
                    new ArrayList<>(1 + this.converterFactories.size() + defaultConverterFactoriesSize);

            // Add the built-in converter factory first. This prevents overriding its behavior but also
            // ensures correct behavior when using converters that consume all types.
            converterFactories.add(new BuiltInConverters());
            converterFactories.addAll(this.converterFactories);
            converterFactories.addAll(defaultConverterFactories);

            return new Retrofit(callFactory, observableFactory, baseTopic,
                    unmodifiableList(converterFactories),
                    defaultConverterFactoriesSize,
                    unmodifiableList(callAdapterFactories),
                    defaultCallAdapterFactories.size(),
                    callbackExecutor, validateEagerly, timeout);
        }
    }
}
