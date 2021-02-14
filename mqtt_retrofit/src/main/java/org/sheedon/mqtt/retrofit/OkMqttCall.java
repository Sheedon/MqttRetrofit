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
 * okmqtt调度
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 22:27
 */
final class OkMqttCall<T> implements Call<T> {
    private final ServiceMethod<T, ?> serviceMethod;
    private final @Nullable
    Object[] args;

    private volatile boolean canceled;

    @GuardedBy("this")
    private @Nullable
    org.sheedon.mqtt.Call rawCall;
    @GuardedBy("this") // Either a RuntimeException, non-fatal Error, or IOException.
    private @Nullable
    Throwable creationFailure;
    @GuardedBy("this")
    private boolean executed;

    OkMqttCall(ServiceMethod<T, ?> serviceMethod, @Nullable Object[] args) {
        this.serviceMethod = serviceMethod;
        this.args = args;
    }

    @Override
    public Call<T> clone() {
        return new OkMqttCall<>(serviceMethod, args);
    }

    @Override
    public synchronized Request request() {
        org.sheedon.mqtt.Call call = rawCall;
        if (call != null) {
            return call.request();
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
            return (rawCall = createRawCall()).request();
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
    public void publishNotCallback() {
        enqueue(null);
    }

    @Override
    public void enqueue(final Callback.Call callback) {

        org.sheedon.mqtt.Call call;
        Throwable failure;

        synchronized (this) {
            if (executed) throw new IllegalStateException("Already executed.");
            executed = true;

            call = rawCall;
            failure = creationFailure;
            if (call == null && failure == null) {
                try {
                    call = rawCall = createRawCall();
                } catch (Throwable t) {
                    failure = creationFailure = t;
                }
            }
        }

        if (failure != null) {
            dealWithCallback(callback, OkMqttCall.this, null, failure, false);
            return;
        }

        if (canceled) {
            call.cancel();
        }

        if (callback == null)
            call.publishNotCallback();
        else
            call.enqueue(new org.sheedon.mqtt.Callback<org.sheedon.mqtt.Response>() {
                @Override
                public void onFailure(Throwable e) {
                    try {
                        dealWithCallback(callback, OkMqttCall.this, null, e, false);
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
                        dealWithCallback(callback, OkMqttCall.this, null, e, false);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }

                private void callSuccess(Response<T> response) {
                    try {
                        dealWithCallback(callback, OkMqttCall.this, response, null, true);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
    }

    /**
     * 处理反馈
     *
     * @param callback  反馈监听
     * @param call      Call
     * @param response  反馈内容
     * @param t         错误
     * @param isSuccess 是否成功
     */
    private void dealWithCallback(Callback.Call callback, Call<T> call, Response<T> response, Throwable t, boolean isSuccess) {

        if (callback == null)
            return;

        if (isSuccess) {
            callback.onResponse(call, response);
        } else {
            callback.onFailure(call, t);
        }

    }

    private org.sheedon.mqtt.Call createRawCall() throws IOException {
        Request request = serviceMethod.toRequest(args);
        org.sheedon.mqtt.Call call = serviceMethod.mqttFactory.newCall(request);
        if (call == null) {
            throw new NullPointerException("MqttFactory returned null.");
        }
        return call;
    }

    private Response<T> parseResponse(org.sheedon.mqtt.Response rawResponse) throws IOException {
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

        org.sheedon.mqtt.Call call;
        synchronized (this) {
            call = rawCall;
        }
        if (call != null) {
            call.cancel();
        }
    }

    @Override
    public boolean isCanceled() {
        if (canceled) {
            return true;
        }
        synchronized (this) {
            return rawCall != null && rawCall.isCanceled();
        }
    }
}
