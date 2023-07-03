package com.huard.androidserial;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.HashMap;


public class SerialClient implements AutoCloseable {
    public static final String ACTION_USB_PERMISSION = "com.huard.androidserial.USB_PERMISSION";
    private static PendingIntent intentPermissionUSB;
    private static final int[] USB_VENDOR_IDs = {9025, 10755}; // Arduino
    private static final int[] USB_PRODUCT_IDs = {67}; // Arduino Uno
    public static UsbEndpoint inputEndpoint;
    public static UsbEndpoint outputEndpoint;
    public static UsbDeviceConnection connection;
    public static UsbDevice device;
    public static UsbManager manager;
    public static UsbInterface usbInterface;
    public static SerialMonitor monitor;
    public static SerialClient client;

    public static Handler terminalHandler;
    public static Handler connectionHandler;

    public SerialClient(@NonNull Context context, Handler terminalHandler, Handler connectionHandler) {
        SerialClient.terminalHandler = terminalHandler;
        SerialClient.connectionHandler = connectionHandler;
        client = this;

        manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        intentPermissionUSB = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);

        context.registerReceiver(eventPermissionUSB, new IntentFilter(ACTION_USB_PERMISSION));
        context.registerReceiver(eventDisconnectUSB, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        context.registerReceiver(eventConnectUSB, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));

        requestUsbPermission();
    }

    public static void requestUsbPermission() {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        if (!deviceList.isEmpty()) {
            for (UsbDevice device : deviceList.values()) {
                for (int vendorId : USB_VENDOR_IDs) {
                    for (int productId : USB_PRODUCT_IDs) {
                        if (device.getVendorId() == vendorId && device.getProductId() == productId) {
                            SerialClient.device = device;
                            break;
                        }
                    }
                }
            }
            if (device != null) {
                manager.requestPermission(device, intentPermissionUSB);
            }
        }
    }

    public static final BroadcastReceiver eventPermissionUSB = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (device == null) {  // permission denied
                        logDiagnostics();
                        Log.d("USB", "Permission denied for device");
                    } else {  // permission granted,
                        if (monitor == null) {  // start a new serial monitor
                            monitor = new SerialMonitor(client);
                            monitor.start();
                            updateConnectionStatus("Connected");
                            updateTerminalStatus("Listening...");
                        }
                        else {
                            if (!monitor.isAlive()) {  // monitor thread stopped, but still in memory
                                monitor.close();
                                monitor = null;  // garbage collect old monitor
                                monitor = new SerialMonitor(client);
                                monitor.start();
                                updateConnectionStatus("Connected");
                                updateTerminalStatus("Listening...");
                            }
                        }
                    }
                }
            }
        }
    };

    public static void updateConnectionStatus(String message) {
        Message msg = connectionHandler.obtainMessage();
        msg.obj = message;
        connectionHandler.sendMessage(msg);
    }

    public static void updateTerminalStatus(String message) {
        Message msg = terminalHandler.obtainMessage();
        msg.obj = message;
        terminalHandler.sendMessage(msg);
    }

    public static final BroadcastReceiver eventDisconnectUSB = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();
            if (action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                if (monitor != null)
                    monitor.quit();  // gracefully shut down
                client.close();
                updateConnectionStatus("Disconnected");
                updateTerminalStatus("Closed Connection");
            }
        }
    };

    public static final BroadcastReceiver eventConnectUSB = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String action = intent.getAction();
            if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED))
                requestUsbPermission();
        }
    };

    public static void logDiagnostics() {
        if (device != null) {  // permission granted, do something with the device

            boolean perm = manager.hasPermission(device);
            Log.d("USB", "Has Permission: " + perm);

            Log.d("USB", "Permission granted for device " + device.toString());
            Log.d("USB", "Connection: " + connection);

            for (int i = 0; i < device.getInterfaceCount(); i++) {
                UsbInterface deviceInterface = device.getInterface(i);
                for (int j = 0; j < deviceInterface.getEndpointCount(); j++) {
                    UsbEndpoint usbEndpoint = deviceInterface.getEndpoint(j);
                    String type = "";
                    String direction = "";
                    if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                        type = "USB_ENDPOINT_XFER_INT";
                    } else if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        type = "USB_ENDPOINT_XFER_BULK";
                    } else if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_CONTROL) {
                        type = "USB_ENDPOINT_XFER_CONTROL";
                    } else if (usbEndpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_ISOC) {
                        type = "USB_ENDPOINT_XFER_ISOC";
                    } else {
                        Log.d("USB", "Unknown Type");
                    }
                    if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                        direction = "USB_DIR_IN";
                    } else if (usbEndpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                        direction = "USB_DIR_OUT";
                    } else {
                        Log.d("USB", "Unknown Direction");
                    }
                    Log.d("USB", String.format("(Interface, Endpoint): %d, %d, %s, %s", i, j, type, direction));
                }
            }

            Log.d("USB", "Interface: " + usbInterface);

            String name = usbInterface.getName();
            Log.d("USB", "Interface Name: " + name);

            Log.d("USB", "InputEndpoint: " + inputEndpoint);
            Log.d("USB", "OutputEndpoint: " + outputEndpoint);

            int contents = inputEndpoint.describeContents();
            Log.d("USB", "DescribeContents(Input Endpoint): " + contents);

            int interval = inputEndpoint.getInterval();
            Log.d("USB", "Interval(Input Endpoint): " + interval);

            int attributes = inputEndpoint.getAttributes();
            Log.d("USB", "Attributes(Input Endpoint): " + attributes);

        } else {
            // permission denied
            Log.d("USB", "Permission denied for device");
        }
    }

    public static void initialize() {
        findInterface();
        findEndpoints();
    }

    private static void findInterface() {
        usbInterface = device.getInterface(1);
    }

    private static void findEndpoints() {
        for (int j=0; j<device.getInterfaceCount(); j++) {
            UsbInterface usbInterface = device.getInterface(j);
            for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                UsbEndpoint endpoint = usbInterface.getEndpoint(i);
                if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                        inputEndpoint = endpoint;
                    } else if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                        outputEndpoint = endpoint;
                    }
                }
            }
        }
    }

    public void write(String command) {
        if (device != null) {  // permission granted, do something with the device
            synchronized (this) {
                byte[] buffer = command.getBytes();
                int numBytesWritten = connection.bulkTransfer(outputEndpoint, buffer, buffer.length, 5000);
                if (numBytesWritten < 0) {
                    // Error occurred
                    Log.d("USB", "Wrote bytes (error): " + numBytesWritten);
                } else if (numBytesWritten == 0) {
                    // No data written within the timeout period
                    Log.d("USB", "Wrote bytes (timeout): " + numBytesWritten);
                } else {
                    // Convert the received data to a string
                    String writtenData = new String(buffer, 0, numBytesWritten);
                    Log.d("USB", "Wrote data: " + writtenData);
                }
            }
        }
    }

    @Override
    public void close() throws NullPointerException {
        if (monitor != null)
            monitor.quit();
        if (connection != null) {
            connection.releaseInterface(usbInterface);
            connection.close();
            connection = null;
            updateConnectionStatus("Disconnected");
            updateTerminalStatus("Closed Connection");
        }
    }
}
