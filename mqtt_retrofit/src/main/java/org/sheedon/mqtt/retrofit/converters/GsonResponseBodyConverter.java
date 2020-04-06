package org.sheedon.mqtt.retrofit.converters;

import com.google.gson.Gson;

import org.sheedon.mqtt.ResponseBody;
import org.sheedon.mqtt.retrofit.Converter;

import java.lang.reflect.Type;

/**
 * 反馈内容Gson解析转化器
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/24 22:42
 */
final class GsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {

    private final Gson gson;
    private final Type type;

    GsonResponseBodyConverter(Gson gson, Type type) {
        this.gson = gson;
        this.type = type;
    }

    @Override
    public T convert(ResponseBody value) {
        return gson.fromJson(value.getBody(), type);
    }
}
