package com.example.smstest;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 1;
    private EditText phoneNumberEditText;
    private TextView batteryLevelTextView;
    private TextView smsStatusTextView;
    private Button startButton;
    private boolean isMonitoring = false;
    private boolean isSmsSent = false;

    private final BroadcastReceiver batteryLevelReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int batteryPct = (int) ((level / (float) scale) * 100);
            batteryLevelTextView.setText("当前电量：" + batteryPct + "%");

            if (batteryPct == 100 && !isSmsSent) {
                String phoneNumber = phoneNumberEditText.getText().toString().trim();
                if (!TextUtils.isEmpty(phoneNumber)) {
                    sendSms(phoneNumber, "您的设备电量已充满（100%）。");
                    isSmsSent = true;
                } else {
                    Toast.makeText(MainActivity.this, "请输入有效的电话号码。", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        batteryLevelTextView = findViewById(R.id.batteryLevelTextView);
        smsStatusTextView = findViewById(R.id.smsStatusTextView);
        startButton = findViewById(R.id.startButton);

        startButton.setOnClickListener(v -> {
            if (!isMonitoring) {
                String phoneNumber = phoneNumberEditText.getText().toString().trim();
                if (TextUtils.isEmpty(phoneNumber)) {
                    Toast.makeText(MainActivity.this, "请输入电话号码。", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.SEND_SMS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.SEND_SMS,Manifest.permission.READ_PHONE_STATE}, SMS_PERMISSION_REQUEST_CODE);
                } else {
                    startMonitoring();
                }
            } else {
                stopMonitoring();
            }
        });
    }

    private void startMonitoring() {
        isMonitoring = true;
        isSmsSent = false;
        registerReceiver(batteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        startButton.setText("停止");
        smsStatusTextView.setText("短信发送状态：监控中...");
    }

    private void stopMonitoring() {
        isMonitoring = false;
        unregisterReceiver(batteryLevelReceiver);
        startButton.setText("开始");
        smsStatusTextView.setText("短信发送状态：已停止监控");
    }

    private void sendSms(String phoneNumber, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            smsStatusTextView.setText("短信发送状态：已发送至 " + phoneNumber);
            Toast.makeText(this, "短信已发送。", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            smsStatusTextView.setText("短信发送状态：发送失败");
            Toast.makeText(this, "短信发送失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isMonitoring) {
            unregisterReceiver(batteryLevelReceiver);
        }
    }
}