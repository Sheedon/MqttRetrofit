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

import org.sheedon.mqtt.CallFactory;
import org.sheedon.mqtt.ObservableFactory;
import org.sheedon.mqtt.OkMqttClient;
import org.sheedon.mqtt.RequestBody;
import org.sheedon.mqtt.ResponseBody;
import org.sheedon.mqtt.Subscribe;

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
 * Retrofit 通过在声明的方法上使用注释来定义如何发出请求，从而使 Java 接口适应 MQTT 调用可观察对象。
 * 使用 {@linkplain Builder the builder} 创建实例并将您的接口传递给 {@link #create(Class)} 以生成实现。
 *
 * <p>例如，
 *
 * <pre><code>
 * Retrofit retrofit = new Retrofit.Builder()
 *     .baseUrl("mq/clouds/cmd")
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

    final CallFactory callFactory;
    final ObservableFactory observableFactory;
    final String baseTopic;
    final List<Converter.Factory> converterFactories;
    final int defaultConverterFactoriesSize;
    final List<CallAdapter.Factory> callAdapterFactories;
    final int defaultCallAdapterFactoriesSize;
    final @Nullable
    Executor callbackExecutor;
    final boolean validateEagerly;
    final int timeout;

    Retrofit(CallFactory callFactory,
             ObservableFactory observableFactory,
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
     * 由 {@code service} 创建接口定义的 API 端点的实现。
     *
     * <p>
     * 1. 订阅主题的相对路径是从所创建接口的方法上的注解{@link org.sheedon.mqtt.retrofit.mqtt.SUBSCRIBE
     * SUBSCRIBE}中获得的。
     * 2. 请求消息的主题相对路径是从所创建接口的方法上的注解{@link org.sheedon.mqtt.retrofit.mqtt.TOPIC
     * TOPIC}中获得的。
     * 3. 对于发送mqtt消息，需要在超时时间范围内得到响应结果，可以通过从所创建接口的方法上的注解
     * {@link org.sheedon.mqtt.retrofit.mqtt.TIMEOUT TIMEOUT}中获得超时时间，超时未接收到数据则向客户端发送超时消息。
     * 4. 对于发送mqtt数据包，若为固定数据，则可通过从所创建接口的方法上的注解{@link org.sheedon.mqtt.retrofit.mqtt.PAYLOAD
     * PAYLOAD}来配置。
     * 5. 配置mqtt消息的有效载荷编码格式，可以通过从所创建接口的方法上的注解{@link org.sheedon.mqtt.retrofit.mqtt.CHARSET
     * CHARSET}来配置。
     * 6. 若订阅内容并不是消息主题，而是需要通过消息或部分主题进行匹配，那么可以通过创建接口的方法上的注解
     * {@link org.sheedon.mqtt.retrofit.mqtt.KEYWORD KEYWORD}来配置。
     *
     * <p>方法参数可用于替换部分「订阅主题/发送消息主题/消息有效载荷」，方法是用注解
     * {@link org.sheedon.mqtt.retrofit.mqtt.Path @Path} 配置它们。替换部分由大括号包围的标识符表示（例如，“{foo}”）。
     * 要将项目根据{@link org.sheedon.mqtt.retrofit.mqtt.Path.type()} 类型，将内容添加到指定的配置字符串上。
     *
     * <p>请求的主体由 {@link org.sheedon.mqtt.retrofit.mqtt.Body @Body} 注解表示。
     * 该对象将由 {@link Converter.Factory} 实例之一转换为请求表示。
     * {@link RequestBody}、{@link Subscribe} 也可以用于原始表示。
     *
     * <p>方法注释和相应的参数注释支持替代请求正文格式:
     *
     * <ul>
     *   <li>{@link org.sheedon.mqtt.retrofit.mqtt.FormEncoded @FormEncoded} -
     *   具有由 {@link org.sheedon.mqtt.retrofit.mqtt.Field @Field} 参数注释指定的键值对的表单编码数据。
     * </ul>
     *
     * <p>默认情况下，方法返回一个代表 MQTT 请求的 {@link Call}或者{@link Observable}。调用的通用参数是响应主体类型，
     * 将由 {@link Converter.Factory} 实例之一进行转换。 {@link ResponseBody} 也可以用于原始表示。
     * 如果您不关心正文内容，可以使用 {@link Void}。
     *
     * <p>例如:
     *
     * <pre>
     * public interface CategoryService {
     *   &#64;TOPIC("mq/device/data/{type}")
     *   &#64;PAYLOAD("test")
     *   &#64;SUBSCRIBE("mq/clouds/cmd/{type}")
     *   Call&lt;List&lt;Item&gt;&gt; categoryList(@Path("type") String a, @Path(value = "type" type = PathType.SUBSCRIBE) String b);
     * }
     * </pre>
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
    public CallFactory callFactory() {
        return callFactory;
    }

    /**
     * The factory used to create {@linkplain org.sheedon.mqtt.Observable OkMqtt observables} for sending a MQTT requests.
     * Typically an instance of {@link OkMqttClient}.
     */
    public ObservableFactory observableFactory() {
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

    FormBodyConverter formBodyConverter() {
        for (int i = 0, count = converterFactories.size(); i < count; i++) {
            FormBodyConverter converter = converterFactories.get(i).formBodyConverter();
            if (converter != null) {
                //noinspection unchecked
                return converter;
            }
        }

        return new FormBody.Builder();
    }

    /**
     * The executor used for {@link Callback} methods on a {@link Call}. This may be {@code null}, in
     * which case callbacks should be made synchronously on the background thread.
     */
    public @Nullable
    Executor callbackExecutor() {
        return callbackExecutor;
    }

    public Builder newBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private @Nullable
        CallFactory callFactory;
        private @Nullable
        ObservableFactory observableFactory;
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
         * 用于请求的 MQTT 客户端。
         * <p>
         * 这是调用 {@link callFactory} 的便捷方法。
         */
        public Builder client(OkMqttClient client) {
            OkMqttClient mqttClient = Objects.requireNonNull(client, "client == null");
            this.timeout = client.getDefaultTimeout();
            callFactory(mqttClient);
            observableFactory(mqttClient);
            return this;
        }

        /**
         * 指定用于创建 {@link Call} 实例的自定义调用工厂。
         * <p>
         * 注意：调用 {@link client} 会自动设置此值。
         */
        public Builder callFactory(CallFactory factory) {
            this.callFactory = Objects.requireNonNull(factory, "CallFactory == null");
            return this;
        }

        /**
         * 指定用于创建 {@link Observable} 实例的自定义调用工厂。
         * <p> 注意：调用 {@link client} 会自动设置此值。
         */
        public Builder observableFactory(ObservableFactory factory) {
            this.observableFactory = Objects.requireNonNull(factory, "ObservableFactory == null");
            return this;
        }

        /**
         * 设置 API 基础主题。
         */
        public Builder baseTopic(String baseTopic) {
            this.baseTopic = Objects.requireNonNull(baseTopic, "baseTopic == null");
            return this;
        }


        /**
         * 为对象的序列化和反序列化添加转换器工厂。
         */
        public Builder addConverterFactory(Converter.Factory factory) {
            converterFactories.add(Objects.requireNonNull(factory, "factory == null"));
            return this;
        }

        /**
         * 添加调用适配器工厂以支持 {@link Call} 以外的服务方法返回类型。
         */
        public Builder addCallAdapterFactory(CallAdapter.Factory factory) {
            callAdapterFactories.add(Objects.requireNonNull(factory, "factory == null"));
            return this;
        }

        /**
         * 从您的服务方法返回 {@link Call} 时调用 {@link Callback} 方法的执行程序，
         * 或者是{@link Observable} 时调用 {@link Consumer}、{@link Subscribe}、{@link FullConsumer}。
         * <p> 注意：{@code executor} 不用于 {@linkplain addCallAdapterFactory 自定义方法返回类型}。
         */
        public Builder callbackExecutor(Executor executor) {
            this.callbackExecutor = Objects.requireNonNull(executor, "executor == null");
            return this;
        }

        /**
         * 返回调用适配器工厂的可修改列表。
         */
        public List<CallAdapter.Factory> callAdapterFactories() {
            return this.callAdapterFactories;
        }

        /**
         * 返回可修改的转换器工厂列表。
         */
        public List<Converter.Factory> converterFactories() {
            return this.converterFactories;
        }

        /**
         * 在生成的 {@link Retrofit} 实例上调用 {@link create} 时，急切地验证所提供接口中所有方法的配置。
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
