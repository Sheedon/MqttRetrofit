package org.sheedon.mqtt.retrofit.mqtt;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 路径内容,替换Topic中指定的内容
 * @TOPIC("yh_classify/device/recyclable/data/{deviceId}}")
 * Call<> getManagerList(@Path("deviceId") String deviceId, @Field("type") String type,
 *                       @Field("upStartTime") String upStartTime);
 *
 * path中的deviceId替换TOPIC中的deviceId
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 10:35
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Path {
    String value();

    /**
     * Specifies whether the argument value to the annotated method parameter is already URL encoded.
     */
    boolean encoded() default false;
}
