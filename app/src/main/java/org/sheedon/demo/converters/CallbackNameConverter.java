package org.sheedon.demo.converters;

import com.google.gson.Gson;

import org.sheedon.demo.RspModel;
import org.sheedon.mqtt.ResponseBody;
import org.sheedon.rr.core.DataConverter;


/**
 * @Description: java类作用描述
 * @Author: sheedon
 * @Email: sheedonsun@163.com
 * @Date: 2020/3/11 0:45
 */
public class CallbackNameConverter implements DataConverter<ResponseBody, String> {
    Gson gson;

    public CallbackNameConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    public String convert(ResponseBody value) {
        if (value == null || value.getData() == null || value.getData().isEmpty())
            return "";

        RspModel<?> rspModel = null;
        try {
            rspModel = gson.fromJson(value.getData(), RspModel.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (rspModel == null || rspModel.getType() == null || rspModel.getType().equals(""))
            return "";
        //value.getTopic()
        return rspModel.getType();
    }
}
