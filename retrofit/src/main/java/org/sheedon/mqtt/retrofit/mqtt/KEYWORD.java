package org.sheedon.mqtt.retrofit.mqtt;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Sets the keyword of the response message
 * <p>
 * If the field is not "", it means that the correlation field is used as the matching field
 * of the response message for correlation; otherwise, the subscription topic in the relation is taken.
 * <p>
 * Note: It is hoped that in the case of subscribing to the same topic, the short-term
 * request will use the topic uniformly, or use the keyword instead of mixing
 * (sometimes there is no keyword). In this case, there is a response error.
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/3 22:49
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface KEYWORD {

    /**
     * message keyword content，example: value = "type"
     */
    String value() default "";
}
