package org.sheedon.mqtt.retrofit;

import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.sheedon.mqtt.Request;
import org.sheedon.mqtt.RequestBody;
import org.sheedon.mqtt.ResponseBody;
import org.sheedon.mqtt.retrofit.mqtt.BACKNAME;
import org.sheedon.mqtt.retrofit.mqtt.Body;
import org.sheedon.mqtt.retrofit.mqtt.DELAYMILLISECOND;
import org.sheedon.mqtt.retrofit.mqtt.Field;
import org.sheedon.mqtt.retrofit.mqtt.FormEncoded;
import org.sheedon.mqtt.retrofit.mqtt.PAYLOAD;
import org.sheedon.mqtt.retrofit.mqtt.Path;
import org.sheedon.mqtt.retrofit.mqtt.QOS;
import org.sheedon.mqtt.retrofit.mqtt.RETAINED;
import org.sheedon.mqtt.retrofit.mqtt.TOPIC;
import org.sheedon.mqtt.retrofit.mqtt.Theme;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 服务方法
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/22 12:51
 */
final class ServiceMethod<R, T> {

    final org.sheedon.mqtt.MQTTFactory mqttFactory;
    final CallAdapter<R, T> callAdapter;

    private final Converter<ResponseBody, R> responseConverter;
    private final ParameterHandler<?>[] parameterHandlers;
    private final MqttMessage mqttMessage;
    private final BindCallback bindCallback;
    private final String topic;
    private final boolean isFormEncoded;
    private final Gson gson;

    ServiceMethod(Builder<R, T> builder) {
        this.mqttFactory = builder.retrofit.mqttFactory();
        this.gson = builder.retrofit.getGson();
        this.callAdapter = builder.callAdapter;
        this.responseConverter = builder.responseConverter;
        this.parameterHandlers = builder.parameterHandlers;
        this.mqttMessage = builder.mqttMessage;
        this.bindCallback = builder.bindCallback;
        this.topic = builder.topic;
        this.isFormEncoded = builder.isFormEncoded;
    }

    Request toRequest(@Nullable Object... args) throws IOException {
        RequestBuilder requestBuilder = new RequestBuilder(topic, mqttMessage,
                bindCallback, isFormEncoded, gson);

        @SuppressWarnings("unchecked") // It is an error to invoke a method with the wrong arg types.
                ParameterHandler<Object>[] handlers = (ParameterHandler<Object>[]) parameterHandlers;

        int argumentCount = args != null ? args.length : 0;
        if (argumentCount != handlers.length) {
            throw new IllegalArgumentException("Argument count (" + argumentCount
                    + ") doesn't match expected count (" + handlers.length + ")");
        }

        for (int p = 0; p < argumentCount; p++) {
            handlers[p].apply(requestBuilder, args[p]);
        }

        return requestBuilder.build();
    }

    T adapt(Call<R> call) {
        return callAdapter.adapt(call);
    }

    T adapt(Observable<R> call) {
        return callAdapter.adapt(call);
    }

    /**
     * Builds a method return value from an HTTP response body.
     */
    R toResponse(ResponseBody body) throws IOException {
        return responseConverter.convert(body);
    }

    static final class Builder<T, R> {
        final Retrofit retrofit;
        final Method method;
        final Annotation[] methodAnnotations;
        final Annotation[][] parameterAnnotationsArray;
        final Type[] parameterTypes;

        String topic;
        boolean isReplace;
        final String baseTopic;
        final MqttMessage mqttMessage;
        final BindCallback bindCallback;

        Type responseType;
        boolean gotPayload;
        boolean gotTheme;
        boolean gotPath;
        boolean gotQuery;
        boolean gotBody;
        boolean isFormEncoded;
        ParameterHandler<?>[] parameterHandlers;
        Converter<ResponseBody, T> responseConverter;
        CallAdapter<T, R> callAdapter;


        /**
         * 构造器，生成初始数据
         *
         * @param retrofit 传入的Retrofit
         * @param method   方法
         */
        public Builder(Retrofit retrofit, Method method) {
            this.retrofit = retrofit;
            this.method = method;
            this.baseTopic = retrofit.baseTopic();
            this.methodAnnotations = method.getAnnotations();
            this.parameterTypes = method.getGenericParameterTypes();
            this.parameterAnnotationsArray = method.getParameterAnnotations();

            this.mqttMessage = new MqttMessage();
            this.bindCallback = new BindCallback();
        }


        public ServiceMethod build() {
            callAdapter = createCallAdapter();
            responseType = callAdapter.responseType();
            if (responseType == Response.class) {
                throw methodError("'"
                        + Utils.getRawType(responseType).getName()
                        + "' is not a valid response body type. Did you mean ResponseBody?");
            }

            responseConverter = createResponseConverter();

            for (Annotation annotation : methodAnnotations) {
                parseMethodAnnotation(annotation);
            }

            int parameterCount = parameterAnnotationsArray.length;
            parameterHandlers = new ParameterHandler[parameterCount];
            for (int p = 0; p < parameterCount; p++) {
                Type parameterType = parameterTypes[p];
                if (Utils.hasUnresolvableType(parameterType)) {
                    throw parameterError(p, "Parameter type must not include a type variable or wildcard: %s",
                            parameterType);
                }

                Annotation[] parameterAnnotations = parameterAnnotationsArray[p];
                if (parameterAnnotations == null) {
                    throw parameterError(p, "No Retrofit annotation found.");
                }

                parameterHandlers[p] = parseParameter(p, parameterType, parameterAnnotations);
            }

            if ((topic == null || topic.equals(""))
                    && (baseTopic == null || baseTopic.isEmpty())) {
                throw methodError("Missing topic.");
            } else if (topic == null || topic.equals("")) {
                topic = baseTopic;
            } else if(!isReplace){
                topic = baseTopic + topic;
            }

            return new ServiceMethod<>(this);
        }

        private CallAdapter<T, R> createCallAdapter() {
            Type returnType = method.getGenericReturnType();
            if (Utils.hasUnresolvableType(returnType)) {
                throw methodError(
                        "Method return type must not include a type variable or wildcard: %s", returnType);
            }
            if (returnType == void.class) {
                throw methodError("Service methods cannot return void.");
            }
            Annotation[] annotations = method.getAnnotations();
            try {
                //noinspection unchecked
                return (CallAdapter<T, R>) retrofit.callAdapter(returnType, annotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
                throw methodError(e, "Unable to create call adapter for %s", returnType);
            }
        }

        private Converter<ResponseBody, T> createResponseConverter() {
            Annotation[] annotations = method.getAnnotations();
            try {
                return retrofit.responseBodyConverter(responseType, annotations);
            } catch (RuntimeException e) { // Wide exception range because factories are user code.
                throw methodError(e, "Unable to create converter for %s", responseType);
            }
        }

        //解析方法注解
        private void parseMethodAnnotation(Annotation annotation) {
            if (annotation instanceof TOPIC) {
                topic = ((TOPIC) annotation).value();
                isReplace = ((TOPIC) annotation).isReplace();
            } else if (annotation instanceof PAYLOAD) {
                gotPayload = true;
                mqttMessage.setPayload(((PAYLOAD) annotation).value().getBytes());
            } else if (annotation instanceof QOS) {
                mqttMessage.setQos(((QOS) annotation).value());
            } else if (annotation instanceof RETAINED) {
                mqttMessage.setRetained(((RETAINED) annotation).value());
            } else if (annotation instanceof DELAYMILLISECOND) {
                bindCallback.setDelayMilliSecond(((DELAYMILLISECOND) annotation).value());
            } else if (annotation instanceof BACKNAME) {
                bindCallback.setBackName(((BACKNAME) annotation).value());
            } else if (annotation instanceof FormEncoded) {
                isFormEncoded = true;
            }
        }


        private ParameterHandler<?> parseParameter(
                int p, Type parameterType, Annotation[] annotations) {
            ParameterHandler<?> result = null;
            for (Annotation annotation : annotations) {
                ParameterHandler<?> annotationAction = parseParameterAnnotation(
                        p, parameterType, annotations, annotation);

                if (annotationAction == null) {
                    continue;
                }

                if (result != null) {
                    throw parameterError(p, "Multiple Retrofit annotations found, only one allowed.");
                }

                result = annotationAction;
            }

            if (result == null) {
                throw parameterError(p, "No Retrofit annotation found.");
            }

            return result;
        }

        private ParameterHandler<?> parseParameterAnnotation(
                int p, Type type, Annotation[] annotations, Annotation annotation) {
            if (annotation instanceof Theme) {
                if (gotTheme) {
                    throw parameterError(p, "Multiple @Theme method annotations found.");
                }

                gotTheme = true;

                if (type == String.class) {
                    return new ParameterHandler.RelativeTopic();
                } else {
                    throw parameterError(p, "@Theme must be String type.");
                }

            } else if (annotation instanceof Path) {
                if (gotQuery) {
                    throw parameterError(p, "A @Path parameter must not come after a @Field.");
                }
                if (gotTheme) {
                    throw parameterError(p, "@Path parameters may not be used with @Theme.");
                }
                gotPath = true;

                Path path = (Path) annotation;
                String name = path.value();
//                validatePathName(p, name);

                Converter<?, String> converter = retrofit.stringConverter(type, annotations);
                return new ParameterHandler.Path<>(name, converter, path.encoded());

            } else if (annotation instanceof Field) {
                if (gotPayload) {
                    throw parameterError(p, "@Field parameters cannot be used with @PAYLOAD.");
                }
                if (gotBody) {
                    throw parameterError(p, "@Field parameters may not be used with @Body.");
                }
                if (!isFormEncoded) {
                    throw parameterError(p, "@Field parameters may be used with @FormEncoded.");
                }

                Field field = (Field) annotation;
                String name = field.value();
                boolean encoded = field.encoded();

                Class<?> rawParameterType = Utils.getRawType(type);
                gotQuery = true;
                if (Iterable.class.isAssignableFrom(rawParameterType)) {
                    if (!(type instanceof ParameterizedType)) {
                        throw parameterError(p, rawParameterType.getSimpleName()
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
                    throw parameterError(p, "@Body parameters cannot be used with @PAYLOAD.");
                }
                if (gotBody) {
                    throw parameterError(p, "@Body method annotations found.");
                }
                if (isFormEncoded) {
                    throw parameterError(p, "@Body parameters cannot be used with @FormEncoded.");
                }

                Converter<?, RequestBody> converter;
                try {
                    converter = retrofit.requestBodyConverter(type, annotations, methodAnnotations);
                } catch (RuntimeException e) {
                    // Wide exception range because factories are user code.
                    throw parameterError(e, p, "Unable to create @Body converter for %s", type);
                }
                gotBody = true;
                return new ParameterHandler.Body<>(converter);
            }

            return null; // Not a Retrofit annotation.
        }


        private RuntimeException methodError(String message, Object... args) {
            return methodError(null, message, args);
        }

        private RuntimeException methodError(Throwable cause, String message, Object... args) {
            message = String.format(message, args);
            return new IllegalArgumentException(message
                    + "\n    for method "
                    + method.getDeclaringClass().getSimpleName()
                    + "."
                    + method.getName(), cause);
        }

        private RuntimeException parameterError(
                Throwable cause, int p, String message, Object... args) {
            return methodError(cause, message + " (parameter #" + (p + 1) + ")", args);
        }

        private RuntimeException parameterError(int p, String message, Object... args) {
            return methodError(message + " (parameter #" + (p + 1) + ")", args);
        }
    }

    static Class<?> boxIfPrimitive(Class<?> type) {
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
