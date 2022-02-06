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

import org.sheedon.mqtt.Request;

/**
 * 对Retrofit方法的调用，该方法将请求发送到串口并返回响应。
 * 每个调用都会产生自己的请求和响应对。
 * 这可用于实现轮询或重试失败的呼叫。(暂未使用)
 * 调用可以与{@link #subscribe}添加订阅，
 * 无论哪种情况，都可以使用{@link #cancel}随时取消订阅。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/4/14 12:58
 */
public interface Observable<T> extends Cloneable {
    /**
     * Returns the original request that initiated this call.
     */
    Request request();

    void subscribe(Callback.Observable<T> callback);

    /**
     * Cancels the request, if possible. Requests that are already complete cannot be canceled.
     */
    void cancel();


    boolean isExecuted();

    boolean isCanceled();

    Observable<T> clone();
}
