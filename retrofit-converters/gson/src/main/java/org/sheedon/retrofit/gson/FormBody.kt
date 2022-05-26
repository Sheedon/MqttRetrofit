package org.sheedon.retrofit.gson

import org.sheedon.mqtt.retrofit.FormBodyConverter

/**
 * 参数内容主体
 * 基础参数，格式：{"name":"value","name1":"value1"}
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/24 22:02
 */
class FormBody(
    private val encodedNames: List<String> = ArrayList(),
    private val encodedValues: List<String> = ArrayList()
) {


    private fun convertToString(): String {
        val builder = StringBuilder()
        builder.append("{")

        val size = encodedNames.size
        for (index in 0 until size) {
            val name = encodedNames[index]
            val values = encodedValues[index]
            builder.append(convert(name, values))
            builder.append(",")
        }

        if (builder.length > 1) {
            builder.deleteCharAt(builder.length - 1)
        }
        builder.append("}")

        return builder.toString()
    }

    private fun convert(name: String, values: String): String {
        return "\"$name\":$values"
    }

    class Builder : FormBodyConverter {
        private val names: MutableList<String> = java.util.ArrayList()
        private val values: MutableList<String> = java.util.ArrayList()
        override fun add(name: String, value: String) {
            names.add(name)
            values.add(value)
        }

        override fun build(): String {
            return FormBody(names, values).convertToString()
        }

        override fun clone() = Builder()
    }

}