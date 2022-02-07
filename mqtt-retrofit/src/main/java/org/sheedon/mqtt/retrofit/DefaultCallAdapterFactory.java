/*
 * Copyright (C) 2020 Sheedon.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sheedon.mqtt.retrofit;

import androidx.annotation.Nullable;

import org.sheedon.mqtt.Request;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

/**
 * 默认Call/Observable适配器工厂
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

        final Type responseType = Utils.getCallResponseType(returnType);
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
                return new ExecutorCallAdapterFactory.ExecutorCallbackCall<>(callbackExecutor, call);
            }

            @Override
            public Observable<Object> adapt(Observable<Object> observable) {
                return new ExecutorCallAdapterFactory.ExecutorCallbackObservable<>(callbackExecutor, observable);
            }
        };
    }


    static final class ExecutorCallbackCall<T> implements Call<T> {
        final Executor callbackExecutor;
        final Call<T> delegate;

        ExecutorCallbackCall(Executor callbackExecutor, Call<T> delegate) {
            this.callbackExecutor = callbackExecutor;
            this.delegate = delegate;
        }

        @Override
        public boolean isExecuted() {
            return delegate.isExecuted();
        }

        @Override
        public void cancel() {
            delegate.cancel();
        }

        @Override
        public boolean isCanceled() {
            return delegate.isCanceled();
        }

        @SuppressWarnings("CloneDoesntCallSuperClone") // Performing deep clone.
        @Override
        public Call<T> clone() {
            return new ExecutorCallAdapterFactory.ExecutorCallbackCall<>(callbackExecutor, delegate.clone());
        }

        @Override
        public Request request() {
            return delegate.request();
        }

        @Override
        public void publishNotCallback() {
            enqueue(null);
        }

        @Override
        public void enqueue(final Callback.Call<T> callback) {
            if (callback == null) {
                delegate.publishNotCallback();
                return;
            }

            delegate.enqueue(new Callback.Call<T>() {
                @Override
                public void onResponse(Call<T> call, final Response<T> response) {
                    callbackExecutor.execute(() -> {
                        if (delegate.isCanceled()) {
                            // Emulate OkHttp's behavior of throwing/delivering an IOException on cancellation.
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

    static final class ExecutorCallbackObservable<T> implements Observable<T> {
        final Executor callbackExecutor;
        final Observable<T> delegate;

        ExecutorCallbackObservable(Executor callbackExecutor, Observable<T> delegate) {
            this.callbackExecutor = callbackExecutor;
            this.delegate = delegate;
        }

        @Override
        public boolean isExecuted() {
            return delegate.isExecuted();
        }

        @Override
        public void cancel() {
            delegate.cancel();
        }

        @Override
        public boolean isCanceled() {
            return delegate.isCanceled();
        }

        @SuppressWarnings("CloneDoesntCallSuperClone") // Performing deep clone.
        @Override
        public Observable<T> clone() {
            return new ExecutorCallAdapterFactory.ExecutorCallbackObservable<>(callbackExecutor, delegate.clone());
        }

        @Override
        public Request request() {
            return delegate.request();
        }

        @Override
        public void subscribe(final Callback.Observable<T> callback) {
            if (callback == null) {
                return;
            }

            delegate.subscribe(new Callback.Observable<T>() {

                @Override
                public void onResponse(Observable<T> call, final Response<T> response) {
                    callbackExecutor.execute(() -> {
                        if (delegate.isCanceled()) {
                            // Emulate OkHttp's behavior of throwing/delivering an IOException on cancellation.
                            callback.onFailure(ExecutorCallbackObservable.this, new IOException("Canceled"));
                        } else {
                            callback.onResponse(ExecutorCallbackObservable.this, response);
                        }
                    });
                }

                @Override
                public void onFailure(Observable<T> call, final Throwable t) {
                    callbackExecutor.execute(() -> callback.onFailure(ExecutorCallbackObservable.this, t));
                }

            });
        }
    }
}