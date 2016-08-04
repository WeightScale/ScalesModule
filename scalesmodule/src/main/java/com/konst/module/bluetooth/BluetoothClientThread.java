package com.konst.module.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;
import android.util.Log;
import com.konst.module.Commands;
import com.konst.module.ObjectCommand;

import java.io.*;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Kostya  on 21.07.2016.
 */
public class BluetoothClientThread extends Thread {
    public BluetoothHandler handler;
    private BluetoothClientConnect bluetoothClientConnect;
    private final BluetoothSocket mmSocket;
    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String TAG = BluetoothClientThread.class.getName();

    public BluetoothClientThread(BluetoothDevice device, BluetoothHandler handler) {
        this.handler = handler;
        BluetoothSocket tmp = null;
        //mmDevice = device;
        try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
                tmp = device.createInsecureRfcommSocketToServiceRecord(uuid);
            else
                tmp = device.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException e) { }
        mmSocket = tmp;
    }

    public void run() {
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

        try {
            mmSocket.connect();
        } catch (IOException connectException) {
            try {mmSocket.close();} catch (IOException closeException) { }
            return;
        }

        bluetoothClientConnect = new BluetoothClientConnect(mmSocket, handler);
        bluetoothClientConnect.start();
    }



    public void cancel() {
        if (bluetoothClientConnect != null){
            bluetoothClientConnect.interrupt();
            bluetoothClientConnect.cancel();
        }
        try {
            mmSocket.close();
        } catch (IOException e) { }
    }


}
