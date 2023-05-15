package com.huard.androidserial;

import androidx.annotation.NonNull;
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

    private static final String ACTION_USB_PERMISSION = "com.huard.androidserial.USB_PERMISSION";
    private PendingIntent intentPermissionUSB;
    private static final int USB_VENDOR_ID = 10755; // Arduino
    private static final int USB_PRODUCT_ID = 67; // Arduino Uno

    private static SerialMonitor monitor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SerialMonitor.manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        intentPermissionUSB = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        requestUsbPermission();

        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnOnD3 = findViewById(R.id.btnOnD3);
        Button btnOnD4 = findViewById(R.id.btnOnD4);
        Button btnOnD5 = findViewById(R.id.btnOnD5);
        Button btnOnD6 = findViewById(R.id.btnOnD6);
        Button btnOnD7 = findViewById(R.id.btnOnD7);
        Button btnOnD8 = findViewById(R.id.btnOnD8);
        Button btnOnD9 = findViewById(R.id.btnOnD9);
        Button btnOffD3 = findViewById(R.id.btnOffD3);
        Button btnOffD4 = findViewById(R.id.btnOffD4);
        Button btnOffD5 = findViewById(R.id.btnOffD5);
        Button btnOffD6 = findViewById(R.id.btnOffD6);
        Button btnOffD7 = findViewById(R.id.btnOffD7);
        Button btnOffD8 = findViewById(R.id.btnOffD8);
        Button btnOffD9 = findViewById(R.id.btnOffD9);

        btnConnect.setOnClickListener(v -> requestUsbPermission());
        btnOnD3.setOnClickListener(v -> onPressOnD3());
        btnOnD4.setOnClickListener(v -> onPressOnD4());
        btnOnD5.setOnClickListener(v -> onPressOnD5());
        btnOnD6.setOnClickListener(v -> onPressOnD6());
        btnOnD7.setOnClickListener(v -> onPressOnD7());
        btnOnD8.setOnClickListener(v -> onPressOnD8());
        btnOnD9.setOnClickListener(v -> onPressOnD9());
        btnOffD3.setOnClickListener(v -> onPressOffD3());
        btnOffD4.setOnClickListener(v -> onPressOffD4());
        btnOffD5.setOnClickListener(v -> onPressOffD5());
        btnOffD6.setOnClickListener(v -> onPressOffD6());
        btnOffD7.setOnClickListener(v -> onPressOffD7());
        btnOffD8.setOnClickListener(v -> onPressOffD8());
        btnOffD9.setOnClickListener(v -> onPressOffD9());
    }

    private void onPressOnD3() {
        monitor.write("$|_D3_ON");
    }

    private void onPressOnD4() {
        monitor.write("$|_D4_ON");
    }

    private void onPressOnD5() {
        monitor.write("$|_D5_ON");
    }

    private void onPressOnD6() {
        monitor.write("$|_D6_ON");
    }

    private void onPressOnD7() {
        monitor.write("$|_D7_ON");
    }

    private void onPressOnD8() {
        monitor.write("$|_D8_ON");
    }

    private void onPressOnD9() {
        monitor.write("$|_D9_ON");
    }

    private void onPressOffD3() {
        monitor.write("$|_D3_OFF");
    }

    private void onPressOffD4() {
        monitor.write("$|_D4_OFF");
    }

    private void onPressOffD5() {
        monitor.write("$|_D5_OFF");
    }

    private void onPressOffD6() {
        monitor.write("$|_D6_OFF");
    }

    private void onPressOffD7() {
        monitor.write("$|_D7_OFF");
    }

    private void onPressOffD8() {
        monitor.write("$|_D8_OFF");
    }

    private void onPressOffD9() {
        monitor.write("$|_D9_OFF");
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

    private void launchSerialMonitor() {
        if (monitor != null) {
            if (monitor.isAlive()) {
                return;  // serial monitor is alive, do nothing
            }
        }
        monitor = new SerialMonitor();
        monitor.start();
    }

    private void quitSerialMonitor() {
        if (monitor != null) {
            if (!monitor.isAlive()) {
                monitor.quit();  // gracefully shut down the thread
            }
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (SerialMonitor.device == null) {  // permission denied
                        SerialMonitor.logDiagnostics();
                        Log.d("USB", "Permission denied for device");
                    }
                    else {  // permission granted, do something with the device
                        launchSerialMonitor();
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
        if (SerialMonitor.device != null)
            if (SerialMonitor.manager.hasPermission(SerialMonitor.device))
                launchSerialMonitor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        if (SerialMonitor.device != null)
            if (SerialMonitor.manager.hasPermission(SerialMonitor.device))
                quitSerialMonitor();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
        quitSerialMonitor();
    }
}