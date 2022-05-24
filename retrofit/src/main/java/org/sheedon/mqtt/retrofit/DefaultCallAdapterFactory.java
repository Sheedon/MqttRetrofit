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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe;
import org.sheedon.mqtt.Request;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * 默认Call/Observable适配器工厂，根据服务接口方法的返回类型创建CallAdapter实例
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/22 17:31
 */
final class DefaultCallAdapterFactory extends CallAdapter.Factory {
    private final @Nullable
    Executor callbackExecutor;

    public DefaultCallAdapterFactory(@Nullable Executor callbackExecutor) {
        this.callbackExecutor = callbackExecutor;
    }

    /**
     * 根据配置的返回值类型，创建对应的CallAdapter
     *
     * @param returnType  返回值类型
     * @param annotations 方法的注解
     * @param retrofit    Retrofit
     */
    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        final Class<?> rawType = getRawType(returnType);
        if (rawType != Call.class && rawType != Observable.class) {
            return null;
        }
        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalArgumentException(
                    "Call return type must be parameterized as Call<Foo> or Call<? extends Foo>," +
                            "and Observable return type must be parameterized as Observable<Foo> or Observable<? extends Foo>");
        }

        final Type responseType = Utils.getParameterUpperBound(0, (ParameterizedType) returnType);

        final Executor executor =
                Utils.isAnnotationPresent(annotations, SkipCallbackExecutor.class)
                        ? null
                        : callbackExecutor;

        return new CallAdapter<Object, Object>() {
            @Override
            public Type rawType() {
                return rawType;
            }

            @Override
            public Type responseType() {
                return responseType;
            }

            @Override
            public Call<Object> adapt(Call<Object> call) {
                return executor == null ? call : new ExecutorCallbackCall<>(executor, call);
            }

            @Override
            public Observable<Object> adapt(Observable<Object> observable) {
                return executor == null ? observable :
                        new ExecutorCallbackObservable<>(callbackExecutor, observable);
            }
        };
    }

    /**
     * 默认的Call的反馈调度实现类。
     * 代理执行okMqtt-Request publish/enqueue
     */
    static final class ExecutorCallbackCall<T> implements Call<T> {
        final Executor callbackExecutor;
        final Call<T> delegate;

        ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
            this.callbackExecutor = callbackExecutor;
            this.delegate = delegate;
        }

        /**
         * 如果此调用已被 {@linkplain publish() published} 或 {@linkplain enqueue(Callback) enqueued}，
         * 则返回 true。多次发布或排队调用是错误的。
         */
        @Override
        public boolean isExecuted() {
            return delegate.isExecuted();
        }

        /**
         * 取消此通话。将尝试取消进行中的呼叫，如果呼叫尚未执行，则永远不会执行。
         */
        @Override
        public void cancel() {
            delegate.cancel();
        }

        /**
         * 如果调用了 {@link cancel()}，则为真。
         */
        @Override
        public boolean isCanceled() {
            return delegate.isCanceled();
        }

        /**
         * 返回发起此调用的原始请求。
         */
        @Override
        public Request request() {
            return delegate.request();
        }

        /**
         * 异步发送请求，无论请求是否发送成功。
         */
        @Override
        public void publish() {
            enqueue(null);
        }

        /**
         * 异步发送请求并通知 {@code callback} 其响应，或者如果与服务器交谈、创建请求或处理响应发生错误。
         */
        @Override
        public void enqueue(final Callback<T> callback) {
            if (callback == null) {
                delegate.publish();
                return;
            }

            delegate.enqueue(
                    new Callback<T>() {
                        @Override
                        public void onResponse(Call<T> call, final Response<T> response) {
                            callbackExecutor.execute(() -> {
                                if (delegate.isCanceled()) {
                                    // Emulate OkMqtt's behavior of throwing/delivering an IOException on cancellation.
                                    callback.onFailure(ExecutorCallbackCall.this, new IOException("Canceled"));
                                } else {
                                    callback.onResponse(ExecutorCallbackCall.this, response);
                                }
                            });
                        }

                        @Override
                        public void onFailure(Call<T> call, final Throwable t) {
                            callbackExecutor.execute(() -> callback.onFailure(ExecutorCallbackCall.this, t));
                        }
                    });
        }
    }

    /**
     * 默认的Observable的反馈调度实现类。
     * 代理执行okMqtt-Request/Subscribe 执行任务入队操作
     */
    static final class ExecutorCallbackObservable<T> implements Observable<T> {
        final Executor callbackExecutor;
        final Observable<T> delegate;

        ExecutorCallbackObservable(Executor callbackExecutor, Observable<T> delegate) {
            this.callbackExecutor = callbackExecutor;
            this.delegate = delegate;
        }

        /**
         * 如果此调用已被 {@linkplain publish() published} 或 {@linkplain enqueue(Callback) enqueued}，
         * 则返回 true。多次发布或排队调用是错误的。
         */
        @Override
        public boolean isExecuted() {
            return delegate.isExecuted();
        }

        /**
         * 取消此通话。将尝试取消进行中的呼叫，如果呼叫尚未执行，则永远不会执行。
         */
        @Override
        public void cancel() {
            delegate.cancel();
        }

        /**
         * 如果调用了 {@link cancel()}，则为真。
         */
        @Override
        public boolean isCanceled() {
            return delegate.isCanceled();
        }

        /**
         * 返回发起此调用的原始请求。
         */
        @Override
        public Request request() {
            return delegate.request();
        }

        /**
         * 返回发起此调用的原始订阅。
         */
        @NonNull
        @Override
        public org.sheedon.mqtt.Subscribe subscribe() {
            return delegate.subscribe();
        }

        /**
         * 异步发送订阅，无论订阅是否发送成功。
         */
        @Override
        public void enqueue() {
            delegate.enqueue();
        }

        /**
         * 异步发送订阅并通知 {@code consumer} 其响应，如果发布和订阅 mqtt-server，创建订阅者请求或处理响应产生
         * 错误时回调错误响应方法。
         */
        @Override
        public void enqueue(final Consumer<T> consumer) {
            Objects.requireNonNull(consumer, "consumer == null");

            delegate.enqueue(new Consumer<T>() {
                @Override
                public void onResponse(@NonNull Observable<T> observable, @Nullable Response<T> response) {
                    callbackExecutor.execute(() -> {
                        if (delegate.isCanceled()) {
                            // Emulate OkMqtt's behavior of throwing/delivering an IOException on cancellation.
                            consumer.onFailure(ExecutorCallbackObservable.this, new IOException("Canceled"));
                        } else {
                            consumer.onResponse(ExecutorCallbackObservable.this, response);
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Observable<T> observable, @Nullable Throwable t) {
                    callbackExecutor.execute(() -> consumer.onFailure(ExecutorCallbackObservable.this, t));
                }
            });
        }

        /**
         * 异步发送订阅并通知 {@code Subscribe} 其响应，或者如果发生错误订阅 mqtt-server、创建订阅者请求或处理响应
         * 产生错误时回调错误响应方法。
         */
        @Override
        public void enqueue(@NonNull Subscribe<T> subscribe) {
            Objects.requireNonNull(subscribe, "subscribe == null");
            delegate.enqueue(new Subscribe<T>() {
                @Override
                public void onResponse(@NonNull Observable<T> observable, @Nullable MqttSubscribe response) {
                    callbackExecutor.execute(() -> {
                        if (delegate.isCanceled()) {
                            // Emulate OkMqtt's behavior of throwing/delivering an IOException on cancellation.
                            subscribe.onFailure(ExecutorCallbackObservable.this, new IOException("Canceled"));
                        } else {
                            subscribe.onResponse(ExecutorCallbackObservable.this, response);
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Observable<T> observable, @Nullable Throwable t) {
                    callbackExecutor.execute(() -> subscribe.onFailure(ExecutorCallbackObservable.this, t));
                }
            });
        }

        /**
         * 异步发送订阅并通知 {@code consumer} 其响应，如果订阅 mqtt-server 或 loacl-server有误，则将错误消息
         * 返回到{@link #onFailure}中。
         */
        @Override
        public void enqueue(@NonNull FullConsumer<T> consumer) {
            Objects.requireNonNull(consumer, "consumer == null");

            delegate.enqueue(new FullConsumer<T>() {
                @Override
                public void onResponse(@NonNull Observable<T> observable, @Nullable Response<T> response) {
                    callbackExecutor.execute(() -> {
                        if (delegate.isCanceled()) {
                            // Emulate OkMqtt's behavior of throwing/delivering an IOException on cancellation.
                            consumer.onFailure(ExecutorCallbackObservable.this, new IOException("Canceled"));
                        } else {
                            consumer.onResponse(ExecutorCallbackObservable.this, response);
                        }
                    });
                }

                @Override
                public void onResponse(@NonNull Observable<T> observable, @Nullable MqttSubscribe response) {
                    callbackExecutor.execute(() -> {
                        if (delegate.isCanceled()) {
                            // Emulate OkMqtt's behavior of throwing/delivering an IOException on cancellation.
                            consumer.onFailure(ExecutorCallbackObservable.this, new IOException("Canceled"));
                        } else {
                            consumer.onResponse(ExecutorCallbackObservable.this, response);
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Observable<T> observable, @Nullable Throwable t) {
                    callbackExecutor.execute(() -> consumer.onFailure(ExecutorCallbackObservable.this, t));
                }
            });
        }

        /**
         * 异步发送取消订阅并通知 {@code callback} 其响应，如果取消订阅 mqtt-server 或 loacl-server有误，则将错误消息
         *          * 返回到{@link #onFailure}中。
         */
        @Override
        public void unsubscribe(Subscribe<T> callback) {
            delegate.unsubscribe(callback == null ? null : new Subscribe<T>() {
                @Override
                public void onResponse(@NonNull Observable<T> observable, @Nullable MqttSubscribe response) {
                    if (callback == null) {
                        return;
                    }

                    callbackExecutor.execute(() -> {
                        if (delegate.isCanceled()) {
                            // Emulate OkMqtt's behavior of throwing/delivering an IOException on cancellation.
                            callback.onFailure(ExecutorCallbackObservable.this, new IOException("Canceled"));
                        } else {
                            callback.onResponse(ExecutorCallbackObservable.this, response);
                        }
                    });
                }

                @Override
                public void onFailure(@NonNull Observable<T> observable, @Nullable Throwable t) {
                    if (callback == null) {
                        return;
                    }

                    callbackExecutor.execute(() -> callback.onFailure(ExecutorCallbackObservable.this, t));
                }
            });
        }
    }
}