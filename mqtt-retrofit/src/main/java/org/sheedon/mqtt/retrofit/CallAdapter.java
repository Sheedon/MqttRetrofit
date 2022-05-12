/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (C) 2020 Sheedon.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sheedon.mqtt.retrofit;


import androidx.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


/**
 * 将响应类型为 {@code R} 的 {@link Call} 调整为 {@code T} 的类型。
 * 实例由 {@linkplain Factory a factory} 创建，在{@link Retrofit}中
 * 通过{@linkplain Retrofit.BuilderaddCallAdapterFactory(Factory) installed}创建。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/22 17:25
 */
public interface CallAdapter<R, T> {

    /**
     * 返回原始类类型，用来区分是否为{@link Observable}
     * 例如，{@code Call<Repo>} 的响应类型是 {@code Call},
     * {@code Observable<Repo>} 的响应类型是 {@code Observable}。
     * <p>
     * Call，一般用来作为请求响应处理的反馈接收，而Observable更侧重于作为订阅的观察者。
     */
    Type rawType();

    /**
     * 返回此适配器在将 MQTT 响应结果转换为 Java 对象时使用的值类型。
     * 例如，{@code Call<Repo>} 的响应类型是 {@code Repo}。此类型用于准备传递给 {@code adapt} 的 {@code call}。
     * <p>
     * 注意：这通常与提供给此调用适配器工厂的 {@code returnType} 类型不同。
     */
    Type responseType();

    /**
     * 通过动态代理，将{@code call}委托执行的结果 {@code T} 实例返回。
     * <p>
     * 例如，给定一个实用程序 {@code Async} 的实例，该实例将返回一个新的 {@code Async<R>}，
     * 它在运行时调用了 {@code call}。
     *
     * <pre><code>
     * &#64;Override
     * public &lt;R&gt; Async&lt;R&gt; adapt(final Call&lt;R&gt; call) {
     *   return Async.create(new Callable&lt;Response&lt;R&gt;&gt;() {
     *     &#64;Override
     *     public Response&lt;R&gt; call() throws Exception {
     *       return call.execute();
     *     }
     *   });
     * }
     * </code></pre>
     */
    T adapt(Call<R> call);

    /**
     * 通过动态代理，将{@code observable}委托执行的结果 {@code T} 实例返回。
     * <p>
     * 例如，给定一个实用程序 {@code Async} 的实例，该实例将返回一个新的 {@code Async<R>}，
     * 它在运行时调用了 {@code observable}。
     *
     * <pre><code>
     * &#64;Override
     * public &lt;R&gt; Async&lt;R&gt; adapt(final Observable&lt;R&gt; observable) {
     *   return Async.create(new Callable&lt;Response&lt;R&gt;&gt;() {
     *     &#64;Override
     *     public Response&lt;R&gt; observable() throws Exception {
     *       return observable.execute();
     *     }
     *   });
     * }
     * </code></pre>
     */
    T adapt(Observable<R> observable);


    /**
     * 根据服务接口方法的返回类型创建CallAdapter实例
     */
    abstract class Factory {

        /**
         * 获取{@code returnType} 的接口方法的调用适配器，如果此工厂无法处理，则返回 null。
         */
        public abstract @Nullable
        CallAdapter<?, ?> get(Type returnType, Annotation[] annotations,
                              Retrofit retrofit);

        /**
         * 从 {@code type} 中提取 {@code index} 的泛型参数的上限。
         * 例如，{@code Map<String, ? extends Runnable>} 返回 {@code Runnable}。
         */
        protected static Type getParameterUpperBound(int index, ParameterizedType type) {
            return Utils.getParameterUpperBound(index, type);
        }

        /**
         * 从 {@code type} 中提取原始类类型。
         * 例如，表示 {@code List<? extends Runnable>} 返回 {@code List.class}。
         */
        static Class<?> getRawType(Type type) {
            return Utils.getRawType(type);
        }
    }
}
