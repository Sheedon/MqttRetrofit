package org.sheedon.sample.factory

import com.google.gson.Gson
import org.sheedon.mqtt.OkMqttClient
import org.sheedon.mqtt.Topics
import org.sheedon.mqtt.retrofit.Retrofit
import org.sheedon.retrofit.gson.GsonConverterFactory
import org.sheedon.sample.App
import java.util.*
import kotlin.collections.HashMap

/**
 * retrofit 客户端
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/25 22:24
 */
class RetrofitClient {

    private val retrofitMap = HashMap<String, Retrofit>()

    private var mClient: OkMqttClient? = null


    /**
     * 根据传入的baseTopic，和api创建retrofit
     */
    fun <T> createBaseApi(clazz: Class<T>, baseUrl: String): T {
        var retrofit = retrofitMap[clazz.canonicalName!!]
        if (retrofit == null) {
            var builder = Retrofit.Builder()
                .baseTopic(baseUrl)
                .client(loadOkMqttClient())

            builder = setRetrofitBuilder(builder)
            retrofit = builder.build()
            retrofitMap[clazz.canonicalName!!] = retrofit
        }
        return retrofit!!.create(clazz)
    }

    /**
     * 创建OkMqttClient
     */
    private fun loadOkMqttClient(): OkMqttClient {
        if (mClient != null) {
            return mClient!!
        }
        val clientContext = App.instance.applicationContext
        mClient = OkMqttClient.Builder() // Configure the basic parameters of mqtt connection
            .clientInfo(clientContext, serverUri, clientId)
            // 作用于关键字关联的响应信息解析器
            .subscribeBodies(topicsBodies = topicsBodies.toTypedArray())
            .keywordConverter(CallbackNameConverter(Gson()))
            .openLog(true)
            .build()
        return mClient!!
    }


    private fun setRetrofitBuilder(builder: Retrofit.Builder): Retrofit.Builder {
        return builder.apply {
            addConverterFactory(GsonConverterFactory.create())
        }
    }

    // 初始化操作
    fun initConfig() {
        loadOkMqttClient()
    }


    companion object {
        private val instance = RetrofitClient()

        @JvmStatic
        fun getInstance() = instance

        // current client id
        private val clientId = UUID.randomUUID().toString()

        // It is recommended that developers change to their own mqtt-Broker
        //    private static final String serverUri = "tcp://test.mosquitto.org:1883";
        private const val serverUri = "tcp://broker-cn.emqx.io:1883"

        // Topic to subscribe to by default.
        private val topicsBodies: List<Topics> = mutableListOf(
            Topics.build(
                "mq/clouds/cmd/test",
                0,
                true
            )
        )
    }
}