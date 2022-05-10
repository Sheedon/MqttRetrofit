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

import java.util.ArrayList;
import java.util.List;

/**
 * 参数内容主体
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/24 16:33
 */
final class FormBody {

    private final List<String> encodedNames;
    private final List<String> encodedValues;

    private FormBody(List<String> encodedNames, List<String> encodedValues) {
        this.encodedNames = Utils.immutableList(encodedNames);
        this.encodedValues = Utils.immutableList(encodedValues);
    }

    private String convertToString() {
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

        return builder.toString();
    }

    private String convert(String name, String values) {
        return "\"" + name + "\":\"" + values + "\"";
    }


    public static final class Builder {
        private final List<String> names = new ArrayList<>();
        private final List<String> values = new ArrayList<>();

        Builder() {}

        Builder add(String name, String value) {
            if (name == null) throw new NullPointerException("name == null");
            if (value == null) throw new NullPointerException("value == null");

            names.add(name);
            values.add(value);

            return this;
        }

        public String build() {
            return new FormBody(names, values).convertToString();
        }
    }
}
