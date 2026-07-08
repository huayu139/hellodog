package com.example.hellodog;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private Button btnScanBle;
    private Button btnConnectSaved;
    private Button btnEyesActivity;
    private BluetoothAdapter bluetoothAdapter;
    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_ON) {
                    // 蓝牙已打开，允许点击
                    btnScanBle.setEnabled(true);
                } else if (state == BluetoothAdapter.STATE_OFF) {
                    // 蓝牙已关闭，禁止点击并提示
                    btnScanBle.setEnabled(false);
                    Toast.makeText(MainActivity.this, "蓝牙已关闭，请打开蓝牙后再使用", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化按钮
        btnScanBle = findViewById(R.id.btnScanBle);
        btnConnectSaved = findViewById(R.id.btnConnectSaved);
        btnEyesActivity = findViewById(R.id.btnEyesActivity);
        
        btnEyesActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EyesActivity.class);
                startActivity(intent);
            }
        });
        
        btnConnectSaved.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 打开已保存设备的列表或直接连接
                Intent intent = new Intent(MainActivity.this, BleConnectActivity.class);
                startActivity(intent);
            }
        });

        // 设置扫描按钮点击事件
        btnScanBle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BleScanActivity.class);
                startActivity(intent);
            }
        });

        // 获取 BluetoothAdapter（兼容 Android 12+）
        BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bm != null ? bm.getAdapter() : BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            // 设备不支持蓝牙
            btnScanBle.setEnabled(false);
            Toast.makeText(this, "此设备不支持蓝牙", Toast.LENGTH_LONG).show();
            return;
        }

        // 检查是否有上次保存的设备信息，若有且蓝牙已打开则尝试直接进入连接界面并自动连接
        android.content.SharedPreferences prefs = getSharedPreferences("hellodog_prefs", MODE_PRIVATE);
        String lastAddr = prefs.getString("last_device_address", null);
        String lastName = prefs.getString("last_device_name", null);
        if (lastAddr != null && bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(MainActivity.this, BleConnectActivity.class);
            intent.putExtra("device_name", lastName);
            intent.putExtra("device_address", lastAddr);
            intent.putExtra("auto_connect", true);
            startActivity(intent);
            finish();
            return;
        }

        // 初始检查蓝牙是否打开
        if (bluetoothAdapter.isEnabled()) {
            btnScanBle.setEnabled(true);
        } else {
            btnScanBle.setEnabled(false);
            // 提示用户打开蓝牙
            Toast.makeText(this, "请先打开蓝牙", Toast.LENGTH_SHORT).show();
            // 可选：直接弹出系统打开蓝牙的对话框
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, 1);
        }

        // 注册蓝牙状态变化监听
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消注册广播接收器，防止内存泄漏
        unregisterReceiver(bluetoothStateReceiver);
    }
}
