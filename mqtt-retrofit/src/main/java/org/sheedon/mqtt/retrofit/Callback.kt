package org.sheedon.mqtt.retrofit

import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe

/**
 * 传达来自服务器请求的响应。响应结果对应请求，且只一个定义只有一个格式。
 *
 * <p>回调方法使用 {@link Retrofit} 回调执行器执行。如果没有指定，则使用以下默认值：
 *
 * <ul>
 *   <li>Android：回调在应用程序的主 (UI) 线程上执行。
 *   <li>JVM: 回调在执行请求的后台线程上执行。（暂未实现！）
 * </ul>
 *
 * @param <T> 成功的响应正文类型。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/4 15:48
 */
interface Callback<T> {
    /**
     * 以传达mqtt-service的响应结果
     */
    fun onResponse(call: Call<T>, response: Response<T>?)

    /**
     * 当与服务器通信发生网络异常时或在创建请求或处理响应时发生意外异常时调用。
     */
    fun onFailure(call: Call<T>, t: Throwable?)
}

/**
 * 传达来自订阅服务器对应主题的响应。响应结果对应订阅，且只一个定义只有一个格式。
 */
interface Consumer<T> : IFailureConsumer<T> {
    /**
     * 以传达收到的 MQTT 响应调用。
     */
    fun onResponse(observable: Observable<T>, response: Response<T>?)
}

/**
 * 传达来自订阅服务器或者本地对应主题的订阅情况。
 */
interface Subscribe<T> : IFailureConsumer<T> {
    /**
     * 在mqtt订阅成功响应时调用。
     */
    fun onResponse(observable: Observable<T>, response: MqttSubscribe?)
}

/**
 * 传达来自订阅服务器或者本地对应主题的订阅失败情况。
 * 或在是创建订阅或处理响应时发生意外异常时的情况。
 */
interface IFailureConsumer<T> {
    /**
     * 当与服务器通信发生网络异常时或在创建请求或处理响应时发生意外异常时调用。
     */
    fun onFailure(observable: Observable<T>, t: Throwable?)
}

/**
 * 传达来自订阅服务器或者本地对应主题的订阅情况，以及监听对应主题响应结果的情况。
 */
interface FullConsumer<T> : Consumer<T>, Subscribe<T>



