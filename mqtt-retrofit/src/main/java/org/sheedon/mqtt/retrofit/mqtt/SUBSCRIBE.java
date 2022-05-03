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

import org.sheedon.mqtt.SubscriptionType;

/**
 * Subscribe topic
 * For example：
 *
 * @BACKTOPIC("xxx") Call<> getManagerList();
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 12:48
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface SUBSCRIBE {

    /**
     * The receiving topic bound to a message feedback operation
     * should not only point to the topic subscribed to by mqtt,
     * but also point to a message type field on the business
     * For example: the request type is test,
     * and the bound request result is test_ack,
     * then the value here should be filled with test_ack
     */
    String value();

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
     * Whether to keep the subscription record,
     * which is used to automatically subscribe to the current topic after mqtt reconnects.
     */
    boolean attachRecord() default false;

    /**
     * Subscription type, defined by {@link SubscriptionType.REMOTE} and {@link SubscriptionType.LOCAL} .
     * default value is {@link SubscriptionType.REMOTE}
     */
    SubscriptionType subscriptionType() default SubscriptionType.REMOTE;
}
