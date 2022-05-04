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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Use the platform
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/2/22 17:23
 */
abstract class Platform {
    private static final Platform PLATFORM = findPlatform();

    static Platform get() {
        return PLATFORM;
    }

    private static Platform findPlatform() {
        switch (System.getProperty("java.vm.name")) {
            case "Dalvik":
                return new Android();

//            case "RoboVM":
//                return new RoboVm();
//
//            default:
//                if (Java16.isSupported()) {
//                    return new Java16();
//                }
//                if (Java14.isSupported()) {
//                    return new Java14();
//                }
//                return new Java8();
            default:
                return null;
        }
    }

    @Nullable
    abstract Executor defaultCallbackExecutor();

    abstract List<? extends CallAdapter.Factory> createDefaultCallAdapterFactories(
            @Nullable Executor callbackExecutor);

    abstract List<? extends Converter.Factory> createDefaultConverterFactories();

    abstract boolean isDefaultMethod(Method method);

    abstract @Nullable
    Object invokeDefaultMethod(
            Method method, Class<?> declaringClass, Object proxy, Object... args) throws Throwable;

    private static final class Android extends Platform {

        @Override
        boolean isDefaultMethod(Method method) {
            return false;
        }

        @Nullable
        @Override
        Object invokeDefaultMethod(Method method, Class<?> declaringClass, Object proxy, Object... args) {
            throw new AssertionError();
        }

        @Nullable
        @Override
        Executor defaultCallbackExecutor() {
            return MainThreadExecutor.INSTANCE;
        }

        @Override
        List<? extends CallAdapter.Factory> createDefaultCallAdapterFactories(@Nullable Executor callbackExecutor) {
            return singletonList(new DefaultCallAdapterFactory(callbackExecutor));
        }

        @Override
        List<? extends Converter.Factory> createDefaultConverterFactories() {
            return emptyList();
        }
    }

    private static final class MainThreadExecutor implements Executor {
        static final Executor INSTANCE = new MainThreadExecutor();

        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable r) {
            handler.post(r);
        }
    }
}