package org.sheedon.mqtt.retrofit.mqtt;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntRange;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

/**
 * Define where {@link Path} replaces value, including TOPIC/SUBSCRIBE/PAYLOAD/KEYWORD.
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/27 5:25 下午
 */
@Documented
@Retention(SOURCE)
@IntRange(from = PathType.TYPE_MIN, to = PathType.TYPE_MAX)
public @interface PathType {

    // 请求主题路径
    int TOPIC = 0;
    // 订阅主题路径
    int SUBSCRIBE = 1;
    // 有效载荷路径
    int PAYLOAD = 2;
    // 关键字路径
    int KEYWORD = 3;


    // 最小类型
    int TYPE_MIN = TOPIC;
    // 最大类型
    int TYPE_MAX = KEYWORD;
}
