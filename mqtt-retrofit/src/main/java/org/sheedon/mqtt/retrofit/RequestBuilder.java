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

import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.sheedon.mqtt.Request;
import org.sheedon.mqtt.RequestBody;
import org.sheedon.mqtt.Subscribe;
import org.sheedon.mqtt.SubscriptionType;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 请求构建，构建两类对象
 * <p>
 * 1.请求对象，用于mqtt publish，请求响应，单一订阅
 * 2.订阅对象，多主题订阅
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 14:04
 */
public class RequestBuilder {

    private String topic;
    private String relativePayload;
    private final int qos;
    private final boolean retained;

    private final String charset;
    private final boolean autoEncode;

    private final long timeout;
    private final TimeUnit timeUnit;

    private String subscribeTopic;
    private final int subscribeQos;
    private final boolean attachRecord;
    private final SubscriptionType subscriptionType;

    private final String keyword;
    private final Request.Builder requestBuilder;
    private final Subscribe.Builder subscribeBuilder;
    private @Nullable
    FormBody.Builder formBuilder;
    private String body = null;

    private Subscribe subscribeBody;
    private RequestBody requestBody;


    public RequestBuilder(String topic, int qos, boolean retained,
                          long timeout, TimeUnit timeUnit, String relativePayload,
                          String subscribeTopic, int subscribeQos,
                          boolean attachRecord, SubscriptionType subscriptionType,
                          String keyword,
                          String charset, boolean autoEncode, boolean isFormEncoded) {
        this.topic = topic;
        this.qos = qos < 0 || qos > 2 ? 0 : qos;
        this.retained = retained;

        this.timeout = timeout;
        this.timeUnit = timeUnit;

        this.relativePayload = relativePayload;

        this.subscribeTopic = subscribeTopic;
        this.subscribeQos = subscribeQos;
        this.attachRecord = attachRecord;
        this.subscriptionType = subscriptionType;

        this.keyword = keyword;

        this.charset = charset;
        this.autoEncode = autoEncode;

        this.requestBuilder = new Request.Builder();
        this.subscribeBuilder = new Subscribe.Builder();
        if (isFormEncoded) {
            this.formBuilder = new FormBody.Builder();
        }
    }


    /**
     * 通过 {@link org.sheedon.mqtt.retrofit.mqtt.Subject} 添加的主题
     *
     * @param value 发送消息主题
     */
    void setRelativeTopic(String value) {
        Objects.requireNonNull(value, "topic == null");
        topic = value;
    }

    /**
     * 往 {@link org.sheedon.mqtt.retrofit.mqtt.TOPIC} 根据 {@link org.sheedon.mqtt.retrofit.mqtt.Path}
     * 替换关键字段
     *
     * @param name  字段名
     * @param value 字段值
     */
    void addTopicPathParam(String name, String value) {
        if (topic == null) {
            // The relative URL is cleared when the first query parameter is set.
            throw new AssertionError();
        }
        topic = topic.replace("{" + name + "}", value);
    }

    void addSubscribeTopicPathParam(String name, String value) {
        if (subscribeTopic == null) {
            subscribeTopic = "";
        }
        subscribeTopic = subscribeTopic.replace("{" + name + "}", value);
    }

    void addPathParam(String name, String value) {
        if (relativePayload == null) {
            // The relative URL is cleared when the first query parameter is set.
            relativePayload = "";
        }
        relativePayload = relativePayload.replace("{" + name + "}", value);
    }

    void addKeywordPathParam(String name, String value) {
        if (relativePayload == null) {
            // The relative URL is cleared when the first query parameter is set.
            relativePayload = "";
        }
        relativePayload = relativePayload.replace("{" + name + "}", value);
    }

    void addFormField(String name, String value) {
        if (formBuilder == null)
            return;

        formBuilder.add(name, value);
    }

    void setSubscribeBody(Subscribe value) {
        subscribeBody = value;
    }

    void setRequestBody(RequestBody value) {
        requestBody = value;
    }

    void setBody(String body) {
        this.body = body;
    }

    Request.Builder get() {
        String topic = this.topic;
        if (topic == null && subscribeTopic == null && keyword == null) {
            throw new IllegalArgumentException(
                    "If you need to send messages, please configure topic, " +
                            "if you need to configure subscription messages, " +
                            "please configure subscribe Topic or keyword");
        }

        // 设置主题
        if (!TextUtils.isEmpty(topic)) {
            requestBuilder.topic(topic, qos, retained);
        }

        // 设置订阅主题
        if (!TextUtils.isEmpty(subscribeTopic)) {
            requestBuilder.subscribeTopic(subscribeTopic, subscribeQos, null, attachRecord, subscriptionType);
        }

        // 关键字
        if (!TextUtils.isEmpty(keyword)) {
            requestBuilder.keyword(keyword);
        }

        // 直接设置请求body
        if (requestBody != null) {
            requestBuilder.body(requestBody);
        }

        // 设置编码格式
        String currentCharset = charset == null ? "" : charset;
        // 设置有效载荷
        String payload = getRequestBody();

        return requestBuilder
                // 设置有效载荷
                .data(payload)
                // 设置超时时间
                .delayMilliSecond(TimeUnit.MILLISECONDS.convert(timeout, timeUnit))
                // 设置编码格式
                .charset(currentCharset, autoEncode);

    }

    Subscribe getSubscribe() {
        if (subscribeBody != null) {
            return subscribeBody;
        }

        if (subscribeTopic == null && keyword == null) {
            throw new IllegalArgumentException(
                    "if you need to configure subscription messages, " +
                            "please configure subscribe Topic or keyword");
        }

        // 设置订阅主题和关键字
        String currentTopic = subscribeTopic == null ? "" : subscribeTopic;
        subscribeBuilder.add(currentTopic, keyword, subscribeQos, attachRecord, subscriptionType);

        return subscribeBuilder.build();
    }

    /**
     * 获取请求数据
     */
    private String getRequestBody() {
        String body = this.body;
        if (body == null) {
            // Try to pull from one of the builders.
            if (formBuilder != null) {
                body = formBuilder.build();
            } else {
                // Body is absent, make body by annotation parameter.
                body = relativePayload;
            }
        }

        if (body == null) {
            return "";
        }
        return body.replaceAll("\\\\\"", "");
    }
}
