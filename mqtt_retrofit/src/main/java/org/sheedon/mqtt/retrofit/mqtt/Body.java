package org.sheedon.mqtt.retrofit.mqtt;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 请求内容
 * 例如 Call<> getManagerList(@Body UserSubmitModel body);
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/24 10:54
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Body {
}
