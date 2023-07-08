/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zjh.btim.Service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.zjh.btim.Activity.MainActivity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";

    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private int mState;
    // Afficher l'état actuel de la connexion
    public static final int STATE_NONE = 0;       // Ne rien faire
    public static final int STATE_LISTEN = 1;     // À l'écoute des connexions
    public static final int STATE_CONNECTING = 2; // Connexion en cours d'établissement
    public static final int STATE_TRANSFER = 3;  // Maintenant connecté à un dispositif distant qui peut transmettre

    //Utilisé pour envoyer des messages au fil principal
    private static Handler uiHandler;
    private BluetoothAdapter bluetoothAdapter;
    //Fils utilisés pour se connecter au port
    private AcceptThread mAcceptThread;
    private TransferThread mTransferThread;
    private ConnectThread mConnectThread;
    private boolean isTransferError = false;


    private static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BTIMBluetooth/";
    private static final int FLAG_MSG = 0;  //Balises de message
    private static final int FLAG_FILE = 1; //Marquage des documents

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    //Obtenir une seule instance
    public static volatile BluetoothChatService instance = null;

    public static BluetoothChatService getInstance(Handler handler) {
        uiHandler = handler;
        if (instance == null) {
            synchronized (BluetoothChatService.class) {
                if (instance == null) {
                    instance = new BluetoothChatService();
                }
            }
        }
        return instance;
    }

    public BluetoothChatService() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    /**
     * Activer l'écoute de service
     */
    public synchronized void start() {
        if (mTransferThread != null) {
            mTransferThread.cancel();
            mTransferThread = null;
        }

        setState(STATE_LISTEN);

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.e(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        if (mTransferThread != null) {
            mTransferThread.cancel();
            mTransferThread = null;
        }

        setState(STATE_NONE);
    }

    public void setState(int state) {
        this.mState = state;
    }

    /**
     * Accès à la connexion
     *
     * @param device
     */
    public synchronized void connectDevice(BluetoothDevice device) {
        Log.e(TAG, "connectDevice: ");
        // Fermer en premier si une transmission est en cours
        if (mState == STATE_CONNECTING) {
            if (mTransferThread != null) {
                mTransferThread.cancel();
                mTransferThread = null;
            }
        }

        //Fermer en premier s'il y a une connexion en cours
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        sendMessageToUi(MainActivity.BLUE_TOOTH_DIALOG, "collabore avec" + device.getName() + "Connexions");
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        //Marqué comme étant connecté
        setState(STATE_CONNECTING);
    }

    //Fils d'attente de connexion
    class AcceptThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            //Obtenir le port d'écoute du serveur
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = tmp;
        }

        @Override
        public void run() {
            super.run();
            //Port d'écoute
            BluetoothSocket socket = null;
            while (mState != STATE_TRANSFER) {
                try {
                    Log.e(TAG, "run: AcceptThread Blocage des appels，En attente de connexion");
                    socket = serverSocket.accept();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: ActivityThread fail");
                    break;
                }
                //Une fois le socket de connexion acquis, la communication commence
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                //Transfert de données, appels côté serveur
                                Log.e(TAG, "run: Server AcceptThread transfer");
                                sendMessageToUi(MainActivity.BLUE_TOOTH_DIALOG, "travaille avec" + socket.getRemoteDevice().getName() + "Connexions");
                                dataTransfer(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_TRANSFER:
                                // Pas prêt ou arrêt de la connexion
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket" + e);
                                }
                                break;
                        }
                    }
                }
            }
        }

        public void cancel() {
            Log.e(TAG, "close: activity Thread");
            try {
                if (serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "close: activity Thread fail");
            }
        }
    }

    private void sendMessageToUi(int what, Object s) {
        Message message = uiHandler.obtainMessage();
        message.what = what;
        message.obj = s;
        uiHandler.sendMessage(message);
    }

    /**
     * Début de la liaison de communication
     *
     * @param socket
     * @param remoteDevice 远程设备
     */
    private void dataTransfer(BluetoothSocket socket, final BluetoothDevice remoteDevice) {
        //Fermer le fil de connexion, où un seul appareil distant peut être connecté
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Démarrer les fils de connexion de gestion et ouvrir les transferts
        mTransferThread = new TransferThread(socket);
        mTransferThread.start();
        //Statut du drapeau comme connecté
        setState(STATE_TRANSFER);
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isTransferError) {
                    sendMessageToUi(MainActivity.BLUE_TOOTH_SUCCESS, remoteDevice);
                }
            }
        }, 300);
    }

    /**
     * Transfert de données
     *
     * @param msg
     */
    public void sendData(String msg) {
        TransferThread r;
        synchronized (this) {
            if (mState != STATE_TRANSFER) return;
            r = mTransferThread;
        }
        r.write(msg);
    }

    /**
     * Transfert de documents
     *
     * @param filePath
     */
    public void sendFile(String filePath) {
        TransferThread r;
        synchronized (this) {
            if (mState != STATE_TRANSFER) return;
            r = mTransferThread;
        }
        r.writeFile(filePath);
    }


    /**
     * Fils utilisés pour le transfert de données
     */
    class TransferThread extends Thread {
        private final BluetoothSocket socket;
        private final OutputStream out;
        private final DataOutputStream OutData;
        private final InputStream in;
        private final DataInputStream inData;


        public TransferThread(BluetoothSocket mBluetoothSocket) {
            socket = mBluetoothSocket;
            OutputStream mOutputStream = null;
            InputStream mInputStream = null;
            try {
                if (socket != null) {
                    //Obtenir les flux d'entrée et de sortie d'une connexion
                    mOutputStream = socket.getOutputStream();
                    mInputStream = socket.getInputStream();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            out = mOutputStream;
            OutData = new DataOutputStream(out);
            in = mInputStream;
            inData = new DataInputStream(in);
            isTransferError = false;
        }

        @Override
        public void run() {
            super.run();
            //Lire les données
            while (true) {
                try {
                    switch (inData.readInt()) {
                        case FLAG_MSG: //Lire des messages courts
                            String msg = inData.readUTF();
                            sendMessageToUi(MainActivity.BLUE_TOOTH_READ, msg);
                            break;
                        case FLAG_FILE: //Lire le fichier
                            File destDir = new File(FILE_PATH);
                            if (!destDir.exists())
                                destDir.mkdirs();
                            String fileName = inData.readUTF(); //Nom du fichier
                            long fileLen = inData.readLong(); //Longueur du fichier
                            sendMessageToUi(MainActivity.BLUE_TOOTH_READ_FILE_NOW, "Documents reçus(" + fileName + ")");
                            // Lire le contenu d'un fichier
                            long len = 0;
                            int r;
                            byte[] b = new byte[4 * 1024];
                            FileOutputStream out = new FileOutputStream(FILE_PATH + fileName);
                            while ((r = in.read(b)) != -1) {
                                out.write(b, 0, r);
                                len += r;
                                if (len >= fileLen)
                                    break;
                            }
                            sendMessageToUi(MainActivity.BLUE_TOOTH_READ_FILE, FILE_PATH + fileName);
                            break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: Transform error" + e.toString());
                    BluetoothChatService.this.start();
                    //TODO Affichage de la perte de connexion et redémarrage de la connexion
                    sendMessageToUi(MainActivity.BLUE_TOOTH_TOAST, "Échec de la connexion du dispositif/arrêt de la transmission");
                    isTransferError = true;
                    break;
                }
            }
        }

        /**
         * Transfert de données en écriture
         *
         * @param msg
         */
        public void write(final String msg) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        OutData.writeInt(FLAG_MSG); //消息标记
                        OutData.writeUTF(msg);
                    } catch (Throwable e) {
                        Log.i("zjhMessagerieBluetooth", "Échec de l'envoi");
                    }
                    sendMessageToUi(MainActivity.BLUE_TOOTH_WRAITE, msg);
                }
            });
        }

        /**
         * Envoi de documents
         */
        public void writeFile(final String filePath) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        sendMessageToUi(MainActivity.BLUE_TOOTH_WRAITE_FILE_NOW, "Documents envoyés(" + filePath + ")");
                        FileInputStream in = new FileInputStream(filePath);
                        File file = new File(filePath);
                        OutData.writeInt(FLAG_FILE); //document de référence
                        OutData.writeUTF(file.getName()); //Nom du fichier
                        OutData.writeLong(file.length()); //Longueur du fichier
                        int r;
                        byte[] b = new byte[4 * 1024];
                        while ((r = in.read(b)) != -1) {
                            OutData.write(b, 0, r);
                        }
                        sendMessageToUi(MainActivity.BLUE_TOOTH_WRAITE_FILE, filePath);
                    } catch (Throwable e) {
                        sendMessageToUi(MainActivity.BLUE_TOOTH_WRAITE_FILE_NOW, "Échec de la livraison d'un fichier");
                    }
                }
            });
        }


        public void cancel() {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed" + e);
            }
        }
    }

    class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
            BluetoothSocket mSocket = null;
            try {
                //Établissement de l'accès
                mSocket = device.createRfcommSocketToServiceRecord(
                        MY_UUID_SECURE);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ConnectThread: fail");
                sendMessageToUi(MainActivity.BLUE_TOOTH_TOAST, "Échec de la connexion, veuillez vous reconnecter");
            }
            socket = mSocket;
        }

        @Override
        public void run() {
            super.run();
            //Annuler l'analyse après la création
            bluetoothAdapter.cancelDiscovery();

            try {
                Log.e(TAG, "run: connectThread Attente");
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    Log.e(TAG, "run: unable to close");
                }
                //TODO Affichage de l'échec de la connexion
                sendMessageToUi(MainActivity.BLUE_TOOTH_TOAST, "Échec de la connexion, veuillez vous reconnecter");
                BluetoothChatService.this.start();
            }


            // Remise à zéro
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }
            //Socket has been connected, the default security, the client will only call
            Log.e(TAG, "run: connectThread Connecté et prêt à être transféré");
            dataTransfer(socket, device);
        }

        public void cancel() {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
