package org.sheedon.sample.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import org.sheedon.mqtt.*
import org.sheedon.mqtt.retrofit.Call
import org.sheedon.mqtt.retrofit.Callback
import org.sheedon.mqtt.retrofit.Response
import org.sheedon.sample.R
import org.sheedon.sample.databinding.FragmentRequestResponseBinding
import org.sheedon.sample.model.User
import org.sheedon.sample.remoteService

/**
 * mqtt请求响应
 *
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2022/4/26 22:09
 */
class RequestAndResponseFragment : Fragment() {

    private lateinit var binding: FragmentRequestResponseBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().setTitle(R.string.label_request_and_response)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_request_response, container, false)
        binding.btnPublish.setOnClickListener { requestAndResponse() }

        return binding.root
    }

    private fun requestAndResponse() {

        val userById = remoteService.getUserById("123")
        userById.enqueue(object : Callback<User> {
            override fun onResponse(call: Call<User>, response: Response<User>?) {
                binding.tvMessage.text = response?.body()?.toString()
            }

            override fun onFailure(call: Call<User>, t: Throwable?) {
                binding.tvMessage.text = t?.toString()
            }

        })
    }
}