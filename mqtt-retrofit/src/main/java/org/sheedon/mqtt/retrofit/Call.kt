package org.sheedon.mqtt.retrofit

import org.sheedon.mqtt.Request

/**
 * 对Retrofit方法的调用，该方法将请求发送到MQTT且可返回响应。
 * 每个调用都会产生自己的请求和响应对。
 * 这可用于实现轮询或重试失败的呼叫。(暂未使用)
 * 调用可以与{@link #publish()}无反馈执行，
 * 也可以与{@link * #enqueue}异步有反馈执行。
 * 无论哪种情况，都可以使用{@link #cancel}随时取消通话。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/4 15:58
 */
interface Call<T> {

    /**
     * Returns the original request that initiated this call.
     */
    fun request(): Request

    fun publish()

    fun enqueue(callback: Callback<T>?)

    /**
     * Cancels the request, if possible. Requests that are already complete cannot be canceled.
     */
    fun cancel()

    fun isExecuted(): Boolean

    fun isCanceled(): Boolean
}

/**
 * 对Retrofit方法的调用，该方法将请求发送到串口并返回响应。
 * 每个调用都会产生自己的请求和响应对。
 * 这可用于实现轮询或重试失败的呼叫。(暂未使用)
 * 调用可以与{@link #subscribe}添加订阅，
 * 无论哪种情况，都可以使用{@link #cancel}随时取消订阅。
 * */
interface Observable<T> {
    /**
     * Returns the original request that initiated this call.
     */
    fun request(): Request
    fun subscribe(): org.sheedon.mqtt.Subscribe

    fun enqueue()
    fun enqueue(consumer: Consumer<T>)
    fun enqueue(consumer: Subscribe<T>)
    fun enqueue(consumer: FullConsumer<T>)


    fun unsubscribe(callback: Subscribe<T>?)

    /**
     * Cancels the request, if possible. Requests that are already complete cannot be canceled.
     */
    fun cancel()

    fun isExecuted(): Boolean

    fun isCanceled(): Boolean
}