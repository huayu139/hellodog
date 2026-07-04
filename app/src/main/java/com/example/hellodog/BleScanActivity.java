package com.example.hellodog;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.polidea.rxandroidble3.RxBleClient;
import com.polidea.rxandroidble3.RxBleDevice;
import com.polidea.rxandroidble3.scan.ScanResult;
import com.polidea.rxandroidble3.scan.ScanSettings;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * 示例 Activity：扫描并显示附近的 BLE 设备
 */
public class BleScanActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    private static final String TAG = "BleScanActivity";
    private static final int REQUEST_CODE_BLE_PERMISSIONS = 100;

    private RxBleClient rxBleClient;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private ArrayAdapter<String> deviceAdapter;
    private List<String> deviceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_scan);

        // 初始化 RxBleClient
        rxBleClient = RxBleClient.create(this);

        // 初始化 ListView 和适配器
        ListView deviceListView = findViewById(R.id.device_list_view);
        deviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        deviceListView.setAdapter(deviceAdapter);

        // 设置扫描按钮点击事件
        Button scanButton = findViewById(R.id.scan_button);
        scanButton.setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                startScan();
            }
        });
    }

    private void startScan() {
        // 清空之前的扫描结果
        deviceList.clear();
        deviceAdapter.notifyDataSetChanged();

        // 扫描 BLE 设备
        Disposable scanDisposable = rxBleClient.scanBleDevices(
                new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build()
        )
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
                scanResult -> {
                    // 扫描到设备时的回调
                    RxBleDevice device = scanResult.getBleDevice();
                    BluetoothDevice bluetoothDevice = device.getBluetoothDevice();
                    String deviceName = bluetoothDevice.getName() != null ? bluetoothDevice.getName() : "Unknown Device";
                    String deviceInfo = String.format("%s (%s)", deviceName, bluetoothDevice.getAddress());
                    
                    if (!deviceList.contains(deviceInfo)) {
                        deviceList.add(deviceInfo);
                        deviceAdapter.notifyDataSetChanged();
                    }
                },
                throwable -> {
                    // 扫描错误
                    Log.e(TAG, "扫描错误: " + throwable.getMessage());
                    Toast.makeText(this, "扫描错误: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
                }
        );

        // 添加到 CompositeDisposable 管理
        compositeDisposable.add(scanDisposable);

        // 点击设备列表项，跳转到连接界面
        ListView deviceListView = findViewById(R.id.device_list_view);
        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDeviceInfo = deviceList.get(position);
            String[] parts = selectedDeviceInfo.split(" \((\w+)\)");
            String deviceName = parts[0];
            String deviceAddress = parts[1];
            
            // 跳转到连接界面
            Intent intent = new Intent(BleScanActivity.this, BleConnectActivity.class);
            intent.putExtra("device_name", deviceName);
            intent.putExtra("device_address", deviceAddress);
            startActivity(intent);
        });

        // 10 秒后自动停止扫描
        compositeDisposable.add(io.reactivex.rxjava3.core.Observable.timer(10, java.util.concurrent.TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        aLong -> {
                            stopScan();
                            Toast.makeText(this, "扫描完成", Toast.LENGTH_SHORT).show();
                        }
                ));
    }
    }

    private void stopScan() {
        compositeDisposable.clear();
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
                    "需要 BLE 权限来扫描和连接设备",
                    REQUEST_CODE_BLE_PERMISSIONS,
                    perms
            );
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        if (requestCode == REQUEST_CODE_BLE_PERMISSIONS) {
            startScan();
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (requestCode == REQUEST_CODE_BLE_PERMISSIONS) {
            Toast.makeText(this, "权限被拒绝，无法扫描 BLE 设备", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 确保在 Activity 销毁时清理资源
        compositeDisposable.clear();
    }
}
