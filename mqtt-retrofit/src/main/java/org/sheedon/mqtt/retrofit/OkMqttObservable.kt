package org.sheedon.mqtt.retrofit

import androidx.annotation.GuardedBy
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage
import org.sheedon.mqtt.*
import java.io.IOException

/**
 * 动态代理「mqtt订阅主题」
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
     * Returns the raw observable, initializing it if necessary. Throws if initializing the raw call throws,
     * or has thrown in previous attempts to create it.
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

    @Throws(IOException::class)
    private fun createRawObservable(): org.sheedon.mqtt.Observable {
        return observableFactory.newObservable(requestFactory.create(args))
    }

    override fun enqueue() {
        val (observable, failure) = createRealObservable()
        if (failure != null) {
            return
        }

        if (canceled) {
            observable?.cancel()
            return
        }

        observable?.enqueue()
    }

    override fun enqueue(consumer: Consumer<T>) {
        val (observable, failure) = createRealObservable()
        if (failure != null) {
            consumer.onFailure(this@OkMqttObservable, failure)
            return
        }

        if (canceled) {
            observable?.cancel()
            return
        }

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

    override fun enqueue(consumer: Subscribe<T>) {
        val (observable, failure) = createRealObservable()
        if (failure != null) {
            consumer.onFailure(this@OkMqttObservable, failure)
            return
        }

        if (canceled) {
            observable?.cancel()
            return
        }

        observable?.enqueue(object : SubscribeBack {
            override fun onFailure(e: Throwable?) {
                consumer.onFailure(this@OkMqttObservable, e)
            }

            override fun onResponse(response: MqttWireMessage?) {
                try {
                    if (response is MqttSubscribe) {
                        consumer.onResponse(this@OkMqttObservable, response)
                    } else {
                        consumer.onResponse(this@OkMqttObservable, null)
                    }
                } catch (e: Throwable) {
                    consumer.onFailure(this@OkMqttObservable, e)
                }
            }

        })
    }

    override fun enqueue(consumer: FullConsumer<T>) {
        val (observable, failure) = createRealObservable()
        if (failure != null) {
            consumer.onFailure(this@OkMqttObservable, failure)
            return
        }

        if (canceled) {
            observable?.cancel()
            return
        }

        observable?.enqueue(object : FullCallback {
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

            override fun onResponse(response: MqttWireMessage?) {
                try {
                    if (response is MqttSubscribe) {
                        consumer.onResponse(this@OkMqttObservable, response)
                    } else {
                        consumer.onResponse(this@OkMqttObservable, MqttSubscribe(null, null))
                    }
                } catch (e: Throwable) {
                    consumer.onFailure(this@OkMqttObservable, e)
                }
            }

        })
    }

    override fun unsubscribe(callback: Subscribe<T>?) {
        val (observable, failure) = createRealObservable()
        if (failure != null) {
            return
        }

        if (canceled) {
            observable?.cancel()
            return
        }

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

    override fun cancel() {
        canceled = true

        var observable: org.sheedon.mqtt.Observable?
        synchronized(this) { observable = rawObservable }
        observable?.cancel()
    }

    override fun isExecuted() = executed

    override fun isCanceled(): Boolean {
        if (canceled) {
            return true
        }
        synchronized(this) { return rawObservable?.isCanceled() == true }
    }
}