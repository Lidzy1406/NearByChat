package com.zjh.btim.Fragment;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.zjh.btim.Activity.ChatActivity;
import com.zjh.btim.Adapter.ItemBtListAdapter;
import com.zjh.btim.Bean.BlueToothBean;
import com.zjh.btim.CallBack.BlueToothInterface;
import com.zjh.btim.R;
import com.zjh.btim.Receiver.BluetoothStateBroadcastReceive;
import com.zjh.btim.Service.BluetoothChatService;
import com.zjh.btim.Util.BluetoothUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.zjh.btim.Activity.MainActivity.BLUE_TOOTH_DIALOG;
import static com.zjh.btim.Activity.MainActivity.BLUE_TOOTH_SUCCESS;
import static com.zjh.btim.Activity.MainActivity.BLUE_TOOTH_TOAST;


public class HomeFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, AdapterView.OnItemClickListener {

    private BluetoothUtil bluetoothUtil;
    private BluetoothStateBroadcastReceive broadcastReceive;
    private SwipeRefreshLayout layoutSwipeRefresh;
    private ListView lvBtList;
    private List<BlueToothBean> list;
    private ItemBtListAdapter adapter;
    private LinearLayout layoutHide;
    private ProgressDialog progressDialog;
    private BluetoothChatService mBluetoothChatService;

    private BlueToothInterface blueToothInterface = new BlueToothInterface() {
        @Override
        public void getBlueToothDevices(BluetoothDevice device) {
            BlueToothBean blueToothBean = new BlueToothBean(device.getName(), device.getAddress());
            if (device.getName() != null && device.getAddress() != null) {
                int k = 0;
                for (BlueToothBean i : list)
                    if (i.getMac().equals(blueToothBean.getMac()))
                        k++;
                if (k == 0) {
                    list.add(blueToothBean);
                    adapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void getConnectedBlueToothDevices(BluetoothDevice device) {
            Snackbar.make(getView(), "Connexions" + device.getName() + "Succès", Snackbar.LENGTH_LONG).show();
        }

        @Override
        public void getDisConnectedBlueToothDevices(BluetoothDevice device) {
            Log.i("zjh-DisConnected", "Déconnexion");
            update();
        }

        @Override
        public void searchFinish() {
            layoutSwipeRefresh.setRefreshing(false);
        }

        @Override
        public void open() {
            mBluetoothChatService = BluetoothChatService.getInstance(handler);
            mBluetoothChatService.start();
            update();
        }

        @Override
        public void disable() {
            mBluetoothChatService.stop();
            update();
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                //Connexion
                case BLUE_TOOTH_DIALOG:
                    showProgressDialog((String) msg.obj);
                    break;
                //Échec de la connexion
                case BLUE_TOOTH_TOAST:
                    dismissProgressDialog();
                    Snackbar.make(getView(), (String) msg.obj, Snackbar.LENGTH_LONG).show();
                    break;
                //Connexion réussie
                case BLUE_TOOTH_SUCCESS:
                    BluetoothDevice remoteDevice = (BluetoothDevice) msg.obj;
                    dismissProgressDialog();
                    final Intent intent = new Intent(getContext(), ChatActivity.class);
                    intent.putExtra(ChatActivity.DEVICE_NAME_INTENT, remoteDevice.getName());
                    intent.putExtra(ChatActivity.DEVICE_MAC_INTENT, remoteDevice.getAddress());
                    final ProgressDialog dialog = new ProgressDialog(getContext());
                    dialog.setMessage("Raccordement de l'équipement" + msg.obj + "Succès");
                    dialog.setCancelable(false);
                    dialog.show();
                    Timer timer = new Timer();
                    TimerTask tast = new TimerTask() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            startActivityForResult(intent, 0);
                        }
                    };
                    timer.schedule(tast, 1500);
                    break;
            }
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bluetoothUtil = new BluetoothUtil(getContext());
        layoutSwipeRefresh = view.findViewById(R.id.layout_swipe_refresh);
        layoutSwipeRefresh.setOnRefreshListener(this);
        lvBtList = view.findViewById(R.id.lv_bt_list);
        list = new ArrayList<>();
        adapter = new ItemBtListAdapter(list, getContext());
        lvBtList.setAdapter(adapter);
        lvBtList.setOnItemClickListener(this);
        layoutHide = view.findViewById(R.id.layout_hide);
        update();
        registerBluetoothReceiver();
    }

    private void registerBluetoothReceiver() {
        Log.i("zjh", "Surveillance radio Bluetooth activée");
        if (broadcastReceive == null) {
            broadcastReceive = new BluetoothStateBroadcastReceive(blueToothInterface);
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_OFF");
        intentFilter.addAction("android.bluetooth.BluetoothAdapter.STATE_ON");
        getContext().registerReceiver(broadcastReceive, intentFilter);
    }

    private void unregisterBluetoothReceiver() {
        Log.i("zjh", "Désactivation de la surveillance radio Bluetooth");
        if (broadcastReceive != null) {
            getContext().unregisterReceiver(broadcastReceive);
            broadcastReceive = null;
        }
    }

    @Override
    public void onRefresh() {
        update();
    }

    private void update() {
        layoutSwipeRefresh.setRefreshing(true);
        list.clear();
        if (bluetoothUtil.isBluetoothEnable()) {
            layoutHide.setVisibility(View.INVISIBLE);
            list.addAll(bluetoothUtil.getDevicesList());
            bluetoothUtil.startDiscovery();
        } else {
            layoutHide.setVisibility(View.VISIBLE);
            layoutSwipeRefresh.setRefreshing(false);
        }
        adapter.notifyDataSetChanged();
        if (bluetoothUtil.isBluetoothEnable()) {
            mBluetoothChatService = BluetoothChatService.getInstance(handler);
            mBluetoothChatService.start();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ShowDialog(bluetoothUtil.getBluetoothDevice(list.get(position).getMac()));
    }

    /**
     * Connectivité Bluetooth
     *
     * @param device
     */
    private void ShowDialog(final BluetoothDevice device) {
        AlertDialog.Builder ad = new AlertDialog.Builder(this.getActivity());
        ad.setPositiveButton("Accepter", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //连接
                mBluetoothChatService.connectDevice(device);
            }
        });
        ad.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad.setMessage("Êtes-vous sûr de vouloir travailler avec " + device.getName() + " Établir un lien ？");
        ad.setTitle("Conseils");
        ad.setCancelable(false);
        ad.show();
    }

    /**
     *Dialogue sur l'état d'avancement
     *
     * @param msg
     */
    public void showProgressDialog(String msg) {
        if (progressDialog == null)
            progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(msg + "\nCette application doit être ouverte pour connecter l'appareil.");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Annuler", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mBluetoothChatService.stop();
                update();
            }
        });
        progressDialog.show();
    }

    /**
     * Fermer le dialogue sur l'état d'avancement
     */
    public void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        update();
    }

    @Override
    public void onDestroy() {
        Log.i("zjh-onDestroy", "HomeFermer");
        super.onDestroy();
        unregisterBluetoothReceiver();
        bluetoothUtil.close();
        if (mBluetoothChatService != null)
            mBluetoothChatService.stop();
    }
}
