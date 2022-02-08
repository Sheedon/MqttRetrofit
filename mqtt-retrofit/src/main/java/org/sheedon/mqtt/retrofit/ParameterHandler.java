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

import org.sheedon.mqtt.RequestBody;

import java.io.IOException;
import java.lang.reflect.Array;

import static org.sheedon.mqtt.retrofit.Utils.checkNotNull;


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

    static final class RelativeTopic extends ParameterHandler<String> {
        @Override
        void apply(RequestBuilder builder, @Nullable String value) {
            checkNotNull(value, "@Theme parameter is null.");
            builder.setRelativeTopic(value);
        }
    }

    static final class Path<T> extends ParameterHandler<T> {
        private final String name;
        private final Converter<T, String> valueConverter;
        private final boolean encoded;

        Path(String name, Converter<T, String> valueConverter, boolean encoded) {
            this.name = checkNotNull(name, "name == null");
            this.valueConverter = valueConverter;
            this.encoded = encoded;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable T value) throws IOException {
            if (value == null) {
                throw new IllegalArgumentException(
                        "Path parameter \"" + name + "\" value must not be null.");
            }
            builder.addPathParam(name, valueConverter.convert(value), encoded);
        }
    }

    static final class Field<T> extends ParameterHandler<T> {
        private final String name;
        private final Converter<T, String> valueConverter;
        private final boolean encoded;

        Field(String name, Converter<T, String> valueConverter, boolean encoded) {
            this.name = checkNotNull(name, "name == null");
            this.valueConverter = valueConverter;
            this.encoded = encoded;
        }

        @Override
        void apply(RequestBuilder builder, @Nullable T value) throws IOException {
            if (value == null) return; // Skip null values.

            String fieldValue = valueConverter.convert(value);
            if (fieldValue == null) return; // Skip null converted values

            builder.addFormField(name, fieldValue, encoded);
        }
    }

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