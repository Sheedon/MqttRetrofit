/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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

import androidx.annotation.Nullable;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe;
import org.sheedon.mqtt.ResponseBody;

import java.util.Objects;


/**
 * 反馈内容
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/22 17:30
 */
public final class Response<T> {
    /**
     * Create a synthetic successful response with {@code body} as the deserialized body.
     */
    public static <T> Response<T> success(@Nullable String topic,
                                          @Nullable T body) {
        return success(body,
                new org.sheedon.mqtt.Response("",
                        new ResponseBody(topic, new MqttMessage())));
    }

    /**
     * Create a synthetic successful response with {@code body} as the deserialized body.
     */
    public static <T> Response<T> success(@Nullable String keyword,
                                          @Nullable String topic,
                                          @Nullable T body) {
        return success(body,
                new org.sheedon.mqtt.Response(keyword,
                        new ResponseBody(topic, new MqttMessage())));
    }

    /**
     * Create a synthetic successful response with {@code body} as the deserialized body.
     */
    public static <T> Response<T> success(@Nullable String topic,
                                          @Nullable MqttSubscribe body) {
        return new Response<>(new org.sheedon.mqtt.Response("",
                new ResponseBody(topic, new MqttMessage((body != null ? body.toString() : "").getBytes()))),
                null, null);
    }

    /**
     * Create a successful response from {@code rawResponse} with {@code body} as the deserialized
     * body.
     */
    public static <T> Response<T> success(@Nullable T body, org.sheedon.mqtt.Response rawResponse) {
        Objects.requireNonNull(rawResponse, "rawResponse == null");
        return new Response<>(rawResponse, body, null);
    }

    /**
     * Create a synthetic error response with {@code body} as the error body.
     */
    public static <T> Response<T> error(@Nullable String topic, ResponseBody body) {
        return new Response<>(null, null,
                new org.sheedon.mqtt.Response("",
                        new ResponseBody(topic, new MqttMessage())));
    }

    /**
     * Create an error response from {@code rawResponse} with {@code body} as the error body.
     */
    public static <T> Response<T> error(@Nullable String keyword,
                                        @Nullable String topic,
                                        ResponseBody body) {
        return new Response<>(null, null,
                new org.sheedon.mqtt.Response(keyword,
                        new ResponseBody(topic, new MqttMessage())));
    }

    private final org.sheedon.mqtt.Response rawResponse;
    private final T body;
    private final org.sheedon.mqtt.Response errorBody;

    private Response(org.sheedon.mqtt.Response rawResponse, @Nullable T body,
                     @Nullable org.sheedon.mqtt.Response errorBody) {
        this.rawResponse = rawResponse;
        this.body = body;
        this.errorBody = errorBody;
    }

    /**
     * The raw response from the MQTT client.
     */
    public org.sheedon.mqtt.Response raw() {
        return rawResponse;
    }

    /**
     * MQTT status backTopic or "" if unknown.
     */
    public String topic() {
        if (rawResponse != null && rawResponse.getBody() != null) {
            return rawResponse.getBody().getTopic();
        }
        if (errorBody != null && errorBody.getBody() != null) {
            return errorBody.getBody().getTopic();
        }
        return "";
    }


    /**
     * Returns true if rawResponse is not null.
     */
    public boolean isSuccessful() {
        return rawResponse != null;
    }

    /**
     * The deserialized response body of a {@linkplain #isSuccessful() successful} response.
     */
    public @Nullable
    T body() {
        return body;
    }

    /**
     * The raw response body of an {@linkplain #isSuccessful() unsuccessful} response.
     */
    public org.sheedon.mqtt.Response errorBody() {
        return errorBody;
    }

    @Override
    public String toString() {
        return "Response{" +
                "rawResponse=" + rawResponse +
                ", body=" + body +
                ", errorBody=" + errorBody +
                '}';
    }
}
