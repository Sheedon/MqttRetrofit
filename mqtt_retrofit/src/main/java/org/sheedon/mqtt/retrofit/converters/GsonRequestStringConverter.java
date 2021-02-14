/**
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
package org.sheedon.mqtt.retrofit.converters;


import com.google.gson.Gson;


import org.sheedon.mqtt.retrofit.Converter;


/**
 * 请求实体类通过Gson转化为String类型
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/24 22:41
 */
public final class GsonRequestStringConverter<T> implements Converter<T, String> {

    private final Gson gson;

    GsonRequestStringConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String convert(T value) {
        return gson.toJson(value);
    }


}
