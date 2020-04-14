package org.sheedon.mqtt.retrofit;

/**
 * 转化反馈内容
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/22 17:30
 */
public interface Callback<T> {

    interface Call<T>{
        void onResponse(org.sheedon.mqtt.retrofit.Call<T> call, Response<T> response);

        /**
         * Invoked when a network exception occurred talking to the server or when an unexpected
         * exception occurred creating the request or processing the response.
         */
        void onFailure(org.sheedon.mqtt.retrofit.Call<T> call, Throwable t);
    }

    interface Observable<T>{
        void onResponse(org.sheedon.mqtt.retrofit.Observable<T> call, Response<T> response);

        /**
         * Invoked when a network exception occurred talking to the server or when an unexpected
         * exception occurred creating the request or processing the response.
         */
        void onFailure(org.sheedon.mqtt.retrofit.Observable<T> call, Throwable t);
    }
}
