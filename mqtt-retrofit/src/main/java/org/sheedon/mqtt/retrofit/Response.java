/*
 * Copyright (C) 2020 Sheedon.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sheedon.mqtt.retrofit;

import androidx.annotation.Nullable;

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
    public static <T> Response<T> success(@Nullable String backTopic, @Nullable T body) {
        return success(body, new org.sheedon.mqtt.Response(backTopic, "OK"));
    }

    /**
     * Create a successful response from {@code rawResponse} with {@code body} as the deserialized
     * body.
     */
    public static <T> Response<T> success(@Nullable T body, org.sheedon.mqtt.Response rawResponse) {
        Objects.requireNonNull(rawResponse, "rawResponse == null");
        if (rawResponse.message().isEmpty()) {
            throw new IllegalArgumentException("rawResponse must be successful response");
        }
        return new Response<>(rawResponse, body, null);
    }

    /**
     * Create a synthetic error response with an RR-binder status message of {@code message}
     * and {@code body} as the error body.
     */
    public static <T> Response<T> error(@Nullable String backTopic, @Nullable String message, ResponseBody body) {
        if (message.isEmpty()) throw new IllegalArgumentException(message);
        return error(body, new org.sheedon.mqtt.Response(backTopic, message, body));
    }

    /**
     * Create an error response from {@code rawResponse} with {@code body} as the error body.
     */
    public static <T> Response<T> error(ResponseBody body, org.sheedon.mqtt.Response rawResponse) {
        Objects.requireNonNull(rawResponse, "rawResponse == null");
        if (rawResponse.message().isEmpty()) {
            throw new IllegalArgumentException("rawResponse should not be successful response");
        }
        return new Response<>(rawResponse, null, body);
    }

    private final org.sheedon.mqtt.Response rawResponse;
    private final @Nullable
    T body;
    private final @Nullable
    ResponseBody errorBody;

    private Response(org.sheedon.mqtt.Response rawResponse, @Nullable T body,
                     @Nullable ResponseBody errorBody) {
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
    public String backTopic() {
        return rawResponse.backTopic();
    }

    /**
     * MQTT status message or null if unknown.
     */
    public String message() {
        return rawResponse.message();
    }

    /**
     * Returns true if {@link #message()} is message not "".
     */
    public boolean isSuccessful() {
        return rawResponse.message().isEmpty();
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
    public @Nullable
    ResponseBody errorBody() {
        return errorBody;
    }

    @Override
    public String toString() {
        return rawResponse.toString();
    }


}
