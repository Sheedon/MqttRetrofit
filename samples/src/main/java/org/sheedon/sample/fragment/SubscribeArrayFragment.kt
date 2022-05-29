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
import org.sheedon.sample.databinding.FragmentSubscribeArrayBinding
import org.sheedon.mqtt.*
import org.sheedon.mqtt.retrofit.FullConsumer
import org.sheedon.mqtt.retrofit.Observable
import org.sheedon.mqtt.retrofit.Response
import org.sheedon.sample.model.User
import org.sheedon.sample.remoteService

/**
 * 订阅一组主题
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:10
 */
class SubscribeArrayFragment : Fragment() {


    private lateinit var binding: FragmentSubscribeArrayBinding
    private val viewModel = SubscribeTopicViewModel()
    private var observable: Observable<User>? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.label_subscribe_topic_array)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_subscribe_array, container, false)
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
        observable?.cancel()
        viewModel.isSubscribe.set(false)
    }

    private fun subscribe() {

        // 单一关联内容
        val relation = Relation.Builder()
            .topics(Topics("sheedon/open_ack"))
            .build()

        // 关联集合
        val relations: List<Relation> = arrayListOf(
            Relation.Builder().topics(Topics("sheedon/alarm_ack")).build(),
            Relation.Builder().keyword("test_ack").build()
        )

        // 订阅对象
        val subscribe = Subscribe.Builder() // 标准配置
            .add("sheedon/test_ack") // 通过Relation配置
            .add(relation) // 添加Relation集合
            .addAll(relations)
            .build()

        observable = remoteService.listenArray(subscribe)
        observable!!.enqueue(object : FullConsumer<User> {
            override fun onResponse(observable: Observable<User>, response: Response<User>?) {
                binding.tvMessage.text = response?.toString()
            }

            override fun onResponse(observable: Observable<User>, response: MqttSubscribe?) {
                viewModel.isSubscribe.set(true)
            }

            override fun onFailure(observable: Observable<User>, t: Throwable?) {
                binding.tvMessage.text = t.toString()
            }

        })

    }
}