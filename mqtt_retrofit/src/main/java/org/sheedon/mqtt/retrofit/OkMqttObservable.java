/**
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
import androidx.annotation.Nullable;

import org.sheedon.mqtt.Request;
import org.sheedon.mqtt.ResponseBody;

import java.io.IOException;

import static org.sheedon.mqtt.retrofit.Utils.throwIfFatal;


/**
 * 串口观察者
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/4/14 14:20
 */
final class OkMqttObservable<T> implements Observable<T> {
    private final ServiceMethod<T, ?> serviceMethod;
    private final @Nullable
    Object[] args;

    private volatile boolean canceled;

    @GuardedBy("this")
    private @Nullable
    org.sheedon.mqtt.Observable rawObservable;
    @GuardedBy("this") // Either a RuntimeException, non-fatal Error, or IOException.
    private @Nullable
    Throwable creationFailure;
    @GuardedBy("this")
    private boolean executed;

    OkMqttObservable(ServiceMethod<T, ?> serviceMethod, @Nullable Object[] args) {
        this.serviceMethod = serviceMethod;
        this.args = args;
    }

    @Override
    public Observable<T> clone() {
        return new OkMqttObservable<>(serviceMethod, args);
    }

    @Override
    public synchronized Request request() {
        org.sheedon.mqtt.Observable observable = rawObservable;
        if (observable != null) {
            return observable.request();
        }
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
        } catch (RuntimeException | Error e) {
            throwIfFatal(e); // Do not assign a fatal error to creationFailure.
            creationFailure = e;
            throw e;
        } catch (IOException e) {
            creationFailure = e;
            throw new RuntimeException("Unable to create request.", e);
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

        observable.subscribe(new org.sheedon.mqtt.Callback<org.sheedon.mqtt.Response>() {
            @Override
            public void onFailure(Throwable e) {
                try {
                    dealWithCallback(callback, OkMqttObservable.this, null, e, false);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            @Override
            public void onResponse(org.sheedon.mqtt.Response rawResponse) {
                Response<T> response;
                try {
                    response = parseResponse(rawResponse);
                } catch (Throwable e) {
                    callFailure(e);
                    return;
                }
                callSuccess(response);
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
    private void dealWithCallback(Callback.Observable callback, Observable<T> observable, Response<T> response, Throwable t, boolean isSuccess) {

        if (callback == null)
            return;

        if (isSuccess) {
            callback.onResponse(observable, response);
        } else {
            callback.onFailure(observable, t);
        }

    }

    private org.sheedon.mqtt.Observable createRawObservable() throws IOException {
        Request request = serviceMethod.toRequest(args);
        org.sheedon.mqtt.Observable observable = serviceMethod.mqttFactory.newObservable(request);
        if (observable == null) {
            throw new NullPointerException("Observable.SerialFactory returned null.");
        }
        return observable;
    }

    Response<T> parseResponse(org.sheedon.mqtt.Response rawResponse) throws IOException {
        ResponseBody rawBody = rawResponse.body();

        try {
            T body = serviceMethod.toResponse(rawBody);
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
