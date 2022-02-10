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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.sheedon.mqtt.Request;
import org.sheedon.mqtt.ResponseBody;

import java.io.IOException;

import static org.sheedon.mqtt.retrofit.Utils.throwIfFatal;

/**
 * okmqtt调度
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 22:27
 */
final class OkMqttCall<T> implements Call<T> {
    private final RequestFactory requestFactory;
    private final Object[] args;
    private final org.sheedon.mqtt.Call.Factory callFactory;
    private final Converter<ResponseBody, T> responseConverter;

    private volatile boolean canceled;

    @GuardedBy("this")
    private @Nullable
    org.sheedon.mqtt.Call rawCall;
    @GuardedBy("this") // Either a RuntimeException, non-fatal Error, or IOException.
    private @Nullable
    Throwable creationFailure;
    @GuardedBy("this")
    private boolean executed;

    OkMqttCall(RequestFactory requestFactory,
               @Nullable Object[] args,
               org.sheedon.mqtt.Call.Factory callFactory,
               Converter<ResponseBody, T> responseConverter) {
        this.requestFactory = requestFactory;
        this.args = args;
        this.callFactory = callFactory;
        this.responseConverter = responseConverter;
    }

    @Override
    public synchronized Request request() {
        try {
            return getRawCall().request();
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
     * Returns the raw call, initializing it if necessary. Throws if initializing the raw call throws,
     * or has thrown in previous attempts to create it.
     */
    @GuardedBy("this")
    private org.sheedon.mqtt.Call getRawCall() throws IOException{
        org.sheedon.mqtt.Call call = rawCall;
        if (call != null) return call;

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
            return rawCall = createRawCall();
        } catch (RuntimeException | Error | IOException e) {
            throwIfFatal(e); // Do not assign a fatal error to creationFailure.
            creationFailure = e;
            throw e;
        }
    }

    @Override
    public void publish() {
        enqueue(null);
    }

    @Override
    public void enqueue(final Callback.Call<T> callback) {

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
            call.publish();
        else
            call.enqueue(new org.sheedon.mqtt.Callback() {
                @Override
                public void onResponse(@NonNull Request request,
                                       @NonNull org.sheedon.mqtt.Response rawResponse) {
                    Response<T> response;
                    try {
                        response = parseResponse(rawResponse);
                    } catch (Throwable e) {
                        throwIfFatal(e);
                        callFailure(e);
                        return;
                    }

                    callSuccess(response);
                }

                @Override
                public void onFailure(Throwable e) {
                    dealWithCallback(callback, OkMqttCall.this, null, e, false);
                }

                private void callFailure(Throwable e) {
                    dealWithCallback(callback, OkMqttCall.this, null, e, false);
                }

                private void callSuccess(Response<T> response) {
                    dealWithCallback(callback, OkMqttCall.this, response, null, true);
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
    private void dealWithCallback(Callback.Call<T> callback, Call<T> call, Response<T> response, Throwable t, boolean isSuccess) {

        if (callback == null)
            return;

        try {
            if (isSuccess) {
                callback.onResponse(call, response);
            } else {
                callback.onFailure(call, t);
            }
        } catch (Throwable throwable) {
            throwIfFatal(throwable);
            t.printStackTrace(); // TODO this is not great
        }

    }

    private org.sheedon.mqtt.Call createRawCall() throws IOException {
        org.sheedon.mqtt.Call call = callFactory.newCall(requestFactory.create(args));
        if (call == null) {
            throw new NullPointerException("MqttFactory returned null.");
        }
        return call;
    }

    private Response<T> parseResponse(org.sheedon.mqtt.Response rawResponse) throws IOException {
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
