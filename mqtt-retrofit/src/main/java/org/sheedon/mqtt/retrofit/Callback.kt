package org.sheedon.mqtt.retrofit

import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe

/**
 * 转化反馈内容
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/4 15:48
 */
interface Callback<T> {
    /**
     * Invoked for a received MQTT response.
     */
    fun onResponse(call: Call<T>, response: Response<T>?)

    /**
     * Invoked when a network exception occurred talking to the server or when an unexpected
     * exception occurred creating the request or processing the response.
     */
    fun onFailure(call: Call<T>, t: Throwable?)
}

interface Consumer<T> :IFailureConsumer<T>{
    /**
     * Invoked for a received MQTT response.
     */
    fun onResponse(observable: Observable<T>, response: Response<T>?)
}

interface Subscribe<T> :IFailureConsumer<T>{
    /**
     * Invoked for a received MQTT response.
     */
    fun onResponse(observable: Observable<T>, response: MqttSubscribe?)
}

interface IFailureConsumer<T> {
    /**
     * Invoked when a network exception occurred talking to the server or when an unexpected
     * exception occurred creating the request or processing the response.
     */
    fun onFailure(observable: Observable<T>, t: Throwable?)
}

interface FullConsumer<T> : Consumer<T>, Subscribe<T>



