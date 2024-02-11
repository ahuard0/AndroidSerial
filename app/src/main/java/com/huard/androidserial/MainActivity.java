package com.huard.androidserial;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements StatusConnectedListener, StatusTerminalListener {

    private SerialClient client;

    private TextView lblTerminal;
    private TextView lblConnected;

    LineChart lineChart;
    private LineDataSet dataSet;

    private int count = 0;  // Create a counter to track the x-axis value

    private static final int MAX_DATA_POINTS = 300;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lineChart = findViewById(R.id.lineChart);  // get the line chart from the XML file

        initialize();
        connect();
    }

    private final Handler statusConnectionHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String message = (String) msg.obj;
            updateConnectionStatus(message);
        }
    };

    private final Handler statusTerminalHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String message = (String) msg.obj;
            updateTerminalStatus(message);
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void initialize() {
        lblTerminal = findViewById(R.id.lblTerminal);
        lblConnected = findViewById(R.id.lblConnected);

        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnDisconnect = findViewById(R.id.btnDisconnect);

        Button btnOnD3 = findViewById(R.id.btnOnD3);
        Button btnOnD4 = findViewById(R.id.btnOnD4);
        Button btnOnD5 = findViewById(R.id.btnOnD5);
        Button btnOnD6 = findViewById(R.id.btnOnD6);
        Button btnOnD7 = findViewById(R.id.btnOnD7);
        Button btnOffD3 = findViewById(R.id.btnOffD3);
        Button btnOffD4 = findViewById(R.id.btnOffD4);
        Button btnOffD5 = findViewById(R.id.btnOffD5);
        Button btnOffD6 = findViewById(R.id.btnOffD6);
        Button btnOffD7 = findViewById(R.id.btnOffD7);

        Button btnBroadcastOn = findViewById(R.id.btnBroadcastOn);
        Button btnBroadcastOff = findViewById(R.id.btnBroadcastOff);
        Button btnSingleRead = findViewById(R.id.btnSingleRead);

        btnConnect.setOnClickListener(v -> onPressConnect());
        btnDisconnect.setOnClickListener(v -> onPressDisconnect());

        btnOnD3.setOnClickListener(v -> onPressOnD3());
        btnOnD4.setOnClickListener(v -> onPressOnD4());
        btnOnD5.setOnClickListener(v -> onPressOnD5());
        btnOnD6.setOnClickListener(v -> onPressOnD6());
        btnOnD7.setOnClickListener(v -> onPressOnD7());
        btnOffD3.setOnClickListener(v -> onPressOffD3());
        btnOffD4.setOnClickListener(v -> onPressOffD4());
        btnOffD5.setOnClickListener(v -> onPressOffD5());
        btnOffD6.setOnClickListener(v -> onPressOffD6());
        btnOffD7.setOnClickListener(v -> onPressOffD7());

        btnBroadcastOn.setOnClickListener(v -> onPressBroadcastOn());
        btnBroadcastOff.setOnClickListener(v -> onPressBroadcastOff());
        btnSingleRead.setOnClickListener(v -> onPressSingleRead());
    }

    public void updateTerminalStatus(String msg) {
        lblTerminal.setText(msg);
        updateChart(msg);
    }

    public void updateChart(String msg) {
        // Process and add the received messages to the entries list
        // Format "#|ID|TYPE|FRAME|PINS|VALUES|CHKSUM"
        // Example: "#|-13678|MON|25|0,1|535,0|1692"
        // Split the received message and extract the pin values
        // Ignore messages that don't start with "#" or have type "MON"
        if (!msg.startsWith("#") || !msg.contains("|MON|")) {
            return;
        }

        String[] messageParts = msg.split("\\|");  // Parse message
        if (messageParts.length >= 7) {  // Extract the pin values from the message parts
            String[] pins = messageParts[4].split(",");
            String[] values = messageParts[5].split(",");

            String checksum = messageParts[6].trim();  // Remove leading and trailing whitespace

            if (!checkChecksum(msg, checksum))
                return;

            // Check if it's the first |MON| message
            if (dataSet == null) {
                if (pins.length == 0) {
                    return;  // Ignore messages without pin values
                }
                initializeChart();  // Initialize the dataSets list based on the number of pins
            }

            try {
                //float pin = Float.parseFloat(pins[0]);
                float value = Float.parseFloat(values[0]);

                // Get the entries list for the current pin
                List<Entry> pinEntries = dataSet.getValues();

                // Create a new Entry with the updated value and x-coordinate
                Entry newEntry = new Entry(count, value);

                // Add the new entry to the pin's entries list
                pinEntries.add(newEntry);

                count++;

                // Sort the entries based on their x-values
                //Collections.sort(pinEntries, new EntryXComparator());

                // Check if the number of data points exceeds the maximum limit
                if (pinEntries.size() > MAX_DATA_POINTS) {
                    // Remove the oldest entry to maintain the rolling plot
                    pinEntries.remove(0);
                }

                dataSet.setValues(pinEntries);


            } catch (NumberFormatException e) {
                Log.d("AndroidSerial", "Parsing Error: " + msg);
            }

            // Create a new LineData object with the updated dataSets
            LineData lineData = new LineData(dataSet);

            // Set the updated LineData to the chart
            lineChart.setData(lineData);

            // Notify the chart that the data has changed
            lineChart.notifyDataSetChanged();

            // Refresh the chart
            lineChart.invalidate();
        }
    }

    private void initializeChart() {
        dataSet = new LineDataSet(new ArrayList<>(), "Pin 0");

        // Customize the attributes of the LineDataSet
        dataSet.setColor(Color.GREEN);
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.RED);  // Alternate colors between red and yellow
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);  // Customize axis labels based on the pin index

        // Get the left y-axis of the chart
        YAxis yAxis = lineChart.getAxisLeft();

        // Set the minimum and maximum values for the y-axis
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(1023f);
    }

    private boolean checkChecksum(String message, String checksum) {
        // Remove the checksum field from the message
        String messageWithoutChecksum = message.substring(0, message.lastIndexOf("|"));

        // Compute the checksum for the message without the checksum field
        int computedChecksum = 0;
        for (char c : messageWithoutChecksum.toCharArray()) {
            computedChecksum += c;
        }

        try {
            int expectedChecksum = Integer.parseInt(checksum);  // Convert the checksum from string to integer
            return computedChecksum == expectedChecksum;  // Compare the computed checksum with the expected checksum
        } catch (NumberFormatException e) {
            return false;  // Checksum was not a number -> reject
        }
    }

    public void updateConnectionStatus(String msg) {
        lblConnected.setText(msg);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void onPressConnect() {
        connect();
    }

    private void onPressDisconnect() {
        disconnect();
    }

    private void onPressOnD3() {
        client.write("$|_D3_ON\n");
    }

    private void onPressOnD4() {
        client.write("$|_D4_ON\n");
    }

    private void onPressOnD5() {
        client.write("$|_D5_ON\n");
    }

    private void onPressOnD6() {
        client.write("$|_D6_ON\n");
    }

    private void onPressOnD7() {
        client.write("$|_D7_ON\n");
    }

    private void onPressOffD3() {
        client.write("$|_D3_OFF\n");
    }

    private void onPressOffD4() {
        client.write("$|_D4_OFF\n");
    }

    private void onPressOffD5() {
        client.write("$|_D5_OFF\n");
    }

    private void onPressOffD6() {
        client.write("$|_D6_OFF\n");
    }

    private void onPressOffD7() {
        client.write("$|_D7_OFF\n");
    }

    private void onPressBroadcastOff() {
        client.write("$|_BROADCAST_OFF\n");
    }

    private void onPressBroadcastOn() {
        client.write("$|_BROADCAST_ON\n");
    }

    private void onPressSingleRead() {
        client.write("$|_SINGLE_READ\n");
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void connect() {
        if (client == null)
            client = new SerialClient(getApplicationContext(), statusTerminalHandler, statusConnectionHandler);
    }

    private void disconnect() {
        try {
            client.close();
        } catch (NullPointerException e) {
            Log.e("USB", "Attempted to disconnect, but no connection was found");
        }
        client = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onResume() {
        super.onResume();
        connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }
}