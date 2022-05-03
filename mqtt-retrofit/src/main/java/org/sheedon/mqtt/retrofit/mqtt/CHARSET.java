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
     * Encoding charset format name
     */
    String value() default "";

    /**
     * Whether the messaging engine should keep publish messages.
     * Sending a message with reserved set to `true` and using an empty byte array as payload,
     * e.g. `new byte[0]` will clear the reserved message from the server.
     * The default value is false.
     */
    boolean autoEncode() default false;
}
