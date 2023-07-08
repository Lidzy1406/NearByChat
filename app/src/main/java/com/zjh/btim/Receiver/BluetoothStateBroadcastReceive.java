package com.zjh.btim.Receiver;

import  android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.zjh.btim.CallBack.BlueToothInterface;

public class BluetoothStateBroadcastReceive extends BroadcastReceiver {

    private BlueToothInterface blueToothInterface;

    public BluetoothStateBroadcastReceive(BlueToothInterface blueToothInterface) {
        this.blueToothInterface = blueToothInterface;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        switch (action) {
            case BluetoothDevice.ACTION_FOUND:
                Log.i("zjhAppareilsBluetooth", "Scanner vers l'appareil" + device.getName());
                blueToothInterface.getBlueToothDevices(device);
                break;
            case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                Log.i("zjhAppareilsBluetooth", "Recherche complète");
                blueToothInterface.searchFinish();
                break;
            case BluetoothDevice.ACTION_ACL_CONNECTED:
                Log.i("zjhAppareilsBluetooth", device.getName() + "Connecté");
                blueToothInterface.getConnectedBlueToothDevices(device);
                break;
            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                Log.i("zjhAppareilsBluetooth", device.getName() + "Déconnecté");
                blueToothInterface.getDisConnectedBlueToothDevices(device);
                break;
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                switch (blueState) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.i("AppareilsBluetooth", "Bluetooth est désactivé");
                        blueToothInterface.disable();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.i("zjhAppareilsBluetooth", "Bluetooth est activé");
                        blueToothInterface.open();
                        break;
                }
                break;
        }
    }
}
