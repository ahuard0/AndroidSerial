package com.huard.androidserial;

import android.graphics.Color;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class ChartManager {

    private final LineChart lineChart;
    private LineDataSet dataSet;
    private int count = 0;  // Create a counter to track the x-axis value
    private static final int MAX_DATA_POINTS = 300;

    public ChartManager(LineChart lineChart) {
        this.lineChart = lineChart;
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

}
