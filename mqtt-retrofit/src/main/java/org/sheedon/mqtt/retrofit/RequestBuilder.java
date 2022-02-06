/**
 * Copyright (C) 2020 Sheedon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sheedon.mqtt.retrofit;

import android.text.TextUtils;

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

        byte[] payload = getRequestBody().getBytes();

        if (payload.length > 0) {
            mqttMessage.setPayload(payload);
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

        if (body == null || TextUtils.isEmpty(body.getData())) {
            return "";
        }
        return body.getData().replaceAll("\\\\\"", "");
    }
}
