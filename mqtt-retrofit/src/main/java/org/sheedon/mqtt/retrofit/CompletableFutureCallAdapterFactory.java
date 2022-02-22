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

import android.annotation.TargetApi;

import androidx.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;

/**
 * CompletableFutureCall AdapterFactory
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/6 11:27 上午
 */
@TargetApi(24)
final class CompletableFutureCallAdapterFactory extends CallAdapter.Factory {
    @Override
    public @Nullable
    CallAdapter<?, ?> get(
            Type returnType, Annotation[] annotations, Retrofit retrofit) {
        Class<?> rawType = getRawType(returnType);
        if (rawType != CompletableFuture.class) {
            return null;
        }
        if (!(returnType instanceof ParameterizedType)) {
            throw new IllegalStateException(
                    "CompletableFuture return type must be parameterized"
                            + " as CompletableFuture<Foo> or CompletableFuture<? extends Foo>");
        }
        Type innerType = getParameterUpperBound(0, (ParameterizedType) returnType);

        if (getRawType(innerType) != Response.class) {
            // Generic type is not Response<T>. Use it for body-only adapter.
            return new BodyCallAdapter<>(rawType, innerType);
        }

        // Generic type is Response<T>. Extract T and create the Response version of the adapter.
        if (!(innerType instanceof ParameterizedType)) {
            throw new IllegalStateException(
                    "Response must be parameterized" + " as Response<Foo> or Response<? extends Foo>");
        }
        Type responseType = getParameterUpperBound(0, (ParameterizedType) innerType);
        return new ResponseCallAdapter<>(rawType, responseType);
    }

    private static final class BodyCallAdapter<R> implements CallAdapter<R, CompletableFuture<R>> {
        private final Class<?> rawType;
        private final Type responseType;

        BodyCallAdapter(Class<?> rawType, Type responseType) {
            this.rawType = rawType;
            this.responseType = responseType;
        }

        @Override
        public Type rawType() {
            return rawType;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public CompletableFuture<R> adapt(final Call<R> call) {
            CompletableFuture<R> future = new CallCancelCompletableFuture<>(call);
            call.enqueue(new BodyCallback(future));
            return future;
        }

        @Override
        public CompletableFuture<R> adapt(Observable<R> observable) {
            CompletableFuture<R> future = new CallCancelCompletableFuture<>(observable);
            observable.subscribe(new BodyCallback(future));
            return future;
        }

        private class BodyCallback implements Callback.Call<R>, Callback.Observable<R> {
            private final CompletableFuture<R> future;

            public BodyCallback(CompletableFuture<R> future) {
                this.future = future;
            }

            @Override
            public void onResponse(Call<R> call, Response<R> response) {
                if (response.isSuccessful()) {
                    future.complete(response.body());
                } else {
                    future.completeExceptionally(new MqttException(response));
                }
            }

            @Override
            public void onFailure(Call<R> call, Throwable t) {
                future.completeExceptionally(t);
            }

            @Override
            public void onResponse(Observable<R> call, Response<R> response) {
                if (response.isSuccessful()) {
                    future.complete(response.body());
                } else {
                    future.completeExceptionally(new MqttException(response));
                }
            }

            @Override
            public void onFailure(Observable<R> call, Throwable t) {
                future.completeExceptionally(t);
            }
        }
    }

    private static final class ResponseCallAdapter<R>
            implements CallAdapter<R, CompletableFuture<Response<R>>> {
        private final Class<?> rawType;
        private final Type responseType;

        ResponseCallAdapter(Class<?> rawType, Type responseType) {
            this.rawType = rawType;
            this.responseType = responseType;
        }

        @Override
        public Type rawType() {
            return rawType;
        }

        @Override
        public Type responseType() {
            return responseType;
        }

        @Override
        public CompletableFuture<Response<R>> adapt(final Call<R> call) {
            CompletableFuture<Response<R>> future = new CallCancelCompletableFuture<>(call);
            call.enqueue(new ResponseCallback(future));
            return future;
        }

        @Override
        public CompletableFuture<Response<R>> adapt(Observable<R> observable) {
            CompletableFuture<Response<R>> future = new CallCancelCompletableFuture<>(observable);
            observable.subscribe(new ResponseCallback(future));
            return future;
        }

        private class ResponseCallback implements Callback.Call<R>, Callback.Observable<R> {
            private final CompletableFuture<Response<R>> future;

            public ResponseCallback(CompletableFuture<Response<R>> future) {
                this.future = future;
            }

            @Override
            public void onResponse(Call<R> call, Response<R> response) {
                future.complete(response);
            }

            @Override
            public void onFailure(Call<R> call, Throwable t) {
                future.completeExceptionally(t);
            }

            @Override
            public void onResponse(Observable<R> call, Response<R> response) {
                future.complete(response);
            }

            @Override
            public void onFailure(Observable<R> call, Throwable t) {
                future.completeExceptionally(t);
            }
        }
    }

    private static final class CallCancelCompletableFuture<T> extends CompletableFuture<T> {
        private final Call<?> call;
        private final Observable<?> observable;

        CallCancelCompletableFuture(Call<?> call) {
            this.call = call;
            this.observable = null;
        }

        CallCancelCompletableFuture(Observable<?> observable) {
            this.observable = observable;
            this.call = null;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (mayInterruptIfRunning) {
                if (call != null)
                    call.cancel();
                if (observable != null) {
                    observable.cancel();
                }
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }
}
