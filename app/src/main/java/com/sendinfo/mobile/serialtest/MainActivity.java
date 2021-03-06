package com.sendinfo.mobile.serialtest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.trello.rxlifecycle.ActivityEvent;
import com.trello.rxlifecycle.components.support.RxAppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import android_serialport_api.SerialPort;
import android_serialport_api.SerialPortFinder;
import rx.Emitter;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends RxAppCompatActivity {
    public static String TAG_SERIAL_PORT_POSITION="TAG_SERIAL_PORT_POSITION";
    public static String TAG_SERIAL_RATE_POSITION="TAG_SERIAL_RATE_POSITION";
    Spinner spSerial, spSerialRate, spSendinterval;
    Button btnSend, btnOpen;
    CheckBox cbHexShow;
    TextView tvReceiveData, tvStatus;
    EditText etSendData;
    public SerialPortFinder mSerialPortFinder = new SerialPortFinder();
    String[] serialEntryValues;
    String[] serialEntryRateValues = new String[]{
            "300", "600", "1200", "2400", "4800", "9600", "19200", "38400", "43000", "56000", "57600",
            "115200",
    };
    String[] sendIntervalArrays = new String[]{
            "0", "1", "3", "5", "8", "10", "15", "20", "30", "60"
    };
    private String serialPortPort = "";
    private int serialRate = 0;
    private int sendInterval = 0;
    int totalSend = 0, totalReceive = 0;
    SerialUtil serialUtil;
    boolean isSerialOpen = false;
    boolean isInContuineSend = false;
    boolean isInContinueRead = false;

    Subscriber continueSubscriber = null;
    Subscriber receiveSubscriber = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cbHexShow = findViewById(R.id.hexShow);
        spSerial = findViewById(R.id.spSerial);
        spSerialRate = findViewById(R.id.spSerialRate);
        spSendinterval = findViewById(R.id.spSendinterval);
        btnSend = findViewById(R.id.btnSend);
        btnOpen = findViewById(R.id.btnOpen);
        tvReceiveData = findViewById(R.id.tvReceiveData);
        etSendData = findViewById(R.id.etSendData);
        tvStatus = findViewById(R.id.tvStatus);
        initSerial();
        initSerialRate();
        initSendInterval();
        serialUtil = SerialUtil.getInstance();
        btnSend.setVisibility(View.GONE);
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openClose();
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickSend();
            }
        });
        tvStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                totalSend = 0;
                totalReceive = 0;
                showStatus();
                tvReceiveData.setText("");
            }
        });


    }

    @Override
    protected void onDestroy() {
        isInContinueRead = false;
        serialUtil.close();
        super.onDestroy();
    }

    private void openClose() {
        if (!isSerialOpen) {
            String openR = serialUtil.open(serialPortPort, serialRate);
            if (TextUtils.isEmpty(openR)) {
                btnOpen.setText("关闭");
                btnSend.setVisibility(View.VISIBLE);
                isSerialOpen = true;
                receiveSerialData();
            } else {
                Toast.makeText(MainActivity.this, openR, Toast.LENGTH_SHORT
                ).show();
            }
        } else {
            isInContinueRead = false;
            if (null != receiveSubscriber) {
                receiveSubscriber.unsubscribe();
            }

            if (isInContuineSend) {
                clickSend();
            }
            serialUtil.close();
            btnOpen.setText("打开");
            btnSend.setVisibility(View.GONE);
            isSerialOpen = false;
        }

    }


    private void clickSend() {
        if (!isSerialOpen) {
            Toast.makeText(MainActivity.this, "串口未打开", Toast.LENGTH_SHORT).show();
            return;
        }
        String data = etSendData.getText().toString();
        if (TextUtils.isEmpty(data)) {
            return;
        }
        byte[] serialBytes =null;
        if(cbHexShow.isChecked()){
            String[] array=data.split(" ");
          serialBytes=new byte[array.length];
            for(int i=0;i<array.length;i++){
                try{
                int num=Integer.parseInt(array[i],16);
                serialBytes[i]= (byte) num;
                }catch (Exception e){
                    Log.e("parsee",""+e.toString());
                }
            }


        }else {
           serialBytes = data.getBytes();
        }

        if (isInContuineSend) {
            continueSubscriber.unsubscribe();
            isInContuineSend = false;
            btnSend.setText("发送");
        } else {
            if (sendInterval == 0) {
                sendSerialDataOnce(serialBytes);
            } else {
                isInContuineSend = true;
                sendSerialDataMuilt(sendInterval, serialBytes);
                btnSend.setText("停止");
            }
        }
    }

    private void showStatus() {
        tvStatus.setText("总接收 " + totalReceive + " 总发送 " + totalSend + " 点击清空"+(totalReceive-totalSend));
    }


    private void sendSerialDataOnce(final byte[] dataBytes) {

        if (null == dataBytes || dataBytes.length == 0) {

            return;
        }
        Observable.just(dataBytes).map(
                new Func1<byte[], Object>() {
                    @Override
                    public Object call(byte[] s) {
                        try {
                            serialUtil.write(s);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return null;
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Action1<Object>() {

                            @Override
                            public void call(Object o) {

                            }
                        }
                );
    }


    private void sendSerialDataMuilt(final int interval, final byte[] dataBytes) {

        if (null == dataBytes || dataBytes.length == 0) {

            return;
        }

        continueSubscriber = new Subscriber() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Object o) {
                if(null!=o&&o instanceof String&&o.toString().equals("write succ")){
                    totalSend++;
                    showStatus();
                }


            }
        };
        Observable.interval(interval, TimeUnit.SECONDS).map(
                new Func1<Long, Object>() {
                    @Override
                    public Object call(Long aLong) {
                        try {
                            Log.e("serial", "sendSerial");
                          return   serialUtil.write(dataBytes);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(
                        continueSubscriber
                );
    }


    private void receiveSerialData() {


        receiveSubscriber = new Subscriber<byte[]>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(byte[] o) {
                Log.e("readbytesrRonnext","avilable"+showHexString(o));
                String text=tvReceiveData.getText().toString();
                if (cbHexShow.isChecked()) {
                    text=(totalReceive+1)+" 收 "+showHexString(o) + "\n"+text;

                } else {
                    text=(totalReceive+1)+" 收 "+new String(o) + "\n"+text;
                }
                if(text.length()>1500){
                    text=text.substring(0,1500)+"...";
                }
                tvReceiveData.setText(text);
                totalReceive++;
                showStatus();


            }
        };
        Observable.unsafeCreate(new Observable.OnSubscribe<byte[]>() {

                                    @Override
                                    public void call(Subscriber<? super byte[]> subscriber) {
                                        isInContinueRead = true;
                                        while (isInContinueRead) {
                                            try {
                                              byte[] bytes=  serialUtil.read();
//                                              Log.e("readbytes",showHexString(bytes));
                                              if(null!=bytes&&bytes.length>0){
                                                  Log.e("readbytesrsonnext","avilable"+showHexString(bytes));
                                                subscriber.onNext(bytes);
                                              }

                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
        )
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .compose(this.bindUntilEvent(ActivityEvent.DESTROY))
                .subscribe(
                        receiveSubscriber
                );
    }


    private void initSerial() {

        serialEntryValues = mSerialPortFinder.getAllDevicesPath();
        ArrayAdapter adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, serialEntryValues);


        //设置下拉列表的风格
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //将adapter 添加到spinner中
        spSerial.setAdapter(adapter);

        //添加事件Spinner事件监听
        spSerial.setOnItemSelectedListener(new SpinnerSelectedListener());
        int serial=PerUtil.getInstance().getPreferInt(getApplicationContext(),TAG_SERIAL_PORT_POSITION);

        spSerial.setSelection(serial);
    }

    //使用数组形式操作
    class SpinnerSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                   long arg3) {
//            view.setText("你的血型是："+m[arg2]);
            serialPortPort = serialEntryValues[arg2];
            PerUtil.getInstance().savePreferInt(getApplicationContext(),TAG_SERIAL_PORT_POSITION,arg2);
//            Toast.makeText(MainActivity.this,serialEntryValues[arg2],Toast.LENGTH_SHORT).show();
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    private void initSerialRate() {


        ArrayAdapter adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, serialEntryRateValues);


        //设置下拉列表的风格
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //将adapter 添加到spinner中
        spSerialRate.setAdapter(adapter);

        //添加事件Spinner事件监听
        spSerialRate.setOnItemSelectedListener(new SpinnerSerialRateSelectedListener());
        int serial=PerUtil.getInstance().getPreferInt(getApplicationContext(),TAG_SERIAL_RATE_POSITION);

        spSerialRate.setSelection(serial);
    }

    //使用数组形式操作
    class SpinnerSerialRateSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                   long arg3) {
//            view.setText("你的血型是："+m[arg2]);
            String rateString = serialEntryRateValues[arg2];
            if (!TextUtils.isEmpty(rateString)) {
                serialRate = Integer.parseInt(rateString);
                PerUtil.getInstance().savePreferInt(getApplicationContext(),TAG_SERIAL_RATE_POSITION,arg2);
            }
//            Toast.makeText(MainActivity.this,serialEntryRateValues[arg2],Toast.LENGTH_SHORT).show();
        }

        public void onNothingSelected(AdapterView<?> arg0) {
        }
    }

    private void initSendInterval() {


        ArrayAdapter adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, sendIntervalArrays);


        //设置下拉列表的风格
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        //将adapter 添加到spinner中
        spSendinterval.setAdapter(adapter);

        //添加事件Spinner事件监听
        spSendinterval.setOnItemSelectedListener(new SpinnerSendIntervalSelectedListener());
    }

    //使用数组形式操作
    class SpinnerSendIntervalSelectedListener implements AdapterView.OnItemSelectedListener {

        public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
                                   long arg3) {
//            view.setText("你的血型是："+m[arg2]);
            String sendIntervalStr = sendIntervalArrays[arg2];
            if (!TextUtils.isEmpty(sendIntervalStr)) {
                sendInterval = Integer.parseInt(sendIntervalStr);
            }
//            Toast.makeText(MainActivity.this,,Toast.LENGTH_SHORT).show();
        }

        public void onNothingSelected(AdapterView<?> arg0) {
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
