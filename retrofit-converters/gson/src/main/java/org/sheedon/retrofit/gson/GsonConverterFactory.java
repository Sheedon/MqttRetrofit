package org.sheedon.retrofit.gson;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

import org.sheedon.mqtt.ResponseBody;
import org.sheedon.mqtt.retrofit.Converter;
import org.sheedon.mqtt.retrofit.FormBodyConverter;
import org.sheedon.mqtt.retrofit.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * 通过 Gson 用于 JSON 的 {@linkplain Converter.Factory 转换器}的实例化使用。
 *
 * <p>因为 Gson 在它支持的类型方面非常灵活，所以这个转换器假定它可以处理所有类型。
 * 如果您将 JSON 序列化与其他内容（例如协议缓冲区）混合，则必须最后
 * {@linkplain Retrofit.BuilderaddConverterFactory(Converter.Factory) add this instance}
 * 让其他转换器有机会看到它们的类型。
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/5/24 21:36
 */
public class GsonConverterFactory extends Converter.Factory {

    /**
     * 使用默认的 {@link Gson} 实例创建一个实例以进行转换。
     */
    public static GsonConverterFactory create() {
        return create(new Gson());
    }

    /**
     * 使用默认的 {@link Gson} 实例创建一个实例以进行转换。
     */
    @SuppressWarnings("ConstantConditions") // Guarding public API nullability.
    public static GsonConverterFactory create(Gson gson) {
        if (gson == null) throw new NullPointerException("gson == null");
        return new GsonConverterFactory(gson);
    }

    private final Gson gson;

    private GsonConverterFactory(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(
            Type type, Annotation[] annotations, Retrofit retrofit) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new GsonResponseBodyConverter<>(adapter);
    }

    @Override
    public Converter<?, String> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new GsonRequestBodyConverter<>(adapter);
    }

    @Override
    public Converter<?, String> stringConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new GsonStringConverter<>(adapter);
    }

    @Override
    public FormBodyConverter formBodyConverter() {
        return new FormBody.Builder();
    }
}
