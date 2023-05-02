package com.huard.androidserial;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.huard.serial.USB_PERMISSION";
    private static final int USB_VENDOR_ID = 10755; // Arduino
    private static final int USB_PRODUCT_ID = 67;
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private PendingIntent mPermissionIntent;

    private UsbDeviceConnection connection;
    private UsbEndpoint inputEndpoint;
    private UsbEndpoint outputEndpoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);

        Button mButton = findViewById(R.id.button);
        mButton.setOnClickListener(v -> requestUsbPermission());
    }

    private void requestUsbPermission() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        if (!deviceList.isEmpty()) {
            for (UsbDevice device : deviceList.values()) {
                if (device.getVendorId() == USB_VENDOR_ID && device.getProductId() == USB_PRODUCT_ID) {
                    mUsbDevice = device;
                    break;
                }
            }
            if (mUsbDevice != null) {
                mUsbManager.requestPermission(mUsbDevice, mPermissionIntent);
            }
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    if (mUsbDevice != null) {  // permission granted, do something with the device

                        UsbAccessory[] accessories = mUsbManager.getAccessoryList();
                        Log.d("USB", "Accessories: " + accessories);

                        connection = mUsbManager.openDevice(mUsbDevice);  // USB HOST

                        boolean perm = mUsbManager.hasPermission(mUsbDevice);
                        Log.d("USB", "Has Permission: " + perm);

                        Log.d("USB", "Permission granted for device " + mUsbDevice.toString());
                        Log.d("USB", "Connection: " + connection);

                        for (int i = 0; i < mUsbDevice.getInterfaceCount(); i++) {
                            UsbInterface usbInterface = mUsbDevice.getInterface(i);
                            for (int j = 0; j < usbInterface.getEndpointCount(); j++) {
                                UsbEndpoint usbEndpoint = usbInterface.getEndpoint(j);
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


                        UsbInterface usbInterface = mUsbDevice.getInterface(1);
                        Log.d("USB", "Interface: " + usbInterface);

                        String name = usbInterface.getName();
                        Log.d("USB", "Interface Name: " + name);

                        boolean claimed = connection.claimInterface(usbInterface, true);
                        Log.d("USB", "Interface Claimed: " + claimed);

                        UsbEndpoint usbEndpoint = usbInterface.getEndpoint(0);
                        Log.d("USB", "Endpoint: " + usbEndpoint);

                        int contents = usbEndpoint.describeContents();
                        Log.d("USB", "DescribeContents: " + contents);

                        int interval = usbEndpoint.getInterval();
                        Log.d("USB", "Interval: " + interval);

                        int attributes = usbEndpoint.getAttributes();
                        Log.d("USB", "Attributes: " + attributes);


                        /*
                        int RQSID_SET_LINE_CODING = 0x20;
                        int RQSID_SET_CONTROL_LINE_STATE = 0x22;

                        int usbResult;
                        usbResult = connection.controlTransfer(0x21, // requestType
                                RQSID_SET_CONTROL_LINE_STATE, // SET_CONTROL_LINE_STATE
                                0, // value
                                0, // index
                                null, // buffer
                                0, // length
                                0); // timeout
                        Log.d("USB", "usbResult: " + usbResult);

                        // baud rate = 9600
                        // 8 data bit
                        // 1 stop bit
                        byte[] encodingSetting = new byte[] { (byte) 0x80, 0x25, 0x00,
                                0x00, 0x00, 0x00, 0x08 };
                        usbResult = connection.controlTransfer(0x21, // requestType
                                RQSID_SET_LINE_CODING, // SET_LINE_CODING
                                0, // value
                                0, // index
                                encodingSetting, // buffer
                                7, // length
                                0); // timeout
                        Log.d("USB", "usbResult: " + usbResult);
                        */

                        /*
                        UsbEndpoint interruptEndpoint = null;
                        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                            UsbEndpoint endpoint = usbInterface.getEndpoint(i);
                            if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
                                interruptEndpoint = endpoint;
                                break;
                            }
                        }
                        Log.d("USB", "InputEndpoint: " + interruptEndpoint);

                        assert interruptEndpoint != null;
                        byte[] buffer = new byte[interruptEndpoint.getMaxPacketSize()];

                        while (true) {
                            int bytesTransferred = connection.bulkTransfer(interruptEndpoint, buffer, buffer.length, 1000);
                            Log.d("USB", "Bytes Transferred: " + bytesTransferred);
                            if (bytesTransferred > 0) {
                                String message = new String(buffer, 0, bytesTransferred, StandardCharsets.UTF_8);
                                // Process the incoming data here.
                                Log.d("USB", "Serial Message: " + message);
                                break;
                            }
                        }
                        connection.releaseInterface(usbInterface);
                        connection.close();
                         */


                        // find endpoints
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
                        Log.d("USB", "InputEndpoint: " + inputEndpoint);
                        Log.d("USB", "OutputEndpoint: " + outputEndpoint);

                        byte[] buffer = new byte[1024];
                        int receivedBytes = connection.bulkTransfer(inputEndpoint, buffer, buffer.length, 1000);
                        if (receivedBytes < 0) {
                            // Error occurred
                            Log.d("USB", "Received bytes (error): " + receivedBytes);
                        } else if (receivedBytes == 0) {
                            // No data received within the timeout period
                            Log.d("USB", "Received bytes (timeout): " + receivedBytes);
                        } else {
                            // Convert the received data to a string
                            String receivedData = new String(buffer, 0, receivedBytes);
                            Log.d("USB", "Received data: " + receivedData);
                        }


                        /*
                        byte[] data = "Hello, Arduino!".getBytes();
                        int sentBytes = connection.bulkTransfer(outputEndpoint, data, data.length, 1000);
                        Log.d("USB", "sentBytes: " + sentBytes);
                         */


                        connection.releaseInterface(usbInterface);
                        connection.close();

                    } else {
                        // permission denied
                        Log.d("USB", "Permission denied for device");
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
}