package org.sheedon.demo;


import com.google.gson.Gson;

import org.sheedon.demo.converters.CallbackNameConverter;
import org.sheedon.mqtt.OkMqttClient;
import org.sheedon.mqtt.Response;
import org.sheedon.mqtt.SubscribeBody;
import org.sheedon.mqtt.retrofit.Callback;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/18 13:26
 */
public class MqttClient {

    public static final MqttClient getInstance() {
        return MqttClientHolder.INSTANCE;
    }

    private OkMqttClient mClient;

    private static class MqttClientHolder {
        private static final MqttClient INSTANCE = new MqttClient();
    }

    private MqttClient() {
        createClient();
    }

    private void createClient() {
        // 创建MqttClient

        String clientId = "xxx";
        String serverUri = "xxx";
//        if (clientId == null || clientId.trim().equals(""))
//            return;

        List<SubscribeBody> subscribeBodies = new ArrayList<>();
        subscribeBodies.add(SubscribeBody.Companion.build("xxxx", 1));
        subscribeBodies.add(SubscribeBody.Companion.build("xxxx", 1));


        if (mClient == null) {
            mClient = new OkMqttClient.Builder()
                    .clientInfo(App.getInstance(), serverUri, clientId)
                    .subscribeBodies(null, subscribeBodies.toArray(new SubscribeBody[0]))
                    .addBackTopicConverter(new CallbackNameConverter(new Gson()))
                    .openLog(true,true)
//                    .callback(this)
                    .build();
        }
    }

    public OkMqttClient getClient() {
        return mClient;
    }

    public void publish(String message, String backName, Callback.Call<Response> responseCallback) {

//        if (mClient == null) {
//            if (responseCallback != null)
//                responseCallback.onFailure(new ConnectException("未连接"));
//            return;
//        }

//        Request request = new RequestBuilder()
//                .payload(message)
//                .backName(backName)
//                .build();
//
//        Call call = mClient.newCall(request);
//        call.enqueue(responseCallback);

    }

    public void publish(String message, String backName) {
//        Request request = new RequestBuilder()
//                .payload(message)
//                .backName(backName)
//                .build();
//
//        Call call = mClient.newCall(request);
//        call.publishNotCallback();

    }

//    @Override
//    public void connectComplete(boolean reconnect, String serverURI) {
//
//    }
//
//    @Override
//    public void connectionLost(Throwable cause) {
//
//    }
//
//    @Override
//    public void messageArrived(String topic, MqttMessage message) {
//
//    }
//
//    @Override
//    public void deliveryComplete(IMqttDeliveryToken token) {
//
//    }
//
//    @Override
//    public void messageArrived(String topic, String data) {
//
//    }
//
//    @Override
//    public void onSuccess(IMqttToken asyncActionToken) {
//
//    }
//
//    @Override
//    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//
//    }
}
