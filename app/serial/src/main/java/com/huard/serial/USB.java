package com.huard.serial;

import android.app.Application;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

import java.util.HashMap;
import java.util.Iterator;

public class USB extends Application {
    public String getDevicesToString(Context context) {
        System.out.println("Creating USB Manager Object");
        UsbManager manager = (UsbManager)getSystemService(Context.USB_SERVICE);
        System.out.println("Done Creating USB Manager Object");

        // TODO:  Ask for permission to use USB first

        // TODO:  Try using USB Accessory instead of host

        //AndroidJavaException: java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.Object android.content.Context.getSystemService(java.lang.String)' on a null object reference
        //04-12 01:33:14.665 20605 20624 E Unity   : java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.Object android.content.Context.getSystemService(java.lang.String)' on a null object reference
        //04-12 01:33:14.665 20605 20624 E Unity   :      at android.content.ContextWrapper.getSystemService(ContextWrapper.java:900)
        //04-12 01:33:14.665 20605 20624 E Unity   :      at com.huard.serial.USB.getDevicesToString(USB.java:16)


        System.out.println("Getting Devices To String...");
        StringBuilder devices_str = new StringBuilder();
        System.out.println("Created String Builder...");

        System.out.println("Created USB Manager...");
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        System.out.println("Got HashMap..");
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        System.out.println("Iterator retrieved...");
        while(deviceIterator.hasNext()) {
            System.out.println("Has Next 1...");
            UsbDevice device = deviceIterator.next();
            System.out.println("Got device...");
            devices_str.append(device.getDeviceName());
            System.out.println(device.getDeviceName());
            if (deviceIterator.hasNext()) {
                System.out.println("Has Next 2...");
                devices_str.append(", ");
            }
        }
        System.out.println(devices_str);
        return devices_str.toString();
    }

    public UsbDevice getDeviceByName(Context context, String deviceName) {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        return deviceList.get(deviceName);
    }
}