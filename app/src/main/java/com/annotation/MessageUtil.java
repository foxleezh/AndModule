package com.annotation;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by foxleezh on 18-3-7.
 * 模块间通信工具
 * type 由 aabb组成，高位aa表示模组，由2位十六进制（共256）表示，低位表示消息下标，由6位十六进制（共16^6）表示
 */

public class MessageUtil {

    final static String Tag="MessageUtil";

    private final static MessageUtil INSTANCE = new MessageUtil();

    public ArrayList<BaseMessageHandler> handlers=new ArrayList<>();

    private MessageUtil(){}

    public static MessageUtil getInstance(){
        return INSTANCE;
    }

    public void addHandlers(BaseMessageHandler handler){
        handlers.add(handler);
    }

    public void postMessage(int type, Object... data){
        BaseMessageHandler handler;
        int index=type >> 8 ;
        if(index>handlers.size()-1){
            Log.w(Tag,"type out of size ,index = "+index);
            return;
        }
        handler = handlers.get(index);
        handler.handleMessage(type, data);
    }

    public interface BaseMessageHandler{

        void handleMessage(int type, Object... data);

    }
}
