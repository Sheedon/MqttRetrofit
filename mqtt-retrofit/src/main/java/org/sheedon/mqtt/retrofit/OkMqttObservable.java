/*
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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.sheedon.mqtt.Request;
import org.sheedon.mqtt.ResponseBody;

import java.io.IOException;

import static org.sheedon.mqtt.retrofit.Utils.throwIfFatal;

/**
 * mqtt观察者
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/4/14 14:20
 */
final class OkMqttObservable<T> implements Observable<T> {
    private final RequestFactory requestFactory;
    private final Object[] args;
    private final org.sheedon.mqtt.ObservableFactory observableFactory;
    private final Converter<ResponseBody, T> responseConverter;

    private volatile boolean canceled;

    @GuardedBy("this")
    private @Nullable
    org.sheedon.mqtt.Observable rawObservable;
    @GuardedBy("this") // Either a RuntimeException, non-fatal Error, or IOException.
    private @Nullable
    Throwable creationFailure;
    @GuardedBy("this")
    private boolean executed;

    OkMqttObservable(RequestFactory requestFactory,
                     @Nullable Object[] args,
                     org.sheedon.mqtt.ObservableFactory observableFactory,
                     Converter<ResponseBody, T> responseConverter) {
        this.requestFactory = requestFactory;
        this.args = args;
        this.observableFactory = observableFactory;
        this.responseConverter = responseConverter;
    }

    @Override
    public synchronized Request request() {
        try {
            return getRawObservable().request();
        } catch (RuntimeException | Error e) {
            throwIfFatal(e); // Do not assign a fatal error to creationFailure.
            creationFailure = e;
            throw e;
        } catch (IOException e) {
            creationFailure = e;
            throw new RuntimeException("Unable to create request.", e);
        }
    }

    /**
     * Returns the raw observable, initializing it if necessary. Throws if initializing the raw call throws,
     * or has thrown in previous attempts to create it.
     */
    @GuardedBy("this")
    private org.sheedon.mqtt.Observable getRawObservable() throws IOException {
        org.sheedon.mqtt.Observable observable = rawObservable;
        if (observable != null) return observable;

        if (creationFailure != null) {
            if (creationFailure instanceof IOException) {
                throw new RuntimeException("Unable to create request.", creationFailure);
            } else if (creationFailure instanceof RuntimeException) {
                throw (RuntimeException) creationFailure;
            } else {
                throw (Error) creationFailure;
            }
        }
        try {
            return (rawObservable = createRawObservable()).request();
        } catch (RuntimeException | Error | IOException e) {
            throwIfFatal(e); // Do not assign a fatal error to creationFailure.
            creationFailure = e;
            throw e;
        }
    }

    @Override
    public void subscribe(final Callback.Observable<T> callback) {
        org.sheedon.mqtt.Observable observable;
        Throwable failure;

        synchronized (this) {

            observable = rawObservable;
            failure = creationFailure;
            if (observable == null && failure == null) {
                try {
                    observable = rawObservable = createRawObservable();
                } catch (Throwable t) {
                    failure = creationFailure = t;
                }
            }
        }

        if (failure != null) {
            dealWithCallback(callback, OkMqttObservable.this, null, failure, false);
            return;
        }

        if (canceled) {
            observable.cancel();
        }

        if (callback == null)
            return;

        observable.subscribe(new org.sheedon.mqtt.Callback() {
            @Override
            public void onResponse(@NonNull Request request, @NonNull org.sheedon.mqtt.Response rawResponse) {
                Response<T> response;
                try {
                    response = parseResponse(rawResponse);
                } catch (Throwable e) {
                    callFailure(e);
                    return;
                }
                callSuccess(response);
            }

            @Override
            public void onFailure(Throwable e) {
                try {
                    dealWithCallback(callback, OkMqttObservable.this, null, e, false);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            private void callFailure(Throwable e) {
                try {
                    dealWithCallback(callback, OkMqttObservable.this, null, e, false);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            private void callSuccess(Response<T> response) {
                try {
                    dealWithCallback(callback, OkMqttObservable.this, response, null, true);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        });
    }

    /**
     * 处理反馈
     *
     * @param callback   反馈监听
     * @param observable Observable
     * @param response   反馈内容
     * @param t          错误
     * @param isSuccess  是否成功
     */
    private void dealWithCallback(Callback.Observable<T> callback, Observable<T> observable, Response<T> response, Throwable t, boolean isSuccess) {

        if (callback == null)
            return;

        try {
            if (isSuccess) {
                callback.onResponse(observable, response);
            } else {
                callback.onFailure(observable, t);
            }
        } catch (Throwable throwable) {
            throwIfFatal(throwable);
            t.printStackTrace(); // TODO this is not great
        }

    }

    private org.sheedon.mqtt.Observable createRawObservable() throws IOException {
        org.sheedon.mqtt.Observable observable = observableFactory.newObservable(requestFactory.create(args));
        if (observable == null) {
            throw new NullPointerException("Observable.SerialFactory returned null.");
        }
        return observable;
    }

    Response<T> parseResponse(org.sheedon.mqtt.Response rawResponse) throws IOException {
        ResponseBody rawBody = rawResponse.body();

        try {
            T body = responseConverter.convert(rawBody);
            return Response.success(body, rawResponse);
        } catch (RuntimeException e) {
            // If the underlying source threw an exception, propagate that rather than indicating it was
            // a runtime exception.
            throw e;
        }
    }

    @Override
    public boolean isExecuted() {
        return executed;
    }

    @Override
    public void cancel() {
        canceled = true;

        org.sheedon.mqtt.Observable observable;
        synchronized (this) {
            observable = rawObservable;
        }
        if (observable != null) {
            observable.cancel();
        }
    }

    @Override
    public boolean isCanceled() {
        if (canceled) {
            return true;
        }
        synchronized (this) {
            return rawObservable != null && rawObservable.isCanceled();
        }
    }
}
