package org.sheedon.mqtt.retrofit.mqtt;

/**
 * Encoding format
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/7 6:48 下午
 */
public @interface CHARSET {

    /**
     * Encoding format name
     */
    String value() default "";

    /**
     * Whether to enable the current encoding format
     */
    boolean encoded() default false;
}
