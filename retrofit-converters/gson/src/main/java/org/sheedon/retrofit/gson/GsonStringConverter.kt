package org.sheedon.retrofit.gson

import com.google.gson.TypeAdapter
import org.sheedon.mqtt.retrofit.Converter

/**
 * gson请求string构造转换器
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/24 22:44
 */
class GsonStringConverter<T>(private val adapter: TypeAdapter<T>) : Converter<T, String> {
    override fun convert(value: T): String {
        return if (value is String) {
            value
        } else adapter.toJson(value)
    }
}