# MqttRetrofit

```tex
在 Android 上使用的MQTT客户端，用于请求订阅结构改造。
```

[English](README.md)



## 使用方式

#### 第一步：将 JitPack 存储库添加到您的构建文件中

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

#### 第二步：添加核心依赖

```groovy
dependencies {
    implementation 'com.github.Sheedon.MqttRetrofit:retrofit:2.0.0-alpha-0'
}
```

#### 第三步：构建请求，监听结果

1. 改造将您的 MQTT API转换为Java接口。

```java
public interface GitHubService {
  @TOPIC("user/{id}")
  Call<Void> notifyRepos(@Path("id") String id);
  
  
  @SUBSCRIBE("cmd/user/{id}")
  Observable<User> getRepos(@Path("id") String id);
}
```



2. Retrofit 类生成 GitHubService 接口的实现。

```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl(mqttClient)
    .build();

GitHubService service = retrofit.create(GitHubService.class);
```



3. 每个来自已创建GitHubService的调用都可以向远程服务器发出异步的MQTT请求，或订阅MQTT消息。

```java
// 将接口传入retrofit中，进行动态代理
Call<Void> repos = service.notifyRepos("1");
Observable<User> repos = service.getRepos("1");
```



## License

```
Copyright 2020 Sheedon.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```