package org.sheedon.mqtt.retrofit;


import org.sheedon.mqtt.Request;

/**
 * 对Retrofit方法的调用，该方法将请求发送到MQTT且可返回响应。
 * 每个调用都会产生自己的请求和响应对。
 * 这可用于实现轮询或重试失败的呼叫。(暂未使用)
 * 调用可以与{@link #publishNotCallback}无反馈执行，
 * 也可以与{@link * #enqueue}异步有反馈执行。
 * 无论哪种情况，都可以使用{@link #cancel}随时取消通话。
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

    void enqueue(Callback.Call<T> responseCallback);

    /**
     * Cancels the request, if possible. Requests that are already complete cannot be canceled.
     */
    void cancel();

    boolean isExecuted();

    boolean isCanceled();

    Call<T> clone();
}
