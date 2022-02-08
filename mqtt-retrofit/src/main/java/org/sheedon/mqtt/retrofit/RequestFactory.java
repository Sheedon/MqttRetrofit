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
package org.sheedon.mqtt.retrofit;

import androidx.annotation.Nullable;

import org.sheedon.mqtt.Request;
import org.sheedon.mqtt.retrofit.mqtt.BACKTOPIC;
import org.sheedon.mqtt.retrofit.mqtt.Body;
import org.sheedon.mqtt.retrofit.mqtt.CHARSET;
import org.sheedon.mqtt.retrofit.mqtt.Field;
import org.sheedon.mqtt.retrofit.mqtt.FormEncoded;
import org.sheedon.mqtt.retrofit.mqtt.PAYLOAD;
import org.sheedon.mqtt.retrofit.mqtt.Path;
import org.sheedon.mqtt.retrofit.mqtt.QOS;
import org.sheedon.mqtt.retrofit.mqtt.RETAINED;
import org.sheedon.mqtt.retrofit.mqtt.TIMEOUT;
import org.sheedon.mqtt.retrofit.mqtt.TOPIC;
import org.sheedon.mqtt.retrofit.mqtt.Theme;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import kotlin.coroutines.Continuation;

/**
 * 请求数据存储工厂类
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/7 4:19 下午
 */
final class RequestFactory {
    static RequestFactory parseAnnotations(Retrofit retrofit, Method method) {
        return new Builder(retrofit, method).build();
    }

    private final Method method;
    private final boolean isFormEncoded;
    private final ParameterHandler<?>[] parameterHandlers;
    final boolean isKotlinSuspendFunction;

    private final String topic;
    private final String relativePayload;
    private final String charset;
    private final int qos;
    private final boolean retained;
    private final long timeout;
    private final TimeUnit timeUnit;
    private final String backTopic;

    RequestFactory(Builder builder) {
        method = builder.method;
        isFormEncoded = builder.isFormEncoded;
        parameterHandlers = builder.parameterHandlers;
        isKotlinSuspendFunction = builder.isKotlinSuspendFunction;
        topic = builder.topic;
        relativePayload = builder.relativePayload;
        if (builder.isCharsetEncoded) {
            charset = builder.charset;
        } else {
            charset = "";
        }
        qos = builder.qos;
        retained = builder.retained;
        timeout = builder.timeout;
        timeUnit = builder.timeUnit;
        backTopic = builder.backTopic;
    }

    Request create(Object[] args) throws IOException {
        ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;

        int argumentCount = args.length;
        if (argumentCount != handlers.length) {
            throw new IllegalArgumentException(
                    "Argument count ("
                            + argumentCount
                            + ") doesn't match expected count ("
                            + handlers.length
                            + ")");
        }

        RequestBuilder requestBuilder =
                new RequestBuilder(topic, relativePayload, charset, qos, retained,
                        timeout, timeUnit, backTopic, isFormEncoded);

        if (isKotlinSuspendFunction) {
            // The Continuation is the last parameter and the handlers array contains null at that index.
            argumentCount--;
        }

        for (int p = 0; p < argumentCount; p++) {
            handlers[p].apply(requestBuilder, args[p]);
        }

        return requestBuilder.get().build();
    }

    static final class Builder {

        final Retrofit retrofit;
        final Method method;
        final Annotation[] methodAnnotations;
        final Annotation[][] parameterAnnotationsArray;
        final Type[] parameterTypes;

        String topic = "";
        boolean isReplace;
        boolean gotTheme;

        String relativePayload = "";
        boolean gotPayload;
        String charset;
        boolean isCharsetEncoded = false;

        int qos = 0;
        boolean retained = false;

        long timeout;
        TimeUnit timeUnit = TimeUnit.SECONDS;
        String backTopic;

        boolean gotPath;
        boolean gotQuery;
        boolean gotBody;
        boolean isFormEncoded;
        @Nullable
        ParameterHandler<?>[] parameterHandlers;
        boolean isKotlinSuspendFunction;

        Builder(Retrofit retrofit, Method method) {
            this.retrofit = retrofit;
            this.method = method;
            this.methodAnnotations = method.getAnnotations();
            this.parameterTypes = method.getGenericParameterTypes();
            this.parameterAnnotationsArray = method.getParameterAnnotations();
            this.timeout = retrofit.timeout <= 0 ? 3 : retrofit.timeout;
        }


        RequestFactory build() {
            for (Annotation annotation : methodAnnotations) {
                parseMethodAnnotation(annotation);
            }

            int parameterCount = parameterAnnotationsArray.length;
            parameterHandlers = new ParameterHandler<?>[parameterCount];
            for (int p = 0, lastParameter = parameterCount - 1; p < parameterCount; p++) {
                parameterHandlers[p] =
                        parseParameter(p, parameterTypes[p], parameterAnnotationsArray[p], p == lastParameter);
            }

            String baseTopic = retrofit.baseTopic;
            if ((topic == null || topic.equals(""))
                    && (baseTopic == null || baseTopic.isEmpty())) {
                throw Utils.methodError(method, "Missing topic.");
            } else if (topic == null || topic.equals("")) {
                topic = baseTopic;
            } else if (!isReplace) {
                topic = baseTopic + topic;
            }

            return new RequestFactory(this);
        }

        //解析方法注解
        private void parseMethodAnnotation(Annotation annotation) {
            if (annotation instanceof TOPIC) {
                topic = ((TOPIC) annotation).value();
                isReplace = ((TOPIC) annotation).isSplice();
            } else if (annotation instanceof PAYLOAD) {
                gotPayload = true;
                relativePayload = ((PAYLOAD) annotation).value();
            } else if (annotation instanceof CHARSET) {
                charset = ((CHARSET) annotation).value();
                isCharsetEncoded = ((CHARSET) annotation).encoded();
            } else if (annotation instanceof QOS) {
                qos = ((QOS) annotation).value();
            } else if (annotation instanceof RETAINED) {
                retained = ((RETAINED) annotation).value();
            } else if (annotation instanceof TIMEOUT) {
                timeout = ((TIMEOUT) annotation).value();
                timeUnit = ((TIMEOUT) annotation).unit();
            } else if (annotation instanceof BACKTOPIC) {
                backTopic = ((BACKTOPIC) annotation).value();
            } else if (annotation instanceof FormEncoded) {
                isFormEncoded = true;
            }
        }

        private @Nullable
        ParameterHandler<?> parseParameter(
                int p, Type parameterType, @Nullable Annotation[] annotations, boolean allowContinuation) {
            ParameterHandler<?> result = null;
            if (annotations != null) {
                for (Annotation annotation : annotations) {
                    ParameterHandler<?> annotationAction =
                            parseParameterAnnotation(p, parameterType, annotations, annotation);

                    if (annotationAction == null) {
                        continue;
                    }

                    if (result != null) {
                        throw Utils.parameterError(
                                method, p, "Multiple Retrofit annotations found, only one allowed.");
                    }

                    result = annotationAction;
                }
            }

            if (result == null) {
                if (allowContinuation) {
                    try {
                        if (Utils.getRawType(parameterType) == Continuation.class) {
                            isKotlinSuspendFunction = true;
                            return null;
                        }
                    } catch (NoClassDefFoundError ignored) {
                        // Ignored
                    }
                }
                throw Utils.parameterError(method, p, "No Retrofit annotation found.");
            }

            return result;
        }

        @Nullable
        private ParameterHandler<?> parseParameterAnnotation(
                int p, Type type, Annotation[] annotations, Annotation annotation) {
            if (annotation instanceof Theme) {
                validateResolvableType(p, type);
                if (gotTheme) {
                    throw Utils.parameterError(method, p, "Multiple @Theme method annotations found.");
                }

                gotTheme = true;

                if (type == String.class) {
                    return new ParameterHandler.RelativeTopic();
                } else {
                    throw Utils.parameterError(method, p, "@Theme must be String type.");
                }

            } else if (annotation instanceof Path) {
                validateResolvableType(p, type);
                if (gotQuery) {
                    throw Utils.parameterError(method, p, "A @Path parameter must not come after a @Field.");
                }
                if (gotTheme) {
                    throw Utils.parameterError(method, p, "@Path parameters may not be used with @Theme.");
                }
                gotPath = true;

                Path path = (Path) annotation;
                String name = path.value();

                Converter<?, String> converter = retrofit.stringConverter(type, annotations);
                return new ParameterHandler.Path<>(name, converter, path.encoded());

            } else if (annotation instanceof Field) {
                if (gotPayload) {
                    throw Utils.parameterError(method, p, "@Field parameters cannot be used with @PAYLOAD.");
                }
                if (gotBody) {
                    throw Utils.parameterError(method, p, "@Field parameters may not be used with @Body.");
                }
                if (!isFormEncoded) {
                    throw Utils.parameterError(method, p, "@Field parameters may be used with @FormEncoded.");
                }

                Field field = (Field) annotation;
                String name = field.value();
                boolean encoded = field.encoded();

                Class<?> rawParameterType = Utils.getRawType(type);
                gotQuery = true;
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw Utils.parameterError(method, p, rawParameterType.getSimpleName()
                                + " must include generic type (e.g., "
                                + rawParameterType.getSimpleName()
                                + "<String>)");
                    }
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    Type iterableType = Utils.getParameterUpperBound(0, parameterizedType);
                    Converter<?, String> converter =
                            retrofit.stringConverter(iterableType, annotations);
                    return new ParameterHandler.Field<>(name, converter, encoded).iterable();
                } else if (rawParameterType.isArray()) {
                    Class<?> arrayComponentType = boxIfPrimitive(rawParameterType.getComponentType());
                    Converter<?, String> converter =
                            retrofit.stringConverter(arrayComponentType, annotations);
                    return new ParameterHandler.Field<>(name, converter, encoded).array();
                } else {
                    Converter<?, String> converter =
                            retrofit.stringConverter(type, annotations);
                    return new ParameterHandler.Field<>(name, converter, encoded);
                }

            } else if (annotation instanceof Body) {
                if (gotPayload) {
                    throw Utils.parameterError(method, p, "@Body parameters cannot be used with @PAYLOAD.");
                }
                if (gotBody) {
                    throw Utils.parameterError(method, p, "@Body method annotations found.");
                }
                if (isFormEncoded) {
                    throw Utils.parameterError(method, p, "@Body parameters cannot be used with @FormEncoded.");
                }

                Converter<?, String> converter;
                try {
                    converter = retrofit.requestBodyConverter(type, annotations, methodAnnotations);
                } catch (RuntimeException e) {
                    // Wide exception range because factories are user code.
                    throw Utils.parameterError(method, e, p, "Unable to create @Body converter for %s", type);
                }
                gotBody = true;
                return new ParameterHandler.Body<>(converter);
            }

            return null; // Not a Retrofit annotation.
        }

        private void validateResolvableType(int p, Type type) {
            if (Utils.hasUnresolvableType(type)) {
                throw Utils.parameterError(
                        method, p, "Parameter type must not include a type variable or wildcard: %s", type);
            }
        }

        private static Class<?> boxIfPrimitive(Class<?> type) {
            if (boolean.class == type) return Boolean.class;
            if (byte.class == type) return Byte.class;
            if (char.class == type) return Character.class;
            if (double.class == type) return Double.class;
            if (float.class == type) return Float.class;
            if (int.class == type) return Integer.class;
            if (long.class == type) return Long.class;
            if (short.class == type) return Short.class;
            return type;
        }
    }
}
