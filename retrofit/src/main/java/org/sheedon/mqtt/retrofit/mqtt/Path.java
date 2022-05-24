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

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Path content, replace the content specified in TOPIC
 *
 * @TOPIC("yh_classify/device/recyclable/data/{deviceId}}") Call<> getManagerList(@Path("deviceId") String deviceId, @Field("type") String type,
 * @Field("upStartTime") String upStartTime);
 * <p>
 * The deviceId in path replaces the deviceId in TOPIC
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 10:35
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Path {

    /**
     * Configure path type.
     * {@link PathType.TOPIC}: replace the associated fields in {@link TOPIC}
     * {@link PathType.SUBSCRIBE}: replace the associated fields in {@link SUBSCRIBE}
     * {@link PathType.PAYLOAD}: replace the associated fields in {@link PAYLOAD}
     * {@link PathType.KEYWORD}: replace the associated fields in {@link KEYWORD}
     */
    @PathType int type() default PathType.TOPIC;

    /**
     * Used to replace the field corresponding to the current value in TOPIC
     */
    String value();
}
