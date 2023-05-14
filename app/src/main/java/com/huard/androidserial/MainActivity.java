package com.huard.androidserial;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.huard.serial.USB_PERMISSION";
    private static final int USB_VENDOR_ID = 10755; // Arduino
    private static final int USB_PRODUCT_ID = 67;
    private PendingIntent intentPermissionUSB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SerialMonitor.manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        intentPermissionUSB = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);

        Button btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(v -> requestUsbPermission());
    }

    private void requestUsbPermission() {
        HashMap<String, UsbDevice> deviceList = SerialMonitor.manager.getDeviceList();
        if (!deviceList.isEmpty()) {
            for (UsbDevice device : deviceList.values()) {
                if (device.getVendorId() == USB_VENDOR_ID && device.getProductId() == USB_PRODUCT_ID) {
                    SerialMonitor.device = device;
                    break;
                }
            }
            if (SerialMonitor.device != null) {
                SerialMonitor.manager.requestPermission(SerialMonitor.device, intentPermissionUSB);
            }
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (SerialMonitor.device == null) {  // permission denied
                        SerialMonitor.logDiagnostics();
                        Log.d("USB", "Permission denied for device");
                    }
                    else {  // permission granted, do something with the device

                        SerialMonitor monitor = new SerialMonitor();
                        monitor.start();

                    }
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
    }

    @Override
    protected void onDestroy() {
        SerialMonitor.closeDevice();
        super.onDestroy();
    }
}