package com.huard.androidserial;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class SerialMonitor extends Thread {
    public static UsbEndpoint inputEndpoint;
    public static UsbEndpoint outputEndpoint;
    public static UsbDeviceConnection connection;

    public static UsbDevice device;
    public static UsbManager manager;
    public static UsbInterface usbInterface;

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
        SerialMonitor.findInterface();
        SerialMonitor.findEndpoints();
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

    private void write(String command) {
        if (device != null) {  // permission granted, do something with the device

            byte[] buffer = command.getBytes();
            int numBytesWritten = connection.bulkTransfer(outputEndpoint, buffer, buffer.length, 1000);
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

    public static void closeDevice() {
        if (connection != null) {
            if (usbInterface != null)
                connection.releaseInterface(usbInterface);
            connection.close();
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[64];
        int idx_header;
        int idx_footer;

        StringBuilder searchStr = new StringBuilder();
        String msg;

        SerialMonitor.initialize();

        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        UsbSerialDriver driver = availableDrivers.get(0);
        connection = manager.openDevice(driver.getDevice());

        List<UsbSerialPort> ports = driver.getPorts();
        UsbSerialPort port = ports.get(0);

        try {
            port.open(connection);
            port.setDTR(true);
            port.setRTS(true);
            port.setParameters(115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        while(true) {
            synchronized (this) {

                int receivedBytes = connection.bulkTransfer(inputEndpoint, buffer, buffer.length, 1000);


                if (receivedBytes < 0) { // Error occurred
                    Log.d("USB", "Received bytes (error): " + receivedBytes);
                } else {
                    String receivedData = new String(buffer, 0, receivedBytes);  // Convert the received data to a string

                    searchStr.append(receivedData);
                    idx_header = searchStr.indexOf("#");

                    if (idx_header >= 0) { // truncate anything before the current header
                        searchStr = new StringBuilder(searchStr.substring(idx_header));
                    }

                    idx_header = searchStr.indexOf("#");
                    idx_footer = searchStr.indexOf("\n");

                    if (idx_footer >= 0) {
                        if (idx_header >= 0) { // complete message was found
                            try {
                                msg = searchStr.substring(idx_header, idx_footer);
                            } catch (StringIndexOutOfBoundsException e) {
                                Log.e("USB", "Index Out of Bounds Error: " + searchStr);
                                return;
                            }
                            Log.d("USB", "Received message: " + msg);
                        }
                        searchStr = new StringBuilder();
                    }
                }
            }
        }
    }
}
