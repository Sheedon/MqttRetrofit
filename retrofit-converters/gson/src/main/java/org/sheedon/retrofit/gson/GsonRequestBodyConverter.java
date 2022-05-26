package org.sheedon.retrofit.gson;

import com.google.gson.TypeAdapter;

import org.sheedon.mqtt.retrofit.Converter;

/**
 * gson请求body构造转换器
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/24 22:44
 */
public class GsonRequestBodyConverter<T> implements Converter<T, String> {

    private final TypeAdapter<T> adapter;

    public GsonRequestBodyConverter(TypeAdapter<T> adapter) {
        this.adapter = adapter;
    }

    @Override
    public String convert(T value) {
        return adapter.toJson(value);
    }
}
