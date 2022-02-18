package org.sheedon.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.google.gson.Gson;

import org.sheedon.mqtt.retrofit.Call;
import org.sheedon.mqtt.retrofit.Callback;
import org.sheedon.mqtt.retrofit.Response;
import org.sheedon.mqtt.retrofit.Retrofit;
import org.sheedon.mqtt.retrofit.converters.GsonConverterFactory;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MqttClient.getInstance();
        final Retrofit retrofit = new Retrofit.Builder()
                .client(MqttClient.getInstance().getClient())
                .baseTopic("")
                .addConverterFactory(GsonConverterFactory.create())
                .build();


        final RemoteService remoteService = retrofit.create(RemoteService.class);

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Call<RspModel<List<AdminCard>>> call = remoteService.getManagerList(new UserSubmitModel("get_manager_list", ""));
//                call.publishNotCallback();
                call.enqueue(new Callback.Call<RspModel<List<AdminCard>>>() {
                    @Override
                    public void onResponse(Call<RspModel<List<AdminCard>>> call,
                                           Response<RspModel<List<AdminCard>>> response) {
                        System.out.println(new Gson().toJson(response));
                    }

                    @Override
                    public void onFailure(Call<RspModel<List<AdminCard>>> call,
                                          Throwable t) {
                        System.out.println(t);
                    }
                });
            }
        });
    }
}
