package com.annotation;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.Toast;

import injecter.annotation.Message;
import injecter.annotation.ModuleType;
import injecter.annotation.TransferModule;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        textView.setText("我通过BindView注解生成的");
    }

    @Message(module = ModuleType.LOGIN,
            trans = {@TransferModule(params ={"username","password"},types = {"java.lang.String","java.lang.String"},index = 1),
                     @TransferModule(params ={"username","password"},types = {"java.lang.String","java.lang.String"},index = 2)})
    public static void openActivity32(Context context,LoginModule string,LoginModule string1){
        Toast.makeText(context, "open "+string, Toast.LENGTH_SHORT).show();
    }
    @Message(module = ModuleType.LOGIN)
    public static void openActivity23(Context context,String string){
        Toast.makeText(context, "open "+string, Toast.LENGTH_SHORT).show();
    }
    @Message(module = ModuleType.LOGIN)
    public void openActivity33(Context context,String string){
        Toast.makeText(context, "open "+string, Toast.LENGTH_SHORT).show();
    }

    @Message(module = ModuleType.LOADING)
    public static void openActivity2(Context context,String string){
        Toast.makeText(context, "open "+string, Toast.LENGTH_SHORT).show();
    }
    @Message(module = ModuleType.LOADING)
    public static void openActivity321(Context context,String string){
        Toast.makeText(context, "open "+string, Toast.LENGTH_SHORT).show();
    }
    @Message(module = ModuleType.LOADING)
    public static void openActivity4(Context context,String string){
        Toast.makeText(context, "open "+string, Toast.LENGTH_SHORT).show();
    }
    @Message(module = ModuleType.LOADING)
    public static void openActivity3(Context context,String string){
        Toast.makeText(context, "open "+string, Toast.LENGTH_SHORT).show();
    }


}
