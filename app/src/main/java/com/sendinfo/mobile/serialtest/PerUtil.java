package com.sendinfo.mobile.serialtest;

import android.content.Context;
import android.content.SharedPreferences;

public class PerUtil {
    SharedPreferences sharedPreferences=null;
    static  PerUtil perUtil=null;
    public static    PerUtil getInstance(){
        if(null==perUtil){
            perUtil=new PerUtil();
        }
        return  perUtil;
    }

    public  void savePrefer(Context context,String key,String value){
        sharedPreferences=context.getSharedPreferences("serial_setting",Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(key,value).commit();

    }

    public  String getPrefer(Context context,String key){
          sharedPreferences=context.getSharedPreferences("serial_setting",Context.MODE_PRIVATE);
        return sharedPreferences.getString(key,"");

    }

    public  void savePreferInt(Context context,String key,int value){
        sharedPreferences=context.getSharedPreferences("serial_setting",Context.MODE_PRIVATE);
        sharedPreferences.edit().putInt(key,value).commit();

    }

    public  int getPreferInt(Context context,String key){
        sharedPreferences=context.getSharedPreferences("serial_setting",Context.MODE_PRIVATE);
        return sharedPreferences.getInt(key,0);

    }




}
