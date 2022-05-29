package org.sheedon.sample.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.sheedon.sample.databinding.FragmentPublishMessageBinding
import org.sheedon.sample.R
import org.sheedon.sample.remoteService

/**
 * 提交Mqtt message 消息
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:07
 */
class SimplePublishFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.label_publish_message)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentPublishMessageBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_publish_message, container, false)

        // 提交mqtt消息
        binding.btnPublish.setOnClickListener {
            publish()
        }

        binding.btnPublish1.setOnClickListener {
            publish1()
        }
        return binding.root
    }

    /**
     * 发送一个简易的Mqtt消息请求
     */
    private fun publish() {

        val orgList = arrayListOf("11", "22", "33")

        val call = remoteService.notifyUser("001", "zhangsan")
        call.publish()
    }

    private fun publish1(){
        val orgList = arrayListOf("444", "555", "66")
        val call = remoteService.notifyUser("002", "lisi")
        call.publish()
    }
}