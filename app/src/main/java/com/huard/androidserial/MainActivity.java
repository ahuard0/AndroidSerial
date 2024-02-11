package com.huard.androidserial;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;

public class MainActivity extends AppCompatActivity implements StatusConnectedListener, StatusTerminalListener {

    private TextView lblTerminal;
    private TextView lblConnected;

    private ChartManager chartManager;
    private ConnectionManager connectionManager;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LineChart lineChart = findViewById(R.id.lineChart);  // get the line chart from the XML file
        chartManager = new ChartManager(lineChart);
        connectionManager = new ConnectionManager(this, statusTerminalHandler, statusConnectionHandler);

        initialize();
        connectionManager.connect();
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

    @Override
    public void updateTerminalStatus(String msg) {
        lblTerminal.setText(msg);
        chartManager.updateChart(msg);
    }

    @Override
    public void updateConnectionStatus(String msg) {
        lblConnected.setText(msg);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void onPressConnect() {
        connectionManager.connect();
    }

    private void onPressDisconnect() {
        connectionManager.disconnect();
    }

    private void onPressOnD3() {
        connectionManager.client.write("$|_D3_ON\n");
    }

    private void onPressOnD4() {
        connectionManager.client.write("$|_D4_ON\n");
    }

    private void onPressOnD5() {
        connectionManager.client.write("$|_D5_ON\n");
    }

    private void onPressOnD6() {
        connectionManager.client.write("$|_D6_ON\n");
    }

    private void onPressOnD7() {
        connectionManager.client.write("$|_D7_ON\n");
    }

    private void onPressOffD3() {
        connectionManager.client.write("$|_D3_OFF\n");
    }

    private void onPressOffD4() {
        connectionManager.client.write("$|_D4_OFF\n");
    }

    private void onPressOffD5() {
        connectionManager.client.write("$|_D5_OFF\n");
    }

    private void onPressOffD6() {
        connectionManager.client.write("$|_D6_OFF\n");
    }

    private void onPressOffD7() {
        connectionManager.client.write("$|_D7_OFF\n");
    }

    private void onPressBroadcastOff() {
        connectionManager.client.write("$|_BROADCAST_OFF\n");
    }

    private void onPressBroadcastOn() {
        connectionManager.client.write("$|_BROADCAST_ON\n");
    }

    private void onPressSingleRead() {
        connectionManager.client.write("$|_SINGLE_READ\n");
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onResume() {
        super.onResume();
        connectionManager.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        connectionManager.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        connectionManager.disconnect();
    }
}