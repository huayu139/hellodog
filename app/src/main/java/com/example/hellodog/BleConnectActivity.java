package com.example.hellodog;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleDevice;

import com.polidea.rxandroidble3.RxBleConnection;
import com.polidea.rxandroidble3.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble3.exceptions.BleGattException;

import java.util.Arrays;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * BLE 设备连接和数据通信 Activity
 * 使用异步方式处理数据接收和发送，以16进制格式显示数据
 */
public class BleConnectActivity extends AppCompatActivity {

    private static final String TAG = "BleConnectActivity";
    private static final int REQUEST_CODE_BLE_PERMISSIONS = 102;

    // BLE 服务和特征 UUID（根据实际设备修改）
    private static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID WRITE_CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final UUID NOTIFY_CHARACTERISTIC_UUID = UUID.fromString("0000ffe2-0000-1000-8000-00805f9b34fb");

    private RxBleClient rxBleClient;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private RxBleConnection rxBleConnection;
    private String deviceName;
    private String deviceAddress;

    // UI 组件
    private TextView deviceInfoText;
    private TextView receivedDataView;
    private EditText inputCommand;
    private Button connectButton;
    private Button sendButton;
    private Button disconnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_connect);

        // 初始化 RxBleClient
        rxBleClient = RxBleClient.create(this);

        // 获取传递的设备信息
        deviceName = getIntent().getStringExtra("device_name");
        deviceAddress = getIntent().getStringExtra("device_address");
        
        if (deviceAddress == null) {
            Toast.makeText(this, "设备地址为空", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 初始化 UI
        deviceInfoText = findViewById(R.id.device_info);
        receivedDataView = findViewById(R.id.received_data_view);
        inputCommand = findViewById(R.id.input_command);
        connectButton = findViewById(R.id.connect_button);
        sendButton = findViewById(R.id.send_button);
        disconnectButton = findViewById(R.id.disconnect_button);

        // 设置设备信息
        deviceInfoText.setText(String.format("设备: %s\n地址: %s", 
            deviceName != null ? deviceName : "未知设备", deviceAddress));

        // 设置按钮点击事件
        connectButton.setOnClickListener(v -> connectToDevice());
        sendButton.setOnClickListener(v -> sendHexCommand());
        disconnectButton.setOnClickListener(v -> disconnectDevice());
    }

    @SuppressLint("CheckResult")
    private void connectToDevice() {
        if (!checkAndRequestPermissions()) {
            return;
        }

        RxBleDevice device = RxBleClient.create(this).getBleDevice(deviceAddress);
        appendLog("正在连接设备...");

        Disposable connectionDisposable = device.establishConnection(false)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                connection -> {
                    rxBleConnection = connection;
                    appendLog("✅ 连接成功！");
                    
                    // 更新按钮状态
                    connectButton.setEnabled(false);
                    sendButton.setEnabled(true);
                    disconnectButton.setEnabled(true);
                    
                    // 开始订阅通知
                    startReceivingData();
                },
                throwable -> {
                    String errorMsg = "❌ 连接失败: " + getErrorMessage(throwable);
                    appendLog(errorMsg);
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                }
            );

        compositeDisposable.add(connectionDisposable);
    }

    @SuppressLint("CheckResult")
    private void startReceivingData() {
        if (rxBleConnection == null) {
            appendLog("⚠️ 连接为空，无法接收数据");
            return;
        }

        appendLog("📡 开始订阅通知...");

        Disposable notificationDisposable = rxBleConnection.setupNotification(NOTIFY_CHARACTERISTIC_UUID)
            .flatMap(notificationObservable -> notificationObservable)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                bytes -> {
                    // 以16进制格式显示接收到的数据
                    String hexData = bytesToHex(bytes);
                    appendLog("📥 收到数据 (16进制): " + hexData);
                },
                throwable -> {
                    String errorMsg = "⚠️ 通知订阅失败: " + getErrorMessage(throwable);
                    appendLog(errorMsg);
                }
            );

        compositeDisposable.add(notificationDisposable);
    }

    @SuppressLint("CheckResult")
    private void sendHexCommand() {
        if (rxBleConnection == null) {
            Toast.makeText(this, "请先连接设备", Toast.LENGTH_SHORT).show();
            return;
        }

        String hexCommand = inputCommand.getText().toString().trim();
        if (hexCommand.isEmpty()) {
            Toast.makeText(this, "请输入要发送的16进制数据", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            byte[] dataToSend = hexStringToBytes(hexCommand);
            appendLog("📤 准备发送数据: " + hexCommand + " (" + Arrays.toString(dataToSend) + ")");

            Disposable sendDisposable = rxBleConnection.writeCharacteristic(WRITE_CHARACTERISTIC_UUID, dataToSend)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    characteristicValue -> {
                        String sentHex = bytesToHex(characteristicValue);
                        appendLog("✅ 发送成功! (16进制: " + sentHex + ")");
                    },
                    throwable -> {
                        String errorMsg = "❌ 发送失败: " + getErrorMessage(throwable);
                        appendLog(errorMsg);
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                    }
                );

            compositeDisposable.add(sendDisposable);
            
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "无效的16进制数据格式", Toast.LENGTH_SHORT).show();
            appendLog("⚠️ 无效的16进制数据: " + hexCommand);
        }
    }

    private void disconnectDevice() {
        if (rxBleConnection != null) {
            compositeDisposable.clear();
            rxBleConnection = null;
            
            appendLog("🔌 已断开连接");
            
            // 重置按钮状态
            connectButton.setEnabled(true);
            sendButton.setEnabled(false);
            disconnectButton.setEnabled(false);
        }
    }

    /**
     * 将字节数组转换为16进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex).append(' ');
        }
        return hexString.toString().trim().toUpperCase();
    }

    /**
     * 将16进制字符串转换为字节数组
     */
    private byte[] hexStringToBytes(String hexString) {
        // 移除所有空格和特殊字符
        String cleanHex = hexString.replaceAll("[^0-9A-Fa-f]", "");
        
        if (cleanHex.length() % 2 != 0) {
            throw new IllegalArgumentException("16进制字符串必须是偶数长度");
        }
        
        byte[] bytes = new byte[cleanHex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            String byteString = cleanHex.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(byteString, 16);
        }
        return bytes;
    }

    /**
     * 追加日志到接收数据显示框
     */
    private void appendLog(String message) {
        runOnUiThread(() -> {
            String currentLog = receivedDataView.getText().toString();
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            receivedDataView.setText("[" + timestamp + "] " + message + "\n" + currentLog);
        });
    }

    /**
     * 获取错误消息
     */
    private String getErrorMessage(Throwable throwable) {
        if (throwable instanceof BleDisconnectedException) {
            return "设备已断开连接";
        } else if (throwable instanceof BleGattException) {
            BleGattException gattException = (BleGattException) throwable;
            return "BLE Gatt 错误: " + gattException.getMessage() + " - 状态码: " + gattException.getStatus();
        } else {
            return throwable.getMessage() != null ? throwable.getMessage() : "未知错误";
        }
    }

    private boolean checkAndRequestPermissions() {
        String[] perms = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
        };
        
        if (!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(
                    this,
                    "需要BLE权限来连接和通信",
                    REQUEST_CODE_BLE_PERMISSIONS,
                    perms
            );
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectDevice();
        compositeDisposable.dispose();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}