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
 * 旨在于构建基础版的转换工厂类，以处理mqtt请求和订阅对象的转化模式，以及对反馈格式的过滤。
 * 而且BuiltInConverters的优先级比所有自定义工厂要高，以避免其他工厂覆盖它的方法。
 * <p>
 * 其中转换方法有:
 * 1.请求对象转化方法，只是核实是否是{@link RequestBody}或者是其子类，是则默认实现{@link RequestBody} 转 String，
 * 否则让其他「请求转换器」实现。
 * 2.响应结果转化方法，用于过滤响应结果和配置结果类一致，或者为Java中的Void，或者是Kotlin中的Unit，都无需转化。
 * 3.String转化方法，一般在于请求对象根据注解信息创建时，由子类实现。
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


    /**
     * 请求对象转化方法，只是核实是否是{@link RequestBody}或者是其子类，是则默认实现{@link RequestBody} 转 String，
     * 否则让其他「请求转换器」实现。
     *
     * @param type                 方法中形参类型
     * @param parameterAnnotations 方法中形参的注解配置
     * @param methodAnnotations    方法的注解配置
     * @param retrofit             Retrofit
     * @return 得到请求数据转化器
     */
    @Override
    public Converter<?, String> requestBodyConverter(Type type,
                                                     Annotation[] parameterAnnotations,
                                                     Annotation[] methodAnnotations, Retrofit retrofit) {
        if (RequestBody.class.isAssignableFrom(Utils.getRawType(type))) {
            return RequestBodyConverter.INSTANCE;
        }
        return null;
    }

    /**
     * 响应结果转换方法，用于过滤无需转化的响应结果。
     * 包括：1.响应结果和配置结果类一致，2.Java中的Void，3.Kotlin中的Unit。
     * 除此之外都需要其他构造器来实现。
     *
     * @param type        responseType，响应结果类型
     * @param annotations 方法的注解配置
     * @param retrofit    Retrofit
     * @return 得到响应数据转化器
     */
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

    /**
     * 请求数据转化器，目标将「RequestBody」转化为「String」，实际上就是提取 {@link RequestBody.data}
     */
    public static final class RequestBodyConverter implements Converter<RequestBody, String> {
        static final RequestBodyConverter INSTANCE = new RequestBodyConverter();

        @Override
        public String convert(RequestBody value) {
            return value.data();
        }
    }

    /**
     * 单例实现Void响应结果转化类，代表响应结果客户端不需要接收，直接返回null
     */
    static final class VoidResponseBodyConverter implements Converter<ResponseBody, Void> {
        static final VoidResponseBodyConverter INSTANCE = new VoidResponseBodyConverter();

        @Override
        public Void convert(ResponseBody value) {
            return null;
        }
    }

    /**
     * 单例实现Unit响应结果转化类，代表响应结果客户端不需要接收，直接返回 {@link Unit.INSTANCE}
     */
    static final class UnitResponseBodyConverter implements Converter<ResponseBody, Unit> {
        static final UnitResponseBodyConverter INSTANCE = new UnitResponseBodyConverter();

        @Override
        public Unit convert(ResponseBody value) {
            return Unit.INSTANCE;
        }
    }

    /**
     * 单例实现Buffering响应结果转化类，目标结果和转化结果一致。
     */
    static final class BufferingResponseBodyConverter
            implements Converter<ResponseBody, ResponseBody> {
        static final BufferingResponseBodyConverter INSTANCE = new BufferingResponseBodyConverter();

        @Override
        public ResponseBody convert(ResponseBody value) {
            // Buffer the entire body to avoid future I/O.
            return value;
        }
    }

    /**
     * 单例实现String转化类，一般在于将请求配置的参数转化为String，特殊格式还得额外实现。
     */
    public static final class ToStringConverter implements Converter<Object, String> {
        static final ToStringConverter INSTANCE = new ToStringConverter();

        @Override
        public String convert(Object value) {
            return value.toString();
        }
    }
}
