package org.sheedon.mqtt.retrofit;

import com.google.gson.Gson;

import org.sheedon.mqtt.RequestBody;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * 参数内容主体
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/24 16:33
 */
final class FormBody extends RequestBody {

    private final List<String> encodedNames;
    private final List<String> encodedValues;

    private Gson gson;

    private FormBody(List<String> encodedNames, List<String> encodedValues, Gson gson) {
        this.encodedNames = Utils.immutableList(encodedNames);
        this.encodedValues = Utils.immutableList(encodedValues);
        this.gson = gson;

        if (this.gson == null) {
            this.gson = new Gson();
        }
        integrationData();
    }

    private void integrationData() {
        StringBuilder builder = new StringBuilder();
        builder.append("{");

        int size = encodedNames.size();
        for (int index = 0; index < size; index++) {
            String name = encodedNames.get(index);
            String values = encodedValues.get(index);
            builder.append(convert(name, values));
            builder.append(",");
        }

        if (builder.length() > 1) {
            builder.deleteCharAt(builder.length() - 1);
        }
        builder.append("}");

        updateData(builder.toString());
    }

    private String convert(String name, String values) {
        return "\"" + name + "\":" + gson.toJson(values);
    }


    public static final class Builder {
        private final List<String> names = new ArrayList<>();
        private final List<String> values = new ArrayList<>();
        private final Charset charset;
        private Gson gson;

        Builder() {
            this(null);
        }

        Builder(Charset charset) {
            this.charset = charset;
        }

        Builder add(String name, String value) {
            if (name == null) throw new NullPointerException("name == null");
            if (value == null) throw new NullPointerException("value == null");

            names.add(name);
            values.add(value);

//            names.add(HttpUrl.canonicalize(name, FORM_ENCODE_SET, false, false, true, true, charset));
//            values.add(HttpUrl.canonicalize(value, FORM_ENCODE_SET, false, false, true, true, charset));
            return this;
        }

        Builder bindGson(Gson gson) {
            if (gson == null) throw new NullPointerException("gson == null");
            this.gson = gson;
            return this;
        }

        public FormBody build() {
            return new FormBody(names, values, gson);
        }

        Builder addEncoded(String name, String value) {
            return add(name, value);
        }
    }
}
