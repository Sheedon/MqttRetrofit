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

import org.sheedon.mqtt.ResponseBody;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 将对象与其在 MQTT 中的表示形式相互转换。
 * 实例由 {@linkplain Factory a factory} 创建，{@linkplain Retrofit.BuilderaddConverterFactory(Factory)
 * installed} 到 {@link Retrofit} 实例中。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 10:00
 */
public interface Converter<F, T> {
    /**
     * 将【类型为F的数据】转化为【类型为T的结果】
     *
     * @param value 需要转化的对象
     * @return 转化后的结果
     * @throws IOException
     */
    T convert(F value) throws IOException;

    /**
     * 根据类型和目标使用情况创建 {@link Converter} 实例。
     */
    abstract class Factory {
        /**
         * 这用于从 {@code Call<SimpleResponse>} 声明创建响应类型的转换器，返回用于将 MQTT 响应正文转换为
         * {@code type} 的 {@link Converter}。
         * <p>
         * 如果此工厂无法处理 {@code type}，则返回 null。
         */
        public @Nullable
        Converter<ResponseBody, ?> responseBodyConverter(Type type,
                                                         Annotation[] annotations, Retrofit retrofit) {
            return null;
        }

        /**
         * 返回用于将 {@code type} 转换为 MQTT 请求正文的 {@link Converter}，
         * 如果此工厂无法处理 {@code type}，则返回 null。
         * <p>
         * 这用于为 {@link Body @Body} 值指定的类型创建转换器。
         */
        public @Nullable
        Converter<?, String> requestBodyConverter(Type type,
                                                  Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
            return null;
        }


        /**
         * 返回用于将 {@code type} 转换为 {@link String} 的 {@link Converter}，
         * 如果此工厂无法处理 {@code type}，则返回 null。
         * <p>
         * 这用于为 {@link Field @Field}、{@link Path @Path} 指定的类型创建转换器。
         */
        public @Nullable
        Converter<?, String> stringConverter(Type type, Annotation[] annotations,
                                             Retrofit retrofit) {
            return null;
        }

        /**
         * 表单数据转化者，用于使用 {@link @Field}是配置表单数据
         */
        public @Nullable
        FormBodyConverter formBodyConverter() {
            return null;
        }

        /**
         * 从 {@code type} 中提取 {@code index} 的泛型参数的上限。
         * 例如，{@code Map<String, ? extends Runnable>} 返回 {@code Runnable}。
         */
        protected static Type getParameterUpperBound(int index, ParameterizedType type) {
            return Utils.getParameterUpperBound(index, type);
        }

        /**
         * 从 {@code type} 中提取原始类类型。
         * 例如，表示 {@code List<? extends Runnable>} 返回 {@code List.class}。
         */
        protected static Class<?> getRawType(Type type) {
            return Utils.getRawType(type);
        }


    }
}
