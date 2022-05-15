package org.sheedon.mqtt.retrofit;

/**
 * 表单数据转化者，将表单数据转成String类型的数据
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/15 20:43
 */
public interface FormBodyConverter {

    /**
     * 添加表单数据，以name字段为键，value字段为值
     */
    void add(String name, String value);

    /**
     * 将添加的表单数据转化为String类型
     */
    String build();
}
