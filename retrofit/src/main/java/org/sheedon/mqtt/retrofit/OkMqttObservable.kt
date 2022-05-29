package org.sheedon.mqtt.retrofit

import androidx.annotation.GuardedBy
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage
import org.sheedon.mqtt.*
import java.io.IOException

/**
 * 关于Observable在OkMqtt的实现类，用于代理创建订阅调度
 *
 * 通过[observableFactory]将请求数据存储工厂[requestFactory]配置对应请求参数[args]，以获取真实Observable，
 *
 * 对于Observable代理执行有以下四项
 * 1.
 *
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/6 22:34
 */
internal class OkMqttObservable<T> constructor(
    private val requestFactory: RequestFactory,
    private val args: Array<Any>,
    private val observableFactory: ObservableFactory,
    private val responseConverter: Converter<ResponseBody, T>
) : Observable<T> {


    @Volatile
    private var canceled: Boolean = false

    @GuardedBy("this")
    private var rawObservable: org.sheedon.mqtt.Observable? = null

    @GuardedBy("this") // Either a RuntimeException, non-fatal Error, or IOException.
    private var creationFailure: Throwable? = null

    @GuardedBy("this")
    private var executed = false

    /**
     * 得到一个请求或订阅对象
     */
    override fun request(): Request {
        return try {
            getRawObservable().request()
        } catch (e: RuntimeException) {
            Utils.throwIfFatal(e) // Do not assign a fatal error to creationFailure.
            creationFailure = e
            throw e
        } catch (e: Error) {
            Utils.throwIfFatal(e)
            creationFailure = e
            throw e
        } catch (e: IOException) {
            creationFailure = e
            throw RuntimeException("Unable to create request.", e)
        }
    }

    /**
     * 得到一个需要订阅主题组
     */
    override fun subscribe(): org.sheedon.mqtt.Subscribe {
        return try {
            getRawObservable().subscribe()
        } catch (e: java.lang.RuntimeException) {
            Utils.throwIfFatal(e) // Do not assign a fatal error to creationFailure.
            creationFailure = e
            throw e
        } catch (e: java.lang.Error) {
            Utils.throwIfFatal(e)
            creationFailure = e
            throw e
        } catch (e: IOException) {
            creationFailure = e
            throw java.lang.RuntimeException("Unable to create request.", e)
        }
    }

    /**
     * 返回原始可观察对象，如有必要，将其初始化。如果初始化原始调用抛出，或者在之前创建它的尝试中已经抛出，则抛出。
     */
    @GuardedBy("this")
    @Throws(IOException::class)
    private fun getRawObservable(): org.sheedon.mqtt.Observable {
        val observable = rawObservable
        if (observable != null) return observable
        if (creationFailure != null) {
            when (creationFailure) {
                is IOException -> {
                    throw java.lang.RuntimeException("Unable to create request.", creationFailure)
                }
                is java.lang.RuntimeException -> {
                    throw (creationFailure as java.lang.RuntimeException?)!!
                }
                else -> {
                    throw (creationFailure as java.lang.Error?)!!
                }
            }
        }
        return try {
            createRawObservable().also { rawObservable = it }
        } catch (e: java.lang.RuntimeException) {
            Utils.throwIfFatal(e) // Do not assign a fatal error to creationFailure.
            creationFailure = e
            throw e
        } catch (e: java.lang.Error) {
            Utils.throwIfFatal(e)
            creationFailure = e
            throw e
        } catch (e: IOException) {
            Utils.throwIfFatal(e)
            creationFailure = e
            throw e
        }
    }

    /**
     * 代理创建原始订阅调用
     */
    @Throws(IOException::class)
    private fun createRawObservable(): org.sheedon.mqtt.Observable {
        val count = args.filterIsInstance<org.sheedon.mqtt.Subscribe>().count()
        return if (count > 0) {
            observableFactory.newObservable(requestFactory.createSubscribe(args))
        } else {
            observableFactory.newObservable(requestFactory.create(args))
        }
    }

    /**
     * 订阅一个或者一组主题，通过该方法执行订阅，代表无需监听是否订阅成功以及不监听在此监听响应结果。
     */
    override fun enqueue() {
        // 构造真实的观察者observable和错误消息failure
        val (observable, failure) = createRealObservable()
        if (failure != null) {
            return
        }

        // 若状态为取消，则调度取消动作，不在执行后续请求动作
        if (canceled) {
            observable?.cancel()
            return
        }

        // 执行订阅
        observable?.enqueue()
    }

    /**
     * 订阅一个或者一组主题，通过该方法执行订阅，并且在此监听响应结果，然而订阅成功在此不反馈结果，
     * 或者网络连接、mqtt断开连接等情况下执行错误回调。
     *
     * @param consumer 响应消息消费者
     */
    override fun enqueue(consumer: Consumer<T>) {
        // 构造真实的观察者observable和错误消息failure
        val (observable, failure) = createRealObservable()
        // 若错误内容不为空，则直接反馈错误
        if (failure != null) {
            consumer.onFailure(this@OkMqttObservable, failure)
            return
        }

        // 若状态为取消，则调度取消动作，不在执行后续请求动作
        if (canceled) {
            observable?.cancel()
            return
        }

        // 订阅消息入队
        observable?.enqueue(object : ObservableBack {
            override fun onFailure(e: Throwable?) {
                consumer.onFailure(this@OkMqttObservable, e)
            }


            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            override fun onResponse(
                observable: org.sheedon.mqtt.Observable,
                rawResponse: org.sheedon.mqtt.Response
            ) {
                try {
                    val response: Response<T> = parseResponse(rawResponse)
                    consumer.onResponse(this@OkMqttObservable, response)
                } catch (e: Throwable) {
                    consumer.onFailure(this@OkMqttObservable, e)
                }
            }

        })
    }

    /**
     * 订阅一个或者一组主题，通过该方法执行订阅，并且在此监听订阅情况，然而响应消息在此不反馈，
     * 或者网络连接、mqtt断开连接等情况下执行错误回调。
     *
     * @param subscribe 订阅消息消费者
     */
    override fun enqueue(subscribe: Subscribe<T>) {
        // 构造真实的观察者observable和错误消息failure
        val (observable, failure) = createRealObservable()
        // 若错误内容不为空，则直接反馈错误
        if (failure != null) {
            subscribe.onFailure(this@OkMqttObservable, failure)
            return
        }

        // 若状态为取消，则调度取消动作，不在执行后续请求动作
        if (canceled) {
            observable?.cancel()
            return
        }

        // 订阅消息入队
        observable?.enqueue(object : SubscribeBack {
            override fun onFailure(e: Throwable?) {
                subscribe.onFailure(this@OkMqttObservable, e)
            }

            override fun onResponse(response: MqttWireMessage?) {
                try {
                    if (response is MqttSubscribe) {
                        subscribe.onResponse(this@OkMqttObservable, response)
                    } else {
                        subscribe.onResponse(this@OkMqttObservable, null)
                    }
                } catch (e: Throwable) {
                    subscribe.onFailure(this@OkMqttObservable, e)
                }
            }

        })
    }

    /**
     * 订阅一个或者一组主题，通过该方法执行订阅，并且在此监听订阅情况和响应结果，或者网络连接、mqtt断开连接等情况下执行错误回调。
     *
     * @param fullConsumer 订阅消息消费者
     */
    override fun enqueue(fullConsumer: FullConsumer<T>) {
        // 构造真实的观察者observable和错误消息failure
        val (observable, failure) = createRealObservable()
        // 若错误内容不为空，则直接反馈错误
        if (failure != null) {
            fullConsumer.onFailure(this@OkMqttObservable, failure)
            return
        }

        // 若状态为取消，则调度取消动作，不在执行后续请求动作
        if (canceled) {
            observable?.cancel()
            return
        }

        // 订阅消息入队
        observable?.enqueue(object : FullCallback {
            override fun onFailure(e: Throwable?) {
                fullConsumer.onFailure(this@OkMqttObservable, e)
            }

            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            override fun onResponse(
                observable: org.sheedon.mqtt.Observable,
                rawResponse: org.sheedon.mqtt.Response
            ) {
                try {
                    val response: Response<T> = parseResponse(rawResponse)
                    fullConsumer.onResponse(this@OkMqttObservable, response)
                } catch (e: Throwable) {
                    fullConsumer.onFailure(this@OkMqttObservable, e)
                }
            }

            override fun onResponse(response: MqttWireMessage?) {
                try {
                    if (response is MqttSubscribe) {
                        fullConsumer.onResponse(this@OkMqttObservable, response)
                    } else {
                        fullConsumer.onResponse(this@OkMqttObservable, MqttSubscribe(null, null))
                    }
                } catch (e: Throwable) {
                    fullConsumer.onFailure(this@OkMqttObservable, e)
                }
            }

        })
    }

    /**
     * 取消订阅一个或者一组主题，通过该方法执行取消订阅，若[callback]不为空，则在此监听取消订阅情况或者网络连接、
     * mqtt断开连接等情况下执行错误回调，否则只是取消订阅，不监听处理结果。
     *
     * @param callback 订阅消息消费者
     */
    override fun unsubscribe(callback: Subscribe<T>?) {
        // 构造真实的观察者observable和错误消息failure
        val (observable, failure) = createRealObservable()
        // 若错误内容不为空，则直接反馈错误
        if (failure != null) {
            callback?.onFailure(this@OkMqttObservable, failure)
            return
        }

        // 若状态为取消，则调度取消动作，不在执行后续请求动作
        if (canceled) {
            observable?.cancel()
            return
        }

        // 取消订阅消息入队
        if (callback == null) {
            observable?.unsubscribe(null)
        } else {
            observable?.unsubscribe(object : SubscribeBack {
                override fun onFailure(e: Throwable?) {
                    callback.onFailure(this@OkMqttObservable, e)
                }

                override fun onResponse(response: MqttWireMessage?) {
                    if (response is MqttSubscribe) {
                        callback.onResponse(this@OkMqttObservable, response)
                    } else {
                        callback.onResponse(this@OkMqttObservable, MqttSubscribe(null, null))
                    }
                }

            })
        }
    }


    /**
     * 代理创建原始调用
     *
     * @return org.sheedon.mqtt.Call okmqtt中的Call
     * @throws IOException
     */
    @Synchronized
    private fun createRealObservable(): Pair<org.sheedon.mqtt.Observable?, Throwable?> {
        var observable: org.sheedon.mqtt.Observable?
        var failure: Throwable?

        observable = rawObservable
        failure = creationFailure
        if (observable == null && failure == null) {
            try {
                rawObservable = createRawObservable()
                observable = rawObservable
            } catch (t: Throwable) {
                creationFailure = t
                failure = creationFailure
            }
        }

        return Pair(observable, failure)
    }

    /**
     * 将原始响应结果转化为封装后的Response
     *
     * @param rawResponse 原始响应结果
     * @return 封装响应结果
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun parseResponse(rawResponse: org.sheedon.mqtt.Response): Response<T> {
        val rawBody: ResponseBody? = rawResponse.body

        return try {
            val body = responseConverter.convert(rawBody)
            Response.success(body, rawResponse)
        } catch (e: java.lang.RuntimeException) {
            // If the underlying source threw an exception, propagate that rather than indicating it was
            // a runtime exception.
            throw e
        }
    }

    /**
     * 取消请求
     */
    override fun cancel() {
        canceled = true

        var observable: org.sheedon.mqtt.Observable?
        synchronized(this) { observable = rawObservable }
        observable?.cancel()
    }

    /**
     * 是否已被执行
     */
    override fun isExecuted() = executed

    /**
     * 是否取消请求
     */
    override fun isCanceled(): Boolean {
        if (canceled) {
            return true
        }
        synchronized(this) { return rawObservable?.isCanceled() == true }
    }
}