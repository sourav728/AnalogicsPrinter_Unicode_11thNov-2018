package com.tvd.analogicsprinter.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.IntDef;
import android.util.Log;

import com.analogics.thermalprinter.AnalogicsThermalPrinter;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Set;

import static com.tvd.analogicsprinter.values.Constant.BLUETOOTH_RESULT;
import static com.tvd.analogicsprinter.values.Constant.DEVICE_NOT_PAIRED;
import static com.tvd.analogicsprinter.values.Constant.DEVICE_PAIRED;
import static com.tvd.analogicsprinter.values.Constant.DISCONNECTED;
import static com.tvd.analogicsprinter.values.Constant.PRINTER_CONNECTED;
import static com.tvd.analogicsprinter.values.Constant.PRINTER_DISCONNECTED;
import static com.tvd.analogicsprinter.values.Constant.RESULT;

public class BluetoothService extends Service {

    public static AnalogicsThermalPrinter conn = new AnalogicsThermalPrinter();
    public static boolean printerconnected = false;
    public static String printer_address = "";
    private boolean broadcast = false;
    BluetoothDevice bluetoothDevice;

    BluetoothAdapter mBluetoothAdapter;

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            Intent intent = new Intent();
            switch (msg.what) {
                case PRINTER_CONNECTED:
                    printerconnected = true;
                    printer_address = bluetoothDevice.getAddress();
                    logStatus("Printer Connected");
                    intent.setAction(BLUETOOTH_RESULT);
                    intent.putExtra("message", RESULT);
                    sendBroadcast(intent);
                    break;

                case PRINTER_DISCONNECTED:
                    printerconnected = false;
                    logStatus("Printer Disconnected");
                    intent.setAction(BLUETOOTH_RESULT);
                    intent.putExtra("message", DISCONNECTED);
                    sendBroadcast(intent);
                    break;

                case DEVICE_PAIRED:
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                conn.openBT(bluetoothDevice.getAddress());
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }, 1000);
                    break;

                case DEVICE_NOT_PAIRED:
                    logStatus("Device not Paired.. Starting Broadcast...");
                    startBroadcast();
                    break;

                default:
                    break;
            }
            return false;
        }
    });

    public BluetoothService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                logStatus("Device Paired but Broadcast starting after 1 seconds...");
                startBroadcast();
            }
        }, 1000);

        return START_STICKY;
    }

    private void startBroadcast() {
        if (!broadcast) {
            broadcast = true;
            mBluetoothAdapter.startDiscovery();
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mReceiver, filter);
        }
    }

    private void getPairedDevices() {
        Set<BluetoothDevice> pairedDevice = mBluetoothAdapter.getBondedDevices();
        if (pairedDevice.size() > 0) {
            try {
                for (BluetoothDevice device : pairedDevice) {
                    if (StringUtils.startsWithIgnoreCase(device.getName(), "AT3TV3")) {
                        bluetoothDevice = device;
                        mHandler.sendEmptyMessage(DEVICE_PAIRED);
                    } else mHandler.sendEmptyMessage(DEVICE_NOT_PAIRED);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else mHandler.sendEmptyMessage(DEVICE_NOT_PAIRED);
    }

    //The BroadcastReceiver that listens for bluetooth broadcasts
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    logStatus("ACTION_FOUND_PAIRED: "+device.getName());
                    if (StringUtils.startsWithIgnoreCase(device.getName(), "AT3TV3")) {
                        bluetoothDevice = device;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    conn.openBT(bluetoothDevice.getAddress());
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }, 500);
                    }
                } else logStatus("ACTION_FOUND_UNPAIRED: "+device.getName());
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                logStatus("ACTION_CONNECTED: "+device.getName());
                mHandler.sendEmptyMessage(PRINTER_CONNECTED);
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                logStatus("ACTION_DISCOVERY_FINISHED");
                if (!printerconnected)
                    mBluetoothAdapter.startDiscovery();
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                logStatus("ACTION_DISCOVERY_STARTED");
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                logStatus("ACTION_DISCONNECTED: "+device.getName());
                mHandler.sendEmptyMessage(PRINTER_DISCONNECTED);
                mBluetoothAdapter.startDiscovery();
            }
            else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    logStatus("Paired Device: "+device.getName());
                }
            }
        }
    };

    private void logStatus(String msg) {
        Log.d("debug", msg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            conn.closeBT();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        unregisterReceiver(mReceiver);
        mHandler.removeCallbacksAndMessages(null);
    }
}
