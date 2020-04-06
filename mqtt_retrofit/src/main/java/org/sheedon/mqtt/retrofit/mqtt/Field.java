package org.sheedon.mqtt.retrofit.mqtt;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 区域，用于设置内容的key
 *
 * 例如
 * Call<> getManagerList(@Field("type") String type,
 *                       @Field("upStartTime") String upStartTime);
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/24 8:39
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Field {
    /** The query parameter name. */
    String value();

    /**
     * Specifies whether the parameter {@linkplain #value() name} and value are already URL encoded.
     */
    boolean encoded() default false;
}
