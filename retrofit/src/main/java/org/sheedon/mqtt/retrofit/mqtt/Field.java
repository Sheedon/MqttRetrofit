/*
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
package org.sheedon.mqtt.retrofit.mqtt;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Named pair for a form-encoded request.
 *
 * <p>Values are converted to strings using {@link Retrofit#stringConverter(Type, Annotation[])} (or
 * {@link Object#toString()}, if no matching string converter is installed) and then form payload
 * encoded. {@code null} values are ignored. Passing a {@link java.util.List List} or array will
 * result in a field pair for each non-{@code null} item.
 *
 * <p>Simple Example:
 *
 * <pre><code>
 * &#64;FormUrlEncoded
 * &#64;TOPIC("user/test")
 * Call&lt;ResponseBody&gt; example(
 *     &#64;Field("name") String name,
 *     &#64;Field("occupation") String occupation);
 * </code></pre>
 * <p>
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/24 8:39
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Field {

    /**
     * The query parameter name.
     */
    String value();
}
