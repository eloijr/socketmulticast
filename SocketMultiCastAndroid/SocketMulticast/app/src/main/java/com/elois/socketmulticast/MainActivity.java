package com.elois.socketmulticast;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    private static final String SERVER_NAME = "LiveLayout";
    private static final int SERVER_PORT = 1805;
    private static final int WAITING_TIME = 10000;
    private static final String ADDRESS_GROUP = "226.0.0.1";
    private static final int BUFFER_SIZE = 4096;

    private static final int TASK_TYPE_RECEIVE = 0;
    private static final int TASK_TYPE_SEND = 1;
    private int TASK_TYPE = TASK_TYPE_SEND;

    private final String deviceName = Build.MODEL;

    private ThreadMulticastSocket mtMulticast;

    private Handler handlerUpdateUI;
    private RadioButton rbSender;
    private Button btnServer;
    private TextView txtLog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handlerUpdateUI = new Handler();

        rbSender = (RadioButton) findViewById(R.id.rdsender);
        btnServer = (Button) findViewById(R.id.btnserver);
        btnServer.setOnClickListener(this);

        txtLog = (TextView) findViewById(R.id.log);

    }

    @Override
    protected void onStop() {
        super.onStop();
        stopAll();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnserver:
                txtLog.setText("");
                startIdentification();
                break;
        }
    }

    private void stopAll() {
        if (mtMulticast != null) {
            mtMulticast.stopMSocket();
            mtMulticast = null;
        }
    }

    private void startIdentification() {
        txtLog.append("Starting request of identification" + "\n");
        if (rbSender.isChecked())
            TASK_TYPE = TASK_TYPE_SEND;
        else
            TASK_TYPE = TASK_TYPE_RECEIVE;
        mtMulticast = new ThreadMulticastSocket();
        mtMulticast.startMSocket();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateUI(final String msg) {
        handlerUpdateUI.post(new Runnable() {
            @Override
            public void run() {
                txtLog.append(msg + "\n");
            }
        });
    }

    private class ThreadMulticastSocket extends Thread {

        public void run() {
            try {
                updateUI("WIFI: Starting access\n");
                WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                WifiManager.MulticastLock enableMulticast;

                if (wifi == null) {
                    updateUI("WIFI: Not acessed!");
                } else {
                    updateUI("WIFI: Acquiring access\n");
                    enableMulticast = wifi.createMulticastLock("livelayout");
                    enableMulticast.acquire();
                    updateUI("WIFI: Acquired - " + enableMulticast.toString() + "\n");
                    updateUI("WIFI: Status: " + enableMulticast.isHeld());

                    MulticastSocket msocket = null;
                    DatagramSocket dsocket = null;
                    InetAddress group = InetAddress.getByName(ADDRESS_GROUP);
                    if (TASK_TYPE == TASK_TYPE_RECEIVE) {
                        msocket = new MulticastSocket(SERVER_PORT);
                        //msocket.setNetworkInterface();
                        msocket.joinGroup(group);

                        dsocket = new DatagramSocket();
                    } else {
                        msocket = new MulticastSocket();
                    }

                    byte[] bbuffer = new byte[4];
                    try {
                        if (TASK_TYPE == TASK_TYPE_RECEIVE) {
                            DatagramPacket packet = new DatagramPacket(bbuffer, bbuffer.length);
                            Log.d(LOG_TAG, "Waiting incoming request...");
                            updateUI("Waiting incoming request");

                            msocket.receive(packet);

                            Log.d(LOG_TAG, "Received data from: " + packet.getAddress().toString() +
                                    ":" + packet.getPort() + " with length: " +
                                    packet.getLength() + " and data: " + new String(packet.getData(), 0, packet.getData().length));
                            updateUI("Received data from: " + packet.getAddress().toString() + ":" + packet.getPort());

                            InetAddress ipclient = packet.getAddress();
                            int port = packet.getPort();
                            packet = new DatagramPacket(deviceName.getBytes(), deviceName.getBytes().length, ipclient, port);
                            Log.d(LOG_TAG, "Sending back to the cliente " + packet.getAddress().toString() + ":" + port);
                            updateUI("Sending back to the cliente " + packet.getAddress().toString() + ":" + port);
                            dsocket.send(packet);
                        } else { // TASK_TYPE == TASK_TYPE_SEND
                            DatagramPacket packet = new DatagramPacket(deviceName.getBytes(), deviceName.getBytes().length, group, SERVER_PORT);
                            updateUI("Sending information of "+deviceName);
                            dsocket = new DatagramSocket();
                            dsocket.send(packet);
                        }

                    } finally {
                        if (dsocket != null)
                            dsocket.close();
                        if (msocket != null) {
                            msocket.leaveGroup(group);
                            msocket.close();
                        }
                        enableMulticast.release();
                    }
                }
                updateUI("Multicast Process Finished");
            } catch (IOException e) {
                Log.d(LOG_TAG, e.toString());
            }
        }

        public void startMSocket() {
            start();
        }

        public void stopMSocket() {
        }
    }

}
