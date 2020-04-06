package org.sheedon.mqtt.retrofit.converters;


import com.google.gson.Gson;

import org.sheedon.mqtt.RequestBody;
import org.sheedon.mqtt.retrofit.Converter;


/**
 * 请求内容Gson转化类
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/24 22:41
 */
public final class GsonRequestBodyConverter<T> implements Converter<T, RequestBody> {

    private final Gson gson;

    GsonRequestBodyConverter(Gson gson) {
        this.gson = gson;
    }

    @Override public RequestBody convert(T value) {
        return new RequestBody().updateData(gson.toJson(value));
    }


}
