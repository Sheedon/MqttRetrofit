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

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.sheedon.mqtt.Request;
import org.sheedon.mqtt.ResponseBody;

import java.io.IOException;

import static org.sheedon.mqtt.retrofit.Utils.throwIfFatal;

/**
 * Call在OkMqtt中的实现类，用于代理创建请求调度。
 * <p>
 * 通过{@link callFactory} 将请求数据存储工厂{@link requestFactory}配置对应请求参数{@link args}，以获取真实Call，
 * 对于Call代理执行无响应请求{@link #publish()}或者「订阅响应的请求」{@link #enqueue(Callback)}。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 22:27
 */
final class OkMqttCall<T> implements Call<T> {
    private final RequestFactory requestFactory;
    private final Object[] args;
    private final org.sheedon.mqtt.CallFactory callFactory;
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
               org.sheedon.mqtt.CallFactory callFactory,
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
     * 返回原始调用，必要时对其进行初始化。如果初始化原始调用抛出，或者在之前创建它的尝试中已经抛出，则抛出。
     */
    @GuardedBy("this")
    private org.sheedon.mqtt.Call getRawCall() throws IOException {
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

    /**
     * 发送请求，表示该请求不需要监听反馈
     */
    @Override
    public void publish() {
        enqueue(null);
    }

    /**
     * 将请求消息入队，若监听{@link callback} 不为空，则借助callback反馈响应结果。
     * 根据mqtt-server的响应反馈结果，或者网络连接、mqtt断开连接等情况下执行错误回调。
     * 若callback为空，则表示该请求不需要监听反馈。
     * <p>
     * 一个请求对象只能被请求调度一次，若调度执行超过一次则抛出IllegalStateException异常。
     *
     * @param callback Callback with request
     */
    @Override
    public void enqueue(@Nullable Callback<T> callback) {
        org.sheedon.mqtt.Call call;
        Throwable failure;

        synchronized (this) {
            if (executed) throw new IllegalStateException("Already executed.");
            executed = true;

            // 同步得到call/failure，若call和failure为空，则执行创建原始调用
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

        // failure不为空则说明已产生错误，执行反馈动作，不再执行后续请求动作
        if (failure != null) {
            dealWithCallback(callback, OkMqttCall.this, null, failure, false);
            return;
        }

        // 若状态为取消，则调度取消动作，不在执行后续请求动作
        if (canceled) {
            call.cancel();
            return;
        }

        // 无响应请求
        if (callback == null) {
            call.publish();
        } else {
            // 请求入队
            call.enqueue(new org.sheedon.mqtt.Callback() {
                @Override
                public void onResponse(@NonNull org.sheedon.mqtt.Call call, @NonNull org.sheedon.mqtt.Response rawResponse) {
                    Response<T> response;
                    try {
                        response = parseResponse(rawResponse);
                        callSuccess(response);
                    } catch (Throwable e) {
                        throwIfFatal(e);
                        callFailure(e);
                    }
                }

                @Override
                public void onFailure(@Nullable Throwable e) {
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
    }

    /**
     * 处理反馈结果。
     * 根据isSuccess得知该请求是否请求成功。
     * 由callback将call和响应结果response发送给请求执行者。
     *
     * @param callback  反馈监听
     * @param call      Call
     * @param response  反馈内容
     * @param t         错误
     * @param isSuccess 是否成功
     */
    private void dealWithCallback(Callback<T> callback, Call<T> call, Response<T> response, Throwable t, boolean isSuccess) {

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

    /**
     * 代理创建原始调用
     *
     * @return org.sheedon.mqtt.Call okmqtt中的Call
     * @throws IOException
     */
    private org.sheedon.mqtt.Call createRawCall() throws IOException {
        org.sheedon.mqtt.Call call = callFactory.newCall(requestFactory.create(args));
        if (call == null) {
            throw new NullPointerException("MqttFactory returned null.");
        }
        return call;
    }

    /**
     * 将原始响应结果转化为封装后的Response
     *
     * @param rawResponse 原始响应结果
     * @return 封装响应结果
     * @throws IOException
     */
    private Response<T> parseResponse(org.sheedon.mqtt.Response rawResponse) throws IOException {
        ResponseBody rawBody = rawResponse.getBody();

        try {
            T body = responseConverter.convert(rawBody);
            return Response.success(body, rawResponse);
        } catch (RuntimeException e) {
            // If the underlying source threw an exception, propagate that rather than indicating it was
            // a runtime exception.
            throw e;
        }
    }

    /**
     * 是否已被执行
     */
    @Override
    public boolean isExecuted() {
        return executed;
    }

    /**
     * 取消请求
     */
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

    /**
     * 是否取消请求
     */
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
