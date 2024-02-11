package com.huard.androidserial;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class ConnectionManager {
    public SerialClient client;
    private final Handler statusTerminalHandler;
    private final Handler statusConnectionHandler;
    private final Context applicationContext;

    public ConnectionManager(Context applicationContext, Handler statusTerminalHandler, Handler statusConnectionHandler) {
        this.applicationContext = applicationContext;
        this.statusTerminalHandler = statusTerminalHandler;
        this.statusConnectionHandler = statusConnectionHandler;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void connect() {
        if (client == null)
            client = new SerialClient(applicationContext, statusTerminalHandler, statusConnectionHandler);
    }

    public void disconnect() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                Log.e("ConnectionManager", "Error occurred while disconnecting", e);
            } finally {
                client = null;
            }
        } else {
            Log.e("ConnectionManager", "Attempted to disconnect, but no connection was found");
        }
    }
}
