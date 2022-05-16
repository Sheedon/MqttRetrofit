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

import org.sheedon.mqtt.ResponseBody;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import kotlin.coroutines.Continuation;

/**
 * 将接口方法的调用适配为 Mqtt Call 或 Mqtt Observable。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/6 4:24 下午
 */
abstract class MqttServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {

    /**
     * 检查接口方法上的注释以构造一个可重用的服务方法，该方法使用 MQTT。
     * 这需要潜在的昂贵反射，因此最好只构建每个服务方法一次并重用它。
     */
    static <ResponseT, ReturnT> MqttServiceMethod<ResponseT, ReturnT> parseAnnotations(
            Retrofit retrofit, Method method, RequestFactory requestFactory) {
        boolean isKotlinSuspendFunction = requestFactory.isKotlinSuspendFunction;
        boolean continuationWantsResponse = false;
        boolean continuationBodyNullable = false;

        Annotation[] annotations = method.getAnnotations();
        Type adapterType;
        if (isKotlinSuspendFunction) {
            Type[] parameterTypes = method.getGenericParameterTypes();
            Type responseType =
                    Utils.getParameterLowerBound(
                            0, (ParameterizedType) parameterTypes[parameterTypes.length - 1]);
            if (Utils.getRawType(responseType) == Response.class && responseType instanceof ParameterizedType) {
                // Unwrap the actual body type from Response<T>.
                responseType = Utils.getParameterUpperBound(0, (ParameterizedType) responseType);
                continuationWantsResponse = true;
            } else {
                // TODO figure out if type is nullable or not
                // Metadata metadata = method.getDeclaringClass().getAnnotation(Metadata.class)
                // Find the entry for method
                // Determine if return type is nullable or not
            }

            adapterType = new Utils.ParameterizedTypeImpl(null, Call.class, responseType);
            annotations = SkipCallbackExecutorImpl.ensurePresent(annotations);
        } else {
            adapterType = method.getGenericReturnType();
        }

        CallAdapter<ResponseT, ReturnT> callAdapter =
                createCallAdapter(retrofit, method, adapterType, annotations);
        Type responseType = callAdapter.responseType();
        if (responseType == org.sheedon.mqtt.Response.class) {
            throw Utils.methodError(
                    method,
                    "'"
                            + Utils.getRawType(responseType).getName()
                            + "' is not a valid response body type. Did you mean ResponseBody?");
        }
        if (responseType == Response.class) {
            throw Utils.methodError(method, "Response must include generic type (e.g., Response<String>)");
        }

        Converter<ResponseBody, ResponseT> responseConverter =
                createResponseConverter(retrofit, method, responseType);

        org.sheedon.mqtt.CallFactory callFactory = retrofit.callFactory;
        if (!isKotlinSuspendFunction) {

            if (callAdapter.rawType() == Observable.class) {

                return new ObservableAdapted<>(requestFactory, responseConverter,
                        retrofit.observableFactory, callAdapter);
            }

            return new CallAdapted<>(requestFactory, callFactory, responseConverter, callAdapter);
        } else if (continuationWantsResponse) {
            //noinspection unchecked Kotlin compiler guarantees ReturnT to be Object.
            return (MqttServiceMethod<ResponseT, ReturnT>)
                    new SuspendForResponse<>(
                            requestFactory,
                            callFactory,
                            responseConverter,
                            (CallAdapter<ResponseT, Call<ResponseT>>) callAdapter);
        } else {
            //noinspection unchecked Kotlin compiler guarantees ReturnT to be Object.
            return (MqttServiceMethod<ResponseT, ReturnT>)
                    new SuspendForBody<>(
                            requestFactory,
                            callFactory,
                            responseConverter,
                            (CallAdapter<ResponseT, Call<ResponseT>>) callAdapter,
                            continuationBodyNullable);
        }
    }

    /**
     * 通过retrofit，根据「returnType」和「annotations」创建CallAdapter
     *
     * @param retrofit    Retrofit
     * @param method      方法类，用于打印错误
     * @param returnType  返回类型
     * @param annotations 方法注解
     * @param <ResponseT> 响应类型
     * @param <ReturnT>   反馈类型
     * @return CallAdapter
     */
    private static <ResponseT, ReturnT> CallAdapter<ResponseT, ReturnT> createCallAdapter(
            Retrofit retrofit, Method method, Type returnType, Annotation[] annotations) {
        try {
            //noinspection unchecked
            return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter(returnType, annotations);
        } catch (RuntimeException e) { // Wide exception range because factories are user code.
            throw Utils.methodError(method, e, "Unable to create call adapter for %s", returnType);
        }
    }

    /**
     * 创建响应转化器
     *
     * @param retrofit     Retrofit
     * @param method       方法，用于获取注解
     * @param responseType 响应类型
     * @param <ResponseT>  响应类型
     * @return Converter<ResponseBody, ResponseT>
     */
    private static <ResponseT> Converter<ResponseBody, ResponseT> createResponseConverter(
            Retrofit retrofit, Method method, Type responseType) {
        Annotation[] annotations = method.getAnnotations();
        try {
            return retrofit.responseBodyConverter(responseType, annotations);
        } catch (RuntimeException e) { // Wide exception range because factories are user code.
            throw Utils.methodError(method, e, "Unable to create converter for %s", responseType);
        }
    }

    private final RequestFactory requestFactory;
    private final org.sheedon.mqtt.CallFactory callFactory;
    private final org.sheedon.mqtt.ObservableFactory observableFactory;
    private final Converter<ResponseBody, ResponseT> responseConverter;
    private final boolean isObservable;

    /**
     * 根据{@link RequestFactory}、{@link org.sheedon.mqtt.CallFactory}、
     * {@link Converter<ResponseBody, ResponseT>} 构建mqtt服务方法类
     *
     * @param requestFactory    创建请求body的工厂
     * @param callFactory       Call构建工厂
     * @param responseConverter 响应转化器
     */
    MqttServiceMethod(
            RequestFactory requestFactory,
            org.sheedon.mqtt.CallFactory callFactory,
            Converter<ResponseBody, ResponseT> responseConverter) {
        this.requestFactory = requestFactory;
        this.callFactory = callFactory;
        this.observableFactory = null;
        this.responseConverter = responseConverter;
        this.isObservable = false;
    }

    /**
     * 根据{@link RequestFactory}、{@link org.sheedon.mqtt.ObservableFactory}、
     * {@link Converter<ResponseBody, ResponseT>} 构建mqtt服务方法类
     *
     * @param requestFactory    创建请求body的工厂
     * @param observableFactory Observable构建工厂
     * @param responseConverter 响应转化器
     */
    MqttServiceMethod(
            RequestFactory requestFactory,
            org.sheedon.mqtt.ObservableFactory observableFactory,
            Converter<ResponseBody, ResponseT> responseConverter) {
        this.requestFactory = requestFactory;
        this.callFactory = null;
        this.observableFactory = observableFactory;
        this.responseConverter = responseConverter;
        this.isObservable = true;
    }

    /**
     * 若{@link isObservable} 为true，则构建{@link OkMqttObservable}，反之构建{@link OkMqttCall}
     *
     * @param args 参数
     */
    @Override
    final @Nullable
    ReturnT invoke(Object[] args) {
        if (isObservable) {
            Observable<ResponseT> observable = new OkMqttObservable<>(requestFactory, args, observableFactory, responseConverter);
            return adapt(observable, args);
        }
        Call<ResponseT> call = new OkMqttCall<>(requestFactory, args, callFactory, responseConverter);
        return adapt(call, args);
    }

    /**
     * 通过Call/args 调度方法
     *
     * @param call Call<ResponseT>
     * @param args Object[]
     * @return ReturnT
     */
    protected @Nullable
    ReturnT adapt(Call<ResponseT> call, Object[] args) {
        throw new RuntimeException("Please implement the method of adapt Call");
    }

    /**
     * Observable/args 调度方法
     *
     * @param call Observable<ResponseT>
     * @param args Object[]
     * @return ReturnT
     */
    protected ReturnT adapt(Observable<ResponseT> call, Object[] args) {
        throw new RuntimeException("Please implement the method of adapt Observable");
    }

    // call调度的mqtt服务方法
    static final class CallAdapted<ResponseT, ReturnT> extends MqttServiceMethod<ResponseT, ReturnT> {
        private final CallAdapter<ResponseT, ReturnT> callAdapter;

        CallAdapted(
                RequestFactory requestFactory,
                org.sheedon.mqtt.CallFactory callFactory,
                Converter<ResponseBody, ResponseT> responseConverter,
                CallAdapter<ResponseT, ReturnT> callAdapter) {
            super(requestFactory, callFactory, responseConverter);
            this.callAdapter = callAdapter;
        }

        @Override
        protected ReturnT adapt(Call<ResponseT> call, Object[] args) {
            return callAdapter.adapt(call);
        }
    }

    // Observable调度的mqtt服务方法
    static final class ObservableAdapted<ResponseT, ReturnT> extends MqttServiceMethod<ResponseT, ReturnT> {

        private final CallAdapter<ResponseT, ReturnT> callAdapter;

        ObservableAdapted(
                RequestFactory requestFactory,
                Converter<ResponseBody, ResponseT> responseConverter,
                org.sheedon.mqtt.ObservableFactory observableFactory,
                CallAdapter<ResponseT, ReturnT> callAdapter) {
            super(requestFactory, observableFactory, responseConverter);
            this.callAdapter = callAdapter;
        }

        @Override
        protected ReturnT adapt(Observable<ResponseT> call, Object[] args) {
            return callAdapter.adapt(call);
        }
    }

    // 通过协程响应结果的mqtt服务方法
    static final class SuspendForResponse<ResponseT> extends MqttServiceMethod<ResponseT, Object> {
        private final CallAdapter<ResponseT, Call<ResponseT>> callAdapter;

        SuspendForResponse(
                RequestFactory requestFactory,
                org.sheedon.mqtt.CallFactory callFactory,
                Converter<ResponseBody, ResponseT> responseConverter,
                CallAdapter<ResponseT, Call<ResponseT>> callAdapter) {
            super(requestFactory, callFactory, responseConverter);
            this.callAdapter = callAdapter;
        }

        @Override
        protected Object adapt(Call<ResponseT> call, Object[] args) {
            call = callAdapter.adapt(call);

            //noinspection unchecked Checked by reflection inside RequestFactory.
            Continuation<Response<ResponseT>> continuation =
                    (Continuation<Response<ResponseT>>) args[args.length - 1];

            // See SuspendForBody for explanation about this try/catch.
            try {
                return KotlinExtensions.awaitResponse(call, continuation);
            } catch (Exception e) {
                return KotlinExtensions.suspendAndThrow(e, continuation);
            }
        }
    }

    // 通过协程处理body的mqtt服务方法
    static final class SuspendForBody<ResponseT> extends MqttServiceMethod<ResponseT, Object> {
        private final CallAdapter<ResponseT, Call<ResponseT>> callAdapter;
        private final boolean isNullable;

        SuspendForBody(
                RequestFactory requestFactory,
                org.sheedon.mqtt.CallFactory callFactory,
                Converter<ResponseBody, ResponseT> responseConverter,
                CallAdapter<ResponseT, Call<ResponseT>> callAdapter,
                boolean isNullable) {
            super(requestFactory, callFactory, responseConverter);
            this.callAdapter = callAdapter;
            this.isNullable = isNullable;
        }

        @Override
        protected Object adapt(Call<ResponseT> call, Object[] args) {
            call = callAdapter.adapt(call);

            //noinspection unchecked Checked by reflection inside RequestFactory.
            Continuation<ResponseT> continuation = (Continuation<ResponseT>) args[args.length - 1];

            // Calls to OkMqtt Call.enqueue() like those inside await and awaitNullable can sometimes
            // invoke the supplied callback with an exception before the invoking stack frame can return.
            // Coroutines will intercept the subsequent invocation of the Continuation and throw the
            // exception synchronously. A Java Proxy cannot throw checked exceptions without them being
            // declared on the interface method. To avoid the synchronous checked exception being wrapped
            // in an UndeclaredThrowableException, it is intercepted and supplied to a helper which will
            // force suspension to occur so that it can be instead delivered to the continuation to
            // bypass this restriction.
            try {
                return isNullable
                        ? KotlinExtensions.awaitNullable(call, continuation)
                        : KotlinExtensions.await(call, continuation);
            } catch (Exception e) {
                return KotlinExtensions.suspendAndThrow(e, continuation);
            }
        }
    }
}
