package org.sheedon.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import org.sheedon.sample.factory.RetrofitClient

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 连接mqtt
        RetrofitClient.getInstance().initConfig()

    }
}