package org.sheedon.mqtt.retrofit.mqtt;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * MQTT 主题
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/22 12:39
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface TOPIC {

    String value() default "";

    boolean isReplace() default false;
}
