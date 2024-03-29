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
 */
package org.sheedon.mqtt.retrofit.mqtt;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * MQTT TOPIC
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/22 12:39
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface TOPIC {

    /**
     * topic value as mqtt request topic
     */
    String value() default "";

    /**
     * whether for topic value changes happened
     * if isSplice is false，topic = baseTopic + topic.value
     */
    boolean isSplice() default false;

    /**
     * mqtt quality of service value in the range of 0 to 2
     */
    @QosScope
    int qos() default 0;

    /**
     * Whether the mqtt message is retained,
     * value == true, the request data will be retained in the mqtt service
     */
    boolean retained() default false;
}
