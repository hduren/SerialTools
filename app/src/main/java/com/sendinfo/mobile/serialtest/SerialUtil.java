package com.sendinfo.mobile.serialtest;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android_serialport_api.SerialPort;

public class SerialUtil {
    //    String serialPort;
//    int serialRate;
    static SerialUtil serialUtil;
    SerialPort serialPort = null;

    private SerialUtil() {
//        this.serialPort=serialPort;
//        this.serialRate=serialRate;

    }

    public static SerialUtil getInstance() {
        if (null == serialUtil) {
            serialUtil = new SerialUtil();
        }
        return serialUtil;

    }

    public String write(byte[] datas) {
        String result = "";
        if (null != serialPort) {
            try {
                OutputStream outputStream = serialPort.getOutputStream();
                if (null != outputStream) {

                    outputStream.write(datas);
                    result = "write succ";
                }
            } catch (IOException e) {
                e.printStackTrace();
                result = "send exception"+e.toString();
            }
        }
        return result;

    }

    private boolean readSerial = false;

    public byte[] read() {
        byte[] result = null;
        int length = 0;

        if (null != serialPort) {
            try {
                InputStream inputStream = serialPort.getInputStream();
                long time = System.currentTimeMillis();
                long lastReadTime = System.currentTimeMillis();
                if (null != inputStream) {
                    readSerial = true;
                    boolean readOnes=false;
                    while (readSerial &&
                            System.currentTimeMillis() - time < 5000) {
                        int avilable = inputStream.available();
                        Log.e("readbytesavilable","avilable"+avilable);
                        if (avilable > 0) {

                            lastReadTime = System.currentTimeMillis();
                            byte[] tempBytes = new byte[avilable];
                            int readLength = inputStream.read(tempBytes);
                            byte[] totalBytes = new byte[length + readLength];
                            System.arraycopy(tempBytes, 0, totalBytes, length, readLength);
                            if(length>0){
                            System.arraycopy(result, 0, totalBytes, 0, length);
                            }
                            length = length + readLength;
                            result = totalBytes;
                            readOnes=true;
                            Log.e("readbytesr","avilable"+avilable+" "+showHexString(result));

                        } else {
                            if (readOnes&&System.currentTimeMillis() - lastReadTime > 150) {
                                break;
                            }
                            try {
                                Thread.sleep(50);
                            } catch (Exception e) {

                            }
                        }
                    }
                } else {

                }
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
        Log.e("readbytes","return"+result);
        return result;
    }


    public String open(final String serialPortstr, final int serialRate) {
        String result="";
        close();

             try{
                    serialPort = new SerialPort(new File(serialPortstr), serialRate, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                 result="串口打开失败"+e.toString();
                }
                return result;



    }


    public void close() {
        readSerial = false;
        if (null != this.serialPort) {
            serialPort.close();
            serialPort = null;
        }
    }

    public String showHexString(byte[] result) {
        StringBuilder stringBuilder = new StringBuilder();
        if (null != result && result.length > 0) {
            for (int i = 0; i < result.length; i++)
                stringBuilder.append((Integer.toHexString(result[i]&0xff))+" ");
        }
        return stringBuilder.toString();
    }
}
