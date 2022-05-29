package org.sheedon.sample

import org.sheedon.mqtt.Subscribe
import org.sheedon.mqtt.retrofit.Call
import org.sheedon.mqtt.retrofit.Observable
import org.sheedon.mqtt.retrofit.mqtt.*
import org.sheedon.sample.factory.RetrofitClient
import org.sheedon.sample.model.AdminCard
import org.sheedon.sample.model.User

/**
 * @Description: java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/27 16:20
 */

internal val remoteService: RemoteService by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
    RetrofitClient.getInstance().createBaseApi(
        RemoteService::class.java,
        "mq/sheedon/"
    )
}

internal interface RemoteService {

    @FormEncoded
    @TOPIC("user/test")
    fun addUserAndOrgList(
        @Field("userName") name: String,
        @Field("orgList") orgList: List<String>
    ): Call<Void>


    @FormEncoded
    @TOPIC("user/addUserAndOrg")
    @CHARSET("GBK", autoEncode = false)
    fun addUserAndOrg(
        @Field("userName") name: String,
        @Field("org") org: String
    ): Call<Void>


    @TOPIC("user/{id}")
    @PAYLOAD("test-{name}")
    fun notifyUser(
        @Path("id") id: String,
        @Path("name", type = PathType.PAYLOAD) name: String
    ): Call<Void>


    @TOPIC("user/{id}")
    @SUBSCRIBE("cmd/user")
    fun getUserById(
        @Path("id") id: String
    ): Call<User>

    @TOPIC("user/{id}")
    @SUBSCRIBE("cmd/user")
    fun addUser(
        @Body adminCard: AdminCard
    ): Call<User>


    @SUBSCRIBE("cmd/user")
    fun listenUser(): Observable<User>


    fun listenArray(@Body subscribe: Subscribe): Observable<User>
}