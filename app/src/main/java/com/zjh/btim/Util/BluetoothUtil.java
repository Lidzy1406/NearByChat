package com.zjh.btim.Util;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.zjh.btim.Bean.BlueToothBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothUtil {

    private Context context;
    private BluetoothAdapter bluetoothAdapter;

    public BluetoothUtil(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); //Get local Bluetooth instance
    }

    /**
     * Determine whether Bluetooth is on or not
     */
    public boolean isBluetoothEnable() {
        return bluetoothAdapter.isEnabled();
    }

    /**
     * Turn on Bluetooth, can be found for 300 seconds
     */
    public void openBluetooth() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        context.startActivity(intent);
    }

    /**
     * Turn off Bluetooth
     */
    public void disableBluetooth() {
        bluetoothAdapter.disable();
    }

    /**
     * Query paired devices
     */
    public List<BlueToothBean> getDevicesList() {
        List<BlueToothBean> list = new ArrayList<>();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices)
                list.add(new BlueToothBean(device.getName(), device.getAddress()));
        }
        return list;
    }

    /**
     * Scanning accessory devices requires locate permission
     */
    public void startDiscovery() {
        bluetoothAdapter.startDiscovery();
    }

    public BluetoothDevice getBluetoothDevice(String mac) {
        return bluetoothAdapter.getRemoteDevice(mac);
    }

    public void close(){
        bluetoothAdapter.cancelDiscovery();
    }

}
