package com.huard.serial;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

public class UsbReceiver extends BroadcastReceiver {
    private static final String ACTION_USB_PERMISSION = "com.huard.serial.USB_PERMISSION";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
            UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
            if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                if (accessory != null) {
                    // Permission granted, do something with the accessory
                }
            } else {
                // Permission denied
            }
        }
    }
}

