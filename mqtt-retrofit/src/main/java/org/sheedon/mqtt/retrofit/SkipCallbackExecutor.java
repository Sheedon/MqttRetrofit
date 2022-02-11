package org.sheedon.mqtt.retrofit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

/**
 * Change the behavior of a {@code Call<BodyType>} return type to not use the {@linkplain
 * Retrofit#callbackExecutor() callback executor} for invoking the {@link Callback.Call#onResponse(Call,
 * Response) onResponse} or {@link Callback.Call#onFailure(Call, Throwable) onFailure} methods.
 *
 * <pre><code>
 * &#64;SkipCallbackExecutor
 * &#64;GET("user/{id}/token")
 * Call&lt;String&gt; getToken(@Path("id") long id);
 * </code></pre>
 *
 * This annotation can also be used when a {@link CallAdapter.Factory} <em>explicitly</em> delegates
 * to the built-in factory for {@link Call} via {@link Retrofit#nextCallAdapter(CallAdapter.Factory,
 * Type, Annotation[])}  in order for the returned {@link Call} to skip the executor. (Note: by
 * default, a {@link Call} supplied directly to a {@link CallAdapter} will already skip the callback
 * executor. The annotation is only useful when looking up the built-in adapter.)
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/9 7:58 下午
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface SkipCallbackExecutor {}
