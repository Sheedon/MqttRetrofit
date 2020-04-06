package org.sheedon.mqtt.retrofit.mqtt;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 超时时间 毫秒
 * 例如：
 * @DELAYMILLISECOND(5000)
 * Call<> getManagerList();
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/23 12:47
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface DELAYMILLISECOND {
    int value() default -1;
}
