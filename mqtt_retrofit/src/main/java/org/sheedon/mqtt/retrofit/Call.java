package org.sheedon.mqtt.retrofit;


import org.sheedon.mqtt.Request;

/**
 * 转化器Call
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/22 17:26
 */
public interface Call<T> extends Cloneable {
    /**
     * Returns the original request that initiated this call.
     */
    Request request();

    void publishNotCallback();

    void enqueue(Callback<T> responseCallback);

    /**
     * Cancels the request, if possible. Requests that are already complete cannot be canceled.
     */
    void cancel();

    boolean isExecuted();

    boolean isCanceled();

    Call<T> clone();
}
