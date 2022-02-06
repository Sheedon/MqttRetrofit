/*
 * Copyright (C) 2022 Sheedon.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
import java.util.concurrent.TimeUnit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Timeout time, the default unit is seconds, the unit type can be configured
 * For example：
 *
 * @TIMEOUT(5) Call<> getManagerList();
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 12:47
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface TIMEOUT {

    /**
     * The timeout duration.
     * If not configured, the globally configured timeout duration will be used by default.
     */
    long value() default -1;

    /**
     * Timeout unit
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
