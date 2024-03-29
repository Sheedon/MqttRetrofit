# MqttRetrofit

```
MQTT client used on Android to request subscription structure changes.
```

[中文](README_CN.md)



## How to use

#### Step 1: Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```



#### Step 2: Add core dependencies

```groovy
dependencies {
    implementation 'com.github.Sheedon.MqttRetrofit:retrofit:2.0.0-alpha-0'
}
```



#### Step 3：Build the request and listen for results

1. Retrofit turns your MQTT API into a Java interface.

```java
public interface GitHubService {
  @TOPIC("user/{id}")
  Call<Void> notifyRepos(@Path("id") String id);
  
  
  @SUBSCRIBE("cmd/user/{id}")
  Observable<User> getRepos(@Path("id") String id);
}
```



2. The `Retrofit` class generates an implementation of the `GitHubService` interface.

```java
Retrofit retrofit = new Retrofit.Builder()
    .baseUrl(mqttClient)
    .build();

GitHubService service = retrofit.create(GitHubService.class);
```



3. Each `Call` or `Observable` from the created `GitHubService` can make an asynchronous MQTT request to the remote webserver or subscribe to the remote webserver.

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