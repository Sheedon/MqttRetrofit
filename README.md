# MqttDispatcher

### Gradle

**Step 1.** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

**Step 2.** Add the dependency

```
	dependencies {
	        implementation 'com.github.Sheedon:MqttRetrofit:1.0.0'
	}
```



### Maven

**Step 1.** Add the JitPack repository to your build file

```
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

**Step 2.** Add the dependency

```
	<dependency>
	    <groupId>com.github.Sheedon</groupId>
	    <artifactId>MqttRetrofit</artifactId>
	    <version>1.0.0</version>
	</dependency>
```



#### 1. Create OkMqttClient

```java
OkMqttClient mClient = new OkMqttClient.Builder()
        .clientInfo(App.getInstance(), serverUri, clientId)// 上下文，mqtt地址，客户端id 
        .subscribeBodies(subscribeBodies)// 订阅主题
        .baseTopic("")// 添加基础主题，用于请求
        .addConverterFactory(CallbackRuleConverterFactory.create())//反馈匹配规则转化工厂
        .callback(this)// 额外反馈监听
        .build();
```



#### 2.  Create Retrofit

```java
Retrofit retrofit = new Retrofit.Builder()
        .client(MqttClient.getInstance().getClient())// 添加绑定客户端
        .baseTopic("")// 添加基础消息主题
        .addConverterFactory(GsonConverterFactory.create())// 添加信息解析转化器工厂
        .build();
```



#### 3. Create Interface

```java
interface RemoteService {


    @TOPIC("xxx/xxx")//主题
    @PAYLOAD("{\"type\":\"get_manager_list\",\"upStartTime\":\"\"}")// 消息体
    @DELAYMILLISECOND(5000)// 延迟时间
    @BACKNAME("get_manager_list")// 反馈名称
    Call<RspModel<List<AdminCard>>> getManagerList();

    
    @BACKNAME("get_manager_list")
    Call<RspModel<List<AdminCard>>> getManagerList(@Theme() String topic，// 替换TOPIC
                                                   @Body UserSubmitModel body);// 内容

    @FormEncoded
    @TOPIC("xxx/xxx/{deviceId}")
    @BACKNAME("get_manager_list")
    Call<RspModel<List<AdminCard>>> getManagerList(@Path("deviceId") String deviceId,//替换TOPIC中的deviceId
                                                   @Field("type") String type,// 消息内容
                                                   @Field("upStartTime") String upStartTime);
}
```

```java
// 将接口传入retrofit中，进行动态代理
RemoteService remoteService = retrofit中，进行动态代理.create(RemoteService.class)
```



#### 4. Use And Dispatcher

```java
// 调用方法
Call<RspModel<List<AdminCard>>> call = remoteService.getManagerList(new UserSubmitModel("get_manager_list", ""));

// 调用
call.enqueue(new Callback<RspModel<List<AdminCard>>>() {
             @Override
             public void onResponse(Call<RspModel<List<AdminCard>>> call, Response<RspModel<List<AdminCard>>> response) {
                  System.out.println(new Gson().toJson(response));
             }

             @Override
             public void onFailure(Call<RspModel<List<AdminCard>>> call, Throwable t) {
                    System.out.println(t);
             }
});
```