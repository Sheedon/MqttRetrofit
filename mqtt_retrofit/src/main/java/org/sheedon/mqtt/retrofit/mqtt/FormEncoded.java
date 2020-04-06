package org.sheedon.mqtt.retrofit.mqtt;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 表格编码
 * 例如
 * @FormEncoded
 * Call<> getManagerList(@Field("type") String type,
 *                       @Field("upStartTime") String upStartTime);
 *
 * 要使用 Field，必须设置FormEncoded，才会去解析Field内容
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/25 14:08
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface FormEncoded {
}
