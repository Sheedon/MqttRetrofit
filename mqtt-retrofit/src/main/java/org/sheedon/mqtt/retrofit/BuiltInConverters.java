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

import org.sheedon.mqtt.RequestBody;
import org.sheedon.mqtt.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import kotlin.Unit;

/**
 * 创建基础转化工厂类
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/24 15:19
 */
public final class BuiltInConverters extends Converter.Factory {
    /**
     * Not volatile because we don't mind multiple threads discovering this.
     */
    private boolean checkForKotlinUnit = true;

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
                                                            Retrofit retrofit) {
        if (type == ResponseBody.class) {
            return BufferingResponseBodyConverter.INSTANCE;
        }
        if (type == Void.class) {
            return VoidResponseBodyConverter.INSTANCE;
        }
        if (checkForKotlinUnit) {
            try {
                if (type == Unit.class) {
                    return UnitResponseBodyConverter.INSTANCE;
                }
            } catch (NoClassDefFoundError ignored) {
                checkForKotlinUnit = false;
            }
        }
        return null;
    }

    @Override
    public Converter<?, String> requestBodyConverter(Type type,
                                                          Annotation[] parameterAnnotations,
                                                          Annotation[] methodAnnotations, Retrofit retrofit) {
        if (RequestBody.class.isAssignableFrom(Utils.getRawType(type))) {
            return RequestBodyConverter.INSTANCE;
        }
        return null;
    }

    static final class VoidResponseBodyConverter implements Converter<ResponseBody, Void> {
        static final VoidResponseBodyConverter INSTANCE = new VoidResponseBodyConverter();

        @Override
        public Void convert(ResponseBody value) {
            return null;
        }
    }

    public static final class RequestBodyConverter implements Converter<RequestBody, String> {
        static final RequestBodyConverter INSTANCE = new RequestBodyConverter();

        @Override
        public String convert(RequestBody value) {
            return value.toString();
        }
    }

    static final class UnitResponseBodyConverter implements Converter<ResponseBody, Unit> {
        static final UnitResponseBodyConverter INSTANCE = new UnitResponseBodyConverter();

        @Override
        public Unit convert(ResponseBody value) {
            return Unit.INSTANCE;
        }
    }

    static final class BufferingResponseBodyConverter
            implements Converter<ResponseBody, ResponseBody> {
        static final BufferingResponseBodyConverter INSTANCE = new BufferingResponseBodyConverter();

        @Override
        public ResponseBody convert(ResponseBody value) {
            // Buffer the entire body to avoid future I/O.
            return value;
        }
    }

    public static final class ToStringConverter implements Converter<Object, String> {
        static final ToStringConverter INSTANCE = new ToStringConverter();

        @Override
        public String convert(Object value) {
            return value.toString();
        }
    }
}
