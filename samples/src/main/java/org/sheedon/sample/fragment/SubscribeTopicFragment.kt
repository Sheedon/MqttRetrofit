package org.sheedon.sample.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.eclipse.paho.client.mqttv3.internal.wire.MqttSubscribe
import org.sheedon.sample.viewmodel.SubscribeTopicViewModel
import org.sheedon.sample.R
import org.sheedon.sample.databinding.FragmentSubscribeTopicBinding
import org.sheedon.mqtt.retrofit.FullConsumer
import org.sheedon.mqtt.retrofit.Observable
import org.sheedon.mqtt.retrofit.Response
import org.sheedon.sample.model.User
import org.sheedon.sample.remoteService

/**
 * 订阅一个主题
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:09
 */
class SubscribeTopicFragment : Fragment() {

    private lateinit var binding: FragmentSubscribeTopicBinding
    private val viewModel = SubscribeTopicViewModel()
    private var observable: Observable<User>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.label_subscribe_topic)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_subscribe_topic, container, false)
        binding.vm = viewModel
        binding.button.setOnClickListener { subscribeAndUnSubscribe() }
        return binding.root
    }

    private fun subscribeAndUnSubscribe() {
        val isSubscribe = viewModel.isSubscribe.get()
        if (!isSubscribe) {
            subscribe()
            return
        }
        observable!!.cancel()
        viewModel.isSubscribe.set(false)
    }

    private fun subscribe() {
        observable = remoteService.listenUser()
        observable!!.enqueue(object : FullConsumer<User> {
            override fun onResponse(
                observable: Observable<User>,
                response: Response<User>?
            ) {
                binding.tvMessage.text = response?.toString()
            }

            override fun onFailure(
                observable: Observable<User>,
                t: Throwable?
            ) {
                binding.tvMessage.text = t.toString()
            }

            override fun onResponse(
                observable: Observable<User>,
                response: MqttSubscribe?
            ) {
                viewModel.isSubscribe.set(true)
            }

        })
    }
}