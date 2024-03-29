/*
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

import java.util.Objects;

/**
 * Exception for an unexpected, MQTT response.
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/6 1:57 下午
 */
public class MqttException extends RuntimeException {
    private static String getMessage(Response<?> response) {
        Objects.requireNonNull(response, "response == null");
        return "MQTT " + response.topic() + " " + response.errorBody();
    }

    private final String topic;
    private final transient Response<?> response;

    public MqttException(Response<?> response) {
        super(getMessage(response));
        this.topic = response.topic();
        this.response = response;
    }

    /**
     * MQTT status topic.
     */
    public String topic() {
        return topic;
    }

    /**
     * The full MQTT response. This may be null if the exception was serialized.
     */
    public @Nullable
    Response<?> response() {
        return response;
    }
}
