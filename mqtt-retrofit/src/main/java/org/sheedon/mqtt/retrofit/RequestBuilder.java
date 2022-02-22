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

import org.sheedon.mqtt.Request;

import java.nio.charset.Charset;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 请求构建
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 14:04
 */
public class RequestBuilder {

    private String topic;
    private final String relativePayload;
    private final String charset;
    private final int qos;
    private final boolean retained;
    private final long timeout;
    private final TimeUnit timeUnit;
    private final String backTopic;
    private final Request.Builder requestBuilder;
    private @Nullable
    FormBody.Builder formBuilder;
    private String body = null;

    RequestBuilder(String topic,
                   String relativePayload,
                   String charset,
                   int qos,
                   boolean retained,
                   long timeout, TimeUnit timeUnit,
                   String backTopic,
                   boolean isFormEncoded) {
        this.topic = topic;
        this.relativePayload = relativePayload;
        this.charset = charset;
        this.qos = qos;
        this.retained = retained;
        this.timeout = timeout;
        this.timeUnit = timeUnit;
        this.backTopic = backTopic;
        this.requestBuilder = new Request.Builder();
        if (isFormEncoded) {
            this.formBuilder = new FormBody.Builder(charset.isEmpty() ? null : Charset.forName(charset));
        }
    }


    void setRelativeTopic(String value) {
        Objects.requireNonNull(value, "topic == null");
        topic = value;
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

    public void setBody(String body) {
        this.body = body;
    }

    Request.Builder get() {
        String topic = this.topic;
        if (topic == null) {
            throw new IllegalArgumentException(
                    "Base Topic is Null");
        }

        String payload = getRequestBody();

        return requestBuilder
                .topic(topic)
                .data(payload)
                .delayMilliSecond(TimeUnit.MILLISECONDS.convert(timeout, timeUnit))
                .qos(qos)
                .retained(retained)
                .backTopic(backTopic);

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
