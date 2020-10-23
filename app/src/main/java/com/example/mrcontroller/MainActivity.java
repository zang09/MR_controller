package com.example.mrcontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.erz.joysticklibrary.JoyStick;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements JoyStick.JoyStickListener{
    public static BluetoothSocket mmSocket;
    public static BluetoothDevice mmDevice;
    public static InputStream mmInputStream;
    public static OutputStream mmOutputStream;
    public BluetoothAdapter mBluetoothAdapter;
    public Set<BluetoothDevice> mPairedDevices;
    public List<String> mListPairedDevices;

    public ConnectedBluetoothThread mThreadConnectedBluetooth;
    public Handler mBluetoothHandler;

    public static boolean isOn = false;
    public static boolean BTisOn = false;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static int BT_DISCONNECTING_STATUS = 4;
    final static int BT_CONNECTION_LOST = -1;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static TextView deviceName;
    public TextView txtDirection;
    public ImageButton btnA;
    public ImageButton btnB;
    public ImageButton btnC;
    public ImageButton btnD;
    public ImageButton btnBTDevices;
    public ImageButton btnOnOff;
    public JoyStick joyStick;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        setContentView(R.layout.activity_main);

        deviceName = findViewById(R.id.deviceName);
        txtDirection = findViewById(R.id.direction);
        btnA = findViewById(R.id.btnA);
        btnB = findViewById(R.id.btnB);
        btnC = findViewById(R.id.btnC);
        btnD = findViewById(R.id.btnD);
        btnBTDevices = findViewById(R.id.btDevies);
        btnOnOff = findViewById(R.id.btnOnOff);
        joyStick = findViewById(R.id.joyStick);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btnA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("*a");
            }
        });
        btnB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("*b");
            }
        });
        btnC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("*c");
            }
        });
        btnD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send("*d");
            }
        });

        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BTisOn) {
                    Toast.makeText(MainActivity.this, "No connected device found!", Toast.LENGTH_SHORT).show();
                }
                else {
                    if (isOn) {
                        isOn = false;
                        btnOnOff.setImageResource(R.drawable.off_btn_state);
                        send("*f");
                    }
                    else {
                        isOn = true;
                        btnOnOff.setImageResource(R.drawable.on_btn_state);
                        send("*o");
                    }
                }
            }
        });

        joyStick.setListener(this);

        btnBTDevices.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                listPairedDevices();
            }
        });

        mBluetoothHandler = new Handler(){
            public void handleMessage(android.os.Message msg) {
                if (msg.what == BT_MESSAGE_READ) {
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    }
                    catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    txtDirection.setText(readMessage);
                }
                if (msg.what == BT_CONNECTING_STATUS) {
                    deviceName.setText(mmDevice.getName());
                    Toast.makeText(getApplicationContext(), "Device connect to " + mmDevice.getName(), Toast.LENGTH_SHORT).show();
                    BTisOn = true;
                }
                if(msg.what == BT_DISCONNECTING_STATUS) {
                    deviceName.setText("None");
                    btnOnOff.setImageResource(R.drawable.off_btn_state);
                    BTisOn = false;
                }
                if (msg.what == BT_CONNECTION_LOST) {
                    if(BTisOn) {
                        Toast.makeText(getApplicationContext(), "Connection lost: " + mmDevice.getName(), Toast.LENGTH_SHORT).show();
                        deviceName.setText("None");
                        btnOnOff.setImageResource(R.drawable.off_btn_state);
                        BTisOn = false;
                    }
                }
            }
        };
    }

    @Override
    public void onMove(JoyStick joyStick, double angle, double power, int direction) {
        String msg = "#" + direction + "," + power;
        System.out.printf("angle: %f, power: %f, direction: %d. %n", angle*180./3.14, power, direction);
        //txtDirection.setText(msg);
        send(msg);
    }

    @Override
    public void onTap() { }

    @Override
    public void onDoubleTap() { }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BT_REQUEST_ENABLE:
                if (resultCode == RESULT_OK) { // 블루투스 활성화 허용을 클릭하였다면
                    mPairedDevices = mBluetoothAdapter.getBondedDevices();

                    if (mPairedDevices.size() > 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        builder.setTitle("Select Device");

                        mListPairedDevices = new ArrayList<String>();
                        for (BluetoothDevice device : mPairedDevices) {
                            mListPairedDevices.add(device.getName());
                            //mListPairedDevices.add(device.getName() + "\n" + device.getAddress());
                        }
                        final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                        mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                        builder.setItems(items, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int item) {
                                connectSelectedDevice(items[item].toString());
                            }
                        });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                    else {
                        //Toast.makeText(getApplicationContext(), "There is no pairing device", Toast.LENGTH_LONG).show();
                    }
                }
                else if (resultCode == RESULT_CANCELED) { // 블루투스 활성화를 거부를 클릭하였다면
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void listPairedDevices() {
        if(BTisOn) {
            if (mBluetoothAdapter.isEnabled()) {
                //mBluetoothAdapter.disable();
                mThreadConnectedBluetooth.cancel();
                mBluetoothHandler.obtainMessage(BT_DISCONNECTING_STATUS, 1, -1).sendToTarget();
                Toast.makeText(getApplicationContext(), "Disconnect device: " + mmDevice.getName(), Toast.LENGTH_SHORT).show();
            }
        }
        else {
            if(mBluetoothAdapter == null) {
                Toast.makeText(getApplicationContext(), "This device not support bluetooth", Toast.LENGTH_LONG).show();
            }
            else {
                if (mBluetoothAdapter.isEnabled()) {
                    //Toast.makeText(getApplicationContext(), "Bluetooth is already activated", Toast.LENGTH_LONG).show();
                }
                else {
                    //Toast.makeText(getApplicationContext(), "Bluetooth is not activated", Toast.LENGTH_LONG).show();
                    Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
                }
            }

            mPairedDevices = mBluetoothAdapter.getBondedDevices();

            if (mPairedDevices.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Select Device");

                mListPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                    //mListPairedDevices.add(device.getName() + "\n" + device.getAddress());
                }
                final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        connectSelectedDevice(items[item].toString());
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            }
            else {
                //Toast.makeText(getApplicationContext(), "There is no pairing device", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void popListParedDevices() {

    }

    public void connectSelectedDevice(String selectedDeviceName) {
        for(BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mmDevice = tempDevice;
                break;
            }
        }
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mmSocket.connect();
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mmSocket);
            mThreadConnectedBluetooth.start();

            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
        }
        catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error occur while connect to bluetooth", Toast.LENGTH_LONG).show();
        }
    }

    public void send(String msg) {
        msg += "\n";

        if (BTisOn) {
            try {
                if (mmSocket.isConnected()) {
                    mmOutputStream.write(msg.getBytes());
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            //To often print func -> disable
            //Toast.makeText(MainActivity.this, "No connected device found!", Toast.LENGTH_SHORT).show();
        }
    }

    private class ConnectedBluetoothThread extends Thread {
        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            }
            catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error occur while connect to socket", Toast.LENGTH_LONG).show();
            }

            mmInputStream = tmpIn;
            mmOutputStream = tmpOut;
        }

        private Queue<byte[]> bufferQueue = new LinkedList<byte[]>();

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInputStream.read(buffer);

                    bufferQueue.offer(new byte[bytes]);
                    System.arraycopy(buffer, 0, bufferQueue.peek(), 0, bytes);

                    mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, bufferQueue.poll()).sendToTarget();
                }
                catch (IOException e) {
                    mBluetoothHandler.obtainMessage(BT_CONNECTION_LOST, 1, -1).sendToTarget();
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            }
            catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error occur while disconnect device", Toast.LENGTH_LONG).show();
            }
        }
    }

}