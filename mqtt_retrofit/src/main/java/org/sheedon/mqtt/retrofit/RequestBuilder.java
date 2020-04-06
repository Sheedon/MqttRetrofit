package org.sheedon.mqtt.retrofit;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.sheedon.mqtt.Request;
import org.sheedon.mqtt.RequestBody;

/**
 * 请求构建
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 14:04
 */
public class RequestBuilder {

    private String topic;
    private @Nullable
    FormBody.Builder formBuilder;
    private @Nullable
    RequestBody body;

    private final org.sheedon.mqtt.RequestBuilder requestBuilder;

    private MqttMessage mqttMessage;
    private BindCallback bindCallback;

    RequestBuilder(String topic, MqttMessage mqttMessage,
                   BindCallback bindCallback, boolean isFormEncoded, Gson gson) {
        this.topic = topic;
        this.mqttMessage = mqttMessage;
        this.bindCallback = bindCallback;
        this.requestBuilder = new org.sheedon.mqtt.RequestBuilder();

        if (isFormEncoded) {
            this.formBuilder = new FormBody.Builder().bindGson(gson);
        }
    }


    void setRelativeTopic(Object value) {
        Utils.checkNotNull(value, "topic == null");

        topic = value.toString();
    }


    void addPathParam(String name, String value, boolean encoded) {
        if (topic == null) {
            // The relative URL is cleared when the first query parameter is set.
            throw new AssertionError();
        }
        topic = topic.replace("{" + name + "}", value);
    }

    void addFormField(String name, String value, boolean encoded) {
        if (formBuilder == null)
            return;

        if (encoded) {
            formBuilder.addEncoded(name, value);
        } else {
            formBuilder.add(name, value);
        }
    }

    public void setBody(RequestBody body) {
        this.body = body;
    }

    Request build() {
        String topic = this.topic;
        if (topic == null) {
            throw new IllegalArgumentException(
                    "Base Topic is Null");
        }

        byte[] payload = mqttMessage.getPayload();
        if (payload.length == 0) {
            payload = getRequestBody().getBytes();

            if (payload.length > 0) {
                mqttMessage.setPayload(payload);
            }
        }


        return requestBuilder
                .topic(topic)
                .message(mqttMessage)
                .delayMilliSecond(bindCallback.getDelayMilliSecond())
                .backName(bindCallback.getBackName())
                .build();
    }

    /**
     * 获取请求数据
     */
    private String getRequestBody() {
        RequestBody body = this.body;
        if (body == null) {
            // Try to pull from one of the builders.
            if (formBuilder != null) {
                body = formBuilder.build();
            } else {
                // Body is absent, make an empty body.
                body = new RequestBody();
            }
        }

        return body.getData().replaceAll("\\\\\"", "");
    }
}
