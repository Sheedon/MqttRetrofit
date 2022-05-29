package org.sheedon.mqtt.retrofit.mqtt;

/**
 * Use this annotation on a service method param when you want to change payload encoding format
 * of a publish request.
 * <p>
 * For example:
 *
 * @CHARSET("GBK", autoEncode = false)
 * Call<Model> getManagerList(@Body UserSubmitModel body);
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
     * Whether to set whether to use {@link org.sheedon.mqtt.OkMqttClient.Builder.charsetName(String)}
     * to configure the encoding format of mqtt messages, if {@link value()} is not set,
     * and {@link autoEncode()} is false , the encoding format is not changed.
     */
    boolean autoEncode() default true;
}
