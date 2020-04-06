package org.sheedon.demo;


import org.sheedon.mqtt.retrofit.Call;
import org.sheedon.mqtt.retrofit.mqtt.BACKNAME;
import org.sheedon.mqtt.retrofit.mqtt.Body;
import org.sheedon.mqtt.retrofit.mqtt.Field;
import org.sheedon.mqtt.retrofit.mqtt.FormEncoded;
import org.sheedon.mqtt.retrofit.mqtt.PAYLOAD;
import org.sheedon.mqtt.retrofit.mqtt.Path;
import org.sheedon.mqtt.retrofit.mqtt.TOPIC;
import org.sheedon.mqtt.retrofit.mqtt.Theme;

import java.util.List;

/**
 * @Description: java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/27 16:20
 */
interface RemoteService {


    @TOPIC("")
    @PAYLOAD("{\"type\":\"get_manager_list\",\"upStartTime\":\"\"}")
    @BACKNAME("get_manager_list")
    Call<RspModel<List<AdminCard>>> getManagerList();

    @BACKNAME("get_manager_list")
    Call<RspModel<List<AdminCard>>> getManagerList(@Body UserSubmitModel body);

    @BACKNAME("get_manager_list")
    Call<RspModel<List<AdminCard>>> getManagerList(@Theme() String topic, @Body UserSubmitModel body);

    @FormEncoded
    @TOPIC("")
    @BACKNAME("get_manager_list")
    Call<RspModel<List<AdminCard>>> getManagerList(@Path("deviceId") String deviceId, @Field("type") String type,
                                                   @Field("upStartTime") String upStartTime);
}
