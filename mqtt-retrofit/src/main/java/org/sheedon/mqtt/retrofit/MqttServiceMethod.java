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
 * Adapts an invocation of an interface method into an Mqtt call or Observable.
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/2/6 4:24 下午
 */
abstract class MqttServiceMethod<ResponseT, ReturnT> extends ServiceMethod<ReturnT> {

    /**
     * Inspects the annotations on an interface method to construct a reusable service method that
     * speaks MQTT. This requires potentially-expensive reflection so it is best to build each service
     * method only once and reuse it.
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

    private static <ResponseT, ReturnT> CallAdapter<ResponseT, ReturnT> createCallAdapter(
            Retrofit retrofit, Method method, Type returnType, Annotation[] annotations) {
        try {
            //noinspection unchecked
            return (CallAdapter<ResponseT, ReturnT>) retrofit.callAdapter(returnType, annotations);
        } catch (RuntimeException e) { // Wide exception range because factories are user code.
            throw Utils.methodError(method, e, "Unable to create call adapter for %s", returnType);
        }
    }

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

    protected @Nullable ReturnT adapt(Call<ResponseT> call, Object[] args) {
        throw new RuntimeException("Please implement the method of adapt Call");
    }

    protected ReturnT adapt(Observable<ResponseT> call, Object[] args) {
        throw new RuntimeException("Please implement the method of adapt Observable");
    }

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
