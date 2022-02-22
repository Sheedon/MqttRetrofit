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

import android.annotation.TargetApi;
import android.os.Build;

import androidx.annotation.Nullable;

import org.sheedon.mqtt.ResponseBody;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * 可选转换器工厂
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/7 11:26 上午
 */
@TargetApi(Build.VERSION_CODES.N)
final class OptionalConverterFactory extends Converter.Factory{
    @Override
    public @Nullable
    Converter<ResponseBody, ?> responseBodyConverter(
            Type type, Annotation[] annotations, Retrofit retrofit) {
        if (getRawType(type) != Optional.class) {
            return null;
        }

        Type innerType = getParameterUpperBound(0, (ParameterizedType) type);
        Converter<ResponseBody, Object> delegate =
                retrofit.responseBodyConverter(innerType, annotations);
        return new OptionalConverter<>(delegate);
    }

    static final class OptionalConverter<T> implements Converter<ResponseBody, Optional<T>> {
        final Converter<ResponseBody, T> delegate;

        OptionalConverter(Converter<ResponseBody, T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Optional<T> convert(ResponseBody value) throws IOException {
            return Optional.ofNullable(delegate.convert(value));
        }
    }
}
