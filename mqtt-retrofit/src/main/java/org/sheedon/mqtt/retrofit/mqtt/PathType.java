package org.sheedon.mqtt.retrofit.mqtt;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntRange;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

/**
 * 参数类型
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/27 5:25 下午
 */
@Documented
@Retention(SOURCE)
@IntRange(from = PathType.TOPIC,to = PathType.PAYLOAD)
public @interface PathType {

    int TOPIC = 0;
    int BACK_TOPIC = 1;
    int PAYLOAD = 2;
}
