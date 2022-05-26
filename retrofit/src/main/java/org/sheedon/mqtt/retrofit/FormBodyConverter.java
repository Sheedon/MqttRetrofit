/*
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
 * */
package org.sheedon.mqtt.retrofit;

/**
 * 表单数据转化者，将表单数据转成String类型的数据
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/15 20:43
 */
public interface FormBodyConverter {

    /**
     * 添加表单数据，以name字段为键，value字段为值
     */
    void add(String name, String value);

    /**
     * 将添加的表单数据转化为String类型
     */
    String build();


    /**
     * 克隆出一个自己
     * @return 表单数据转化者
     */
    FormBodyConverter clone();
}
