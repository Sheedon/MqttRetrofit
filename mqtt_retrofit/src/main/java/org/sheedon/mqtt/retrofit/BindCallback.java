package org.sheedon.mqtt.retrofit;

/**
 * 反馈绑定数据
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/25 13:23
 */
class BindCallback {

    private int delayMilliSecond = -1;
    private String backName;

    int getDelayMilliSecond() {
        return delayMilliSecond;
    }

    void setDelayMilliSecond(int delayMilliSecond) {
        this.delayMilliSecond = delayMilliSecond;
    }

    String getBackName() {
        return backName;
    }

    void setBackName(String backName) {
        this.backName = backName;
    }
}
