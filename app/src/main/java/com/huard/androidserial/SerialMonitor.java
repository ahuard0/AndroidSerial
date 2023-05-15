package com.huard.androidserial;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.util.List;

public class SerialMonitor extends Thread implements AutoCloseable {

    private boolean running;
    private StringBuilder searchStr;
    private String msg;

    public Handler terminalHandler;

    private SerialClient client;

    SerialMonitor(SerialClient client) {
        this.client = client;
        this.terminalHandler = SerialClient.terminalHandler;

        SerialClient.initialize();
        running = false;
    }

    public void quit() {
        running = false;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[64];
        int idx_header;
        int idx_footer;

        running = true;

        searchStr = new StringBuilder();
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(SerialClient.manager);
        UsbSerialDriver driver = availableDrivers.get(0);

        if (SerialClient.device == null)
            SerialClient.device = driver.getDevice();

        if (!SerialClient.manager.hasPermission(SerialClient.device)) {
            Log.e("USB", "Permission denied. USB Connection: " + SerialClient.connection);
            return;
        }

        SerialClient.connection = SerialClient.manager.openDevice(SerialClient.device);
        if (SerialClient.connection == null) {
            Log.e("USB", "Could not open connection. USB Connection: " + null);
            return;
        }

        SerialClient.connection.claimInterface(SerialClient.usbInterface, true);

        List<UsbSerialPort> ports = driver.getPorts();
        UsbSerialPort port = ports.get(0);

        try {
            port.open(SerialClient.connection);
            port.setDTR(true);
            port.setRTS(true);
            port.setParameters(115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int iterCount = 0;
        while(running) {
            synchronized (this) {
                iterCount++;
                if (iterCount == 100) {
                    client.write("$|clear\n");  // clear the write buffer
                }

                int receivedBytes = SerialClient.connection.bulkTransfer(SerialClient.inputEndpoint, buffer, buffer.length, 1000);

                if (receivedBytes >= 0) {
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
                            updateTerminalStatus(msg);
                        }
                        searchStr = new StringBuilder();
                    }
                }
            }
        }
        close();
    }

    public void updateTerminalStatus(String message) {
        Message msg = terminalHandler.obtainMessage();
        msg.obj = message;
        terminalHandler.sendMessage(msg);
    }

    @Override
    public void close() {
        searchStr = null;
        msg = null;
    }
}
