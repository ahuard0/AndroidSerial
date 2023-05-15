package com.huard.androidserial;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements StatusConnectedListener, StatusTerminalListener {

    private SerialClient client;

    private TextView lblTerminal;
    private TextView lblConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialize();
        connect();
    }

    private Handler statusConnectionHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String message = (String) msg.obj;
            updateConnectionStatus(message);
        }
    };

    private Handler statusTerminalHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            String message = (String) msg.obj;
            updateTerminalStatus(message);
        }
    };

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
        Button btnOnD8 = findViewById(R.id.btnOnD8);
        Button btnOnD9 = findViewById(R.id.btnOnD9);
        Button btnOffD3 = findViewById(R.id.btnOffD3);
        Button btnOffD4 = findViewById(R.id.btnOffD4);
        Button btnOffD5 = findViewById(R.id.btnOffD5);
        Button btnOffD6 = findViewById(R.id.btnOffD6);
        Button btnOffD7 = findViewById(R.id.btnOffD7);
        Button btnOffD8 = findViewById(R.id.btnOffD8);
        Button btnOffD9 = findViewById(R.id.btnOffD9);

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
        btnOnD8.setOnClickListener(v -> onPressOnD8());
        btnOnD9.setOnClickListener(v -> onPressOnD9());
        btnOffD3.setOnClickListener(v -> onPressOffD3());
        btnOffD4.setOnClickListener(v -> onPressOffD4());
        btnOffD5.setOnClickListener(v -> onPressOffD5());
        btnOffD6.setOnClickListener(v -> onPressOffD6());
        btnOffD7.setOnClickListener(v -> onPressOffD7());
        btnOffD8.setOnClickListener(v -> onPressOffD8());
        btnOffD9.setOnClickListener(v -> onPressOffD9());

        btnBroadcastOn.setOnClickListener(v -> onPressBroadcastOn());
        btnBroadcastOff.setOnClickListener(v -> onPressBroadcastOff());
        btnSingleRead.setOnClickListener(v -> onPressSingleRead());
    }

    public void updateTerminalStatus(String msg) {
        lblTerminal.setText(msg);
    }

    public void updateConnectionStatus(String msg) {
        lblConnected.setText(msg);
    }

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

    private void onPressOnD8() {
        client.write("$|_D8_ON\n");
    }

    private void onPressOnD9() {
        client.write("$|_D9_ON\n");
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

    private void onPressOffD8() {
        client.write("$|_D8_OFF\n");
    }

    private void onPressOffD9() {
        client.write("$|_D9_OFF\n");
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