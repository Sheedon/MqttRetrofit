package org.sheedon.mqtt.retrofit

import org.sheedon.mqtt.Request

/**
 * 对Retrofit方法的调用，该方法将请求发送到MQTT且可返回响应。
 * 每个调用都会产生自己的请求和响应对。
 * 这可用于实现轮询或重试失败的呼叫。(暂未使用)
 * 调用可以与{@link #publish()}无反馈执行，
 * 也可以与{@link #enqueue}异步有反馈执行。
 * 无论哪种情况，都可以使用{@link #cancel}随时取消通话。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/4 15:58
 */
interface Call<T> {

    /**
     * Send the request asynchronously, regardless of whether the request was sent successfully.
     */
    fun publish()

    /**
     * Asynchronously send the request and notify {@code callback} of its response or if an error
     * occurred talking to the server, creating the request, or processing the response.
     */
    fun enqueue(callback: Callback<T>?)

    /**
     * Returns true if this call has been either {@linkplain #publish() published} or {@linkplain
     * #enqueue(Callback) enqueued}. It is an error to publish or enqueue a call more than once.
     */
    fun isExecuted(): Boolean

    /**
     * Cancel this call. An attempt will be made to cancel in-flight calls, and if the call has not
     * yet been executed it never will be.
     */
    fun cancel()

    /** True if {@link #cancel()} was called. */
    fun isCanceled(): Boolean

    /**
     * Returns the original request that initiated this call.
     */
    fun request(): Request
}

/**
 * 对Retrofit方法的调用，该方法将请求发送到串口并返回响应。
 * 每个调用都会产生自己的请求和响应对。
 * 这可用于实现轮询或重试失败的呼叫。(暂未使用)
 * 调用{@link #enqueue}添加订阅，
 * 订阅方式有四种,
 * 1.{@link enqueue},无响应的订阅。
 * 2.{@link enqueue(consumer)},订阅主题，并且接收响应结果和发生错误。
 * 3.{@link enqueue(subscribe)},订阅主题，并且接收订阅结果和发送错误，不监听响应结果。
 * 4.{@link enqueue(fullConsumer)},订阅主题，并且即接收订阅结果，也接收响应结果和发生错误。
 *
 * 调用{@link #unsubscribe}取消订阅
 *
 * 无论哪种情况，都可以使用{@link #cancel}随时取消订阅。
 * */
interface Observable<T> {

    /**
     * Send the subscribe asynchronously, regardless of whether the subscribe was sent successfully.
     */
    fun enqueue()

    /**
     * Asynchronously send the subscribe and notify {@code consumer} of its response or if an error
     * occurred publish and subscribe to the mqtt-server, creating the subscribe/request,
     * or processing the response.
     */
    fun enqueue(consumer: Consumer<T>)

    /**
     * Asynchronously send the subscribe and notify {@code Subscribe} of its response or if an error
     * occurred subscribe to the mqtt-server, creating the subscribe/request, or processing the response.
     */
    fun enqueue(subscribe: Subscribe<T>)

    /**
     * Asynchronously send the request and notify {@code callback} of its response or if an error
     * occurred publish and subscribe to the server, creating the subscribe/request, or processing
     * the response.
     */
    fun enqueue(fullConsumer: FullConsumer<T>)

    /**
     * Asynchronously send the request and notify {@code callback} of its response or if an error
     * occurred unsubscribe topic to the mqtt-server, creating the subscribe/request, or processing
     * the response.
     */
    fun unsubscribe(callback: Subscribe<T>?)

    /**
     * Returns true if this call has been either {@linkplain #enqueue()/#enqueue(Consumer)
     * /#enqueue(Subscribe)/#enqueue(FullConsumer) enqueued} or {@linkplain #unsubscribe(Subscribe)
     * unsubscribed}. It is an error to enqueue or unsubscribe a call more than once.
     */
    fun isExecuted(): Boolean

    /**
     * Cancel this subscribe/unsubscribe. An attempt will be made to cancel in-flight subscribe,
     * and if the call has not yet been executed/unsubscribed it never will be.
     */
    fun cancel()

    /** True if {@link #cancel()} was called. */
    fun isCanceled(): Boolean

    /**
     * Returns the original request that initiated this call.
     */
    fun request(): Request

    /**
     * Returns the original subscribe that initiated this call.
     */
    fun subscribe(): org.sheedon.mqtt.Subscribe
}