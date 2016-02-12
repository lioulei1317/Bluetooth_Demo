package com.liulw.bluetooth_demo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.bluetooth.BluetoothAdapter;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private final int  REQUEST_ENABLE_BT = 1;
    private BluetoothSocket socket = null;
    private ArrayAdapter<String> array_adapter;
    private ListView list_view;
    private BluetoothDevice remote_device;
    private EditText edit_send_msg;
    private readThread read_thread;
    private int offset = 0;
    private byte[] rev_buf = new byte[1024];
    BluetoothAdapter bluetooth_device = BluetoothAdapter.getDefaultAdapter();
    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String strPin = "1234";
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                switch(device.getBondState())
                {
                    case BluetoothDevice.BOND_BONDED:
                        Toast.makeText(getApplicationContext(), "配对完成", Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothDevice.BOND_BONDING:
                        Toast.makeText(getApplicationContext(), "正在配对", Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Toast.makeText(getApplicationContext(), "没有配对", Toast.LENGTH_LONG).show();
                        break;
                    default:
                        break;
                }
            }
            if (intent.getAction().equals(BluetoothDevice.ACTION_PAIRING_REQUEST)){

                Toast.makeText(getApplicationContext(), "have a requst", Toast.LENGTH_LONG).show();

                try {
                    ClsUtils.setPin(device.getClass(), device,strPin);


                }catch (Exception e){
                    Toast.makeText(getApplicationContext(), "fail", Toast.LENGTH_LONG).show();
                }
            }
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                // Get the BluetoothDevice object from the Intent
                //BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    Toast.makeText(getApplicationContext(), "found device:" + device.getName().toString(), Toast.LENGTH_LONG).show();
                    //array_adapter.add(device.getName() + '\n' + device.getAddress());
                    if (device.getName().equals("Terminal")) {
                        remote_device = device;

                        try {
                            ClsUtils.setPinConfig(device.getClass(), device, true);
                            ClsUtils.createBond(device.getClass(), device);
                            ClsUtils.setPin(device.getClass(), device, strPin);
                            //ClsUtils.cancelPairingUserInput(device.getClass(),device);

                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), "fail:" + device.getName().toString(), Toast.LENGTH_LONG).show();
                        }

                    }
                }
                // When discovery is finished, change the Activity title
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                setProgressBarIndeterminateVisibility(false);

            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Register for broadcasts when a device is discovered
        IntentFilter discoveryFilter = new IntentFilter();
        discoveryFilter.addAction(BluetoothDevice.ACTION_FOUND);
        discoveryFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        discoveryFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        //discoveryFilter.addAction("android.bluetooth.device.action.PAIRING_REQUEST");
        this.registerReceiver(mReceiver, discoveryFilter);
        edit_send_msg = (EditText)findViewById(R.id.edit_send_msg);
        // Register for broadcasts when discovery has finished
//        IntentFilter pairFilter = new IntentFilter("android.bluetooth.device.action.PAIRING_REQUEST");
//        this.registerReceiver(mReceiver, pairFilter);
    }

    public void bt_turn_on(View view){
        if(!bluetooth_device.isEnabled()){
            Intent bluetooth_enable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(bluetooth_enable, REQUEST_ENABLE_BT);
            Toast.makeText(getApplicationContext(), "Bluetooth Enable Success.", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(getApplicationContext(), "Bluetooth had Enable.", Toast.LENGTH_LONG).show();
    }

    public void bt_find(View view){
         array_adapter = new ArrayAdapter<String>
                (this,android.R.layout.simple_expandable_list_item_1);
        Set<BluetoothDevice> bt_devices = bluetooth_device.getBondedDevices();
        bluetooth_device.startDiscovery();
        if(bt_devices.size() > 0){
            for(BluetoothDevice device : bt_devices){
                array_adapter.add(device.getName() + '\n' + device.getAddress());
            }
            //list_view = new ListView(this);
            //list_view.setAdapter(array_adapter);
            //setContentView(list_view);
        }
    }

    public void bt_connect(View view){
        Set<BluetoothDevice> bt_devices = bluetooth_device.getBondedDevices();
//        if(bluetooth_device.isDiscovering()){
//            bluetooth_device.cancelDiscovery();
//        }
        //bluetooth_device.startDiscovery();

        if(bt_devices.size() > 0){
            for(BluetoothDevice device : bt_devices){
                Toast.makeText(getApplicationContext(), device.getName().toString(), Toast.LENGTH_LONG).show();
                if(device.getName().toString().equals("Terminal")){
                    //connect
                    if(!BluetoothAdapter.checkBluetoothAddress(device.getAddress().toString())){
                        Toast.makeText(getApplicationContext(), "地址无效", Toast.LENGTH_LONG).show();
                        return;
                    }

                    try {
                        bluetooth_device.getRemoteDevice(device.getAddress().toString());
                        socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));

                        socket.connect();
                        read_thread = new readThread();
                        read_thread.start();
                        Toast.makeText(getApplicationContext(), "连接开始", Toast.LENGTH_LONG).show();
//                        try {
//                            String msg = "Hello World";
//                            OutputStream os = socket.getOutputStream();
//                            os.write(msg.getBytes());
//                        } catch (IOException e) {
//                            // TODO Auto-generated catch block
//                            e.printStackTrace();
//                        }

                    }catch (IOException e){
                        Toast.makeText(getApplicationContext(), "连接失败", Toast.LENGTH_LONG).show();
                    }

                }
            }
        }
    }
    public void bt_turn_off(View view){
        if(bluetooth_device.isEnabled()){
            if(bluetooth_device.isDiscovering()){
                bluetooth_device.cancelDiscovery();
        }
            bluetooth_device.disable();
            try {
                ClsUtils.removeBond(remote_device.getClass(), remote_device);
            }catch (Exception e){
                Toast.makeText(getApplicationContext(), "remove bond fail.", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(getApplicationContext(), "Close Bluetooth Server.", Toast.LENGTH_LONG).show();
        }
    }

    public void bt_send_msg(View view){
        if(socket == null){
            return;
        }
        Toast.makeText(getApplicationContext(), "socket send.", Toast.LENGTH_LONG).show();
        try {
            String msg = edit_send_msg.getText().toString();
            OutputStream os = socket.getOutputStream();
            os.write(msg.getBytes());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void bt_read_msg(View view){
        byte val = rev_buf[10];
        rev_buf[10] = 0;
        String msg = new String(rev_buf);
        Toast.makeText(getApplicationContext(),msg + "\nbuf:"+ Integer.toString(val),Toast.LENGTH_LONG).show();

    }
    private class readThread extends Thread{
        public void run(){
            byte[] buf = new byte[1];
            int bytes;
            InputStream is=null;
            try{

                is = socket.getInputStream();

            }catch(IOException e){
                e.printStackTrace();
            }
            while(true){
                try{
                    bytes = is.read(buf);
                    if(bytes == 1){
                       if(buf[0] != '\n')
                       {
                           if(offset == 0){
                               for(int i=0;i<rev_buf.length;i++){
                                   rev_buf[i] = 0;
                               }
                           }
                           rev_buf[offset++] = buf[0];
                       }
                       else
                       {
                           offset = 0;
                       }
                    }
                }catch (IOException e){
                    try {
                        is.close();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    break;
                }
            }
        }
    }
}
