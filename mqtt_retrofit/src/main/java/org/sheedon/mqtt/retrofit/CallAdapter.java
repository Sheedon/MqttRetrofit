package org.sheedon.mqtt.retrofit;


import androidx.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Call 适配器
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/22 17:25
 */
public interface CallAdapter<R, T> {

    Type rawType();

    Type responseType();

    T adapt(Call<R> call);

    T adapt(Observable<R> observable);


    abstract class Factory {

        public abstract @Nullable
        CallAdapter<?, ?> get(Type returnType, Annotation[] annotations,
                              Retrofit retrofit);

        /**
         * Extract the raw class type from {@code type}. For example, the type representing
         * {@code List<? extends Runnable>} returns {@code List.class}.
         */
        static Class<?> getRawType(Type type) {
            return Utils.getRawType(type);
        }
    }
}
