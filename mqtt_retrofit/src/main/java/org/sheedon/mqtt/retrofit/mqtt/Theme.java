package org.sheedon.mqtt.retrofit.mqtt;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 方法参数内容的主题
 * Call<> getManagerList(@Theme() String topic, @Body UserSubmitModel body);
 * <p>
 * Theme 替换 TOPIC 内容
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 14:48
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Theme {
}
