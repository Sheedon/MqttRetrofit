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
