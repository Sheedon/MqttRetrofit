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

import org.sheedon.mqtt.RequestBody;
import org.sheedon.mqtt.Subscribe;
import org.sheedon.mqtt.retrofit.mqtt.PathType;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Objects;

/**
 * 参数处理程序
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 14:03
 */
abstract class ParameterHandler<T> {
    abstract void apply(RequestBuilder builder, @Nullable T value) throws IOException;

    final ParameterHandler<Iterable<T>> iterable() {
        return new ParameterHandler<Iterable<T>>() {
            @Override
            void apply(RequestBuilder builder, @Nullable Iterable<T> values)
                    throws IOException {
                if (values == null) return; // Skip null values.

                for (T value : values) {
                    ParameterHandler.this.apply(builder, value);
                }
            }
        };
    }

    final ParameterHandler<Object> array() {
        return new ParameterHandler<Object>() {
            @Override
            void apply(RequestBuilder builder, @Nullable Object values) throws IOException {
                if (values == null) return; // Skip null values.

                for (int i = 0, size = Array.getLength(values); i < size; i++) {
                    ParameterHandler.this.apply(builder, (T) Array.get(values, i));
                }
            }
        };
    }

    /**
     * 通过{@link org.sheedon.mqtt.retrofit.mqtt.Subject}注解 添加的主题
     */
    static final class RelativeTopic extends ParameterHandler<String> {
        @Override
        void apply(RequestBuilder builder, @Nullable String value) {
            Objects.requireNonNull(value, "@Theme parameter is null.");
            builder.setRelativeTopic(value);
        }
    }

    /**
     * 由{@link org.sheedon.mqtt.retrofit.mqtt.Path}的对应类型，来更换一下四项的字段
     * {@link org.sheedon.mqtt.retrofit.mqtt.TOPIC} 发送消息的主题
     * {@link org.sheedon.mqtt.retrofit.mqtt.SUBSCRIBE} 订阅的主题
     * {@link org.sheedon.mqtt.retrofit.mqtt.PAYLOAD} 发送的有效载荷
     * {@link org.sheedon.mqtt.retrofit.mqtt.KEYWORD} 订阅的关键字
     *
     * @param <T> 类型
     */
    static final class Path<T> extends ParameterHandler<T> {
        private final String name;
        private final int pathType;
        private final Converter<T, String> valueConverter;

        Path(String name, int pathType, Converter<T, String> valueConverter) {
            this.name = Objects.requireNonNull(name, "name == null");
            this.pathType = pathType;
            this.valueConverter = valueConverter;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable T value) throws IOException {
            if (value == null) {
                throw new IllegalArgumentException(
                        "Path parameter \"" + name + "\" value must not be null.");
            }
            if (pathType == PathType.TOPIC) {
                // 发送数据的主题
                builder.addTopicPathParam(name, valueConverter.convert(value));
            } else if (pathType == PathType.SUBSCRIBE) {
                // 订阅的主题
                builder.addSubscribeTopicPathParam(name, valueConverter.convert(value));
            } else if (pathType == PathType.PAYLOAD) {
                // 发送的数据
                builder.addPathParam(name, valueConverter.convert(value));
            } else if (pathType == PathType.KEYWORD) {
                // 订阅关键字
                builder.addKeywordPathParam(name, valueConverter.convert(value));
            }
        }
    }

    /**
     * 请求数据配置的参数
     *
     * @param <T> 类型
     */
    static final class Field<T> extends ParameterHandler<T> {
        private final String name;
        private final Converter<T, String> valueConverter;

        Field(String name, Converter<T, String> valueConverter) {
            this.name = Objects.requireNonNull(name, "name == null");
            this.valueConverter = valueConverter;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable T value) throws IOException {
            if (value == null) return; // Skip null values.

            String fieldValue = valueConverter.convert(value);
            if (fieldValue == null) return; // Skip null converted values

            builder.addFormField(name, fieldValue);
        }
    }

    /**
     * 设置请求body，存在三种数据
     * 1. okmqtt订阅对象 Subscribe
     * 2. okmqtt请求body RequestBody
     * 3. 常规请求对象
     *
     * @param <T> 类型
     */
    static final class Body<T> extends ParameterHandler<T> {
        private final Converter<T, String> converter;

        Body(Converter<T, String> converter) {
            this.converter = converter;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable T value) {
            if (value == null) {
                throw new IllegalArgumentException("Body parameter value must not be null.");
            }

            if(value instanceof Subscribe){
                builder.setSubscribeBody((Subscribe) value);
                return;
            }

            if(value instanceof RequestBody){
                builder.setRequestBody((RequestBody) value);
                return;
            }

            String body;
            try {
                body = converter.convert(value);
            } catch (IOException e) {
                throw new RuntimeException("Unable to convert " + value + " to RequestBody", e);
            }
            builder.setBody(body);
        }
    }
}