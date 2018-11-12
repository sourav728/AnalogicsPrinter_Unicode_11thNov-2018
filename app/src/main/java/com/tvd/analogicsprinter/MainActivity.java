package com.tvd.analogicsprinter;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;

import android.graphics.Canvas;

import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;

import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.analogics.thermalAPI.Bluetooth_Printer_3inch_prof_ThermalAPI;
import com.analogics.thermalprinter.AnalogicsThermalPrinter;
import com.analogics.utils.AnalogicsUtil;
import com.onbarcode.barcode.android.BarcodeFactory;
import com.tvd.analogicsprinter.services.BluetoothService;
import com.tvd.analogicsprinter.values.FunctionCalls;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static com.tvd.analogicsprinter.values.Constant.MAIN_PRINTER_CONNECTED;
import static com.tvd.analogicsprinter.values.Constant.MAIN_PRINTER_DISCONNECTED;
import static com.tvd.analogicsprinter.values.Constant.MAIN_PRINTER_PAIRED;
import static com.tvd.analogicsprinter.values.Constant.MAIN_PRINTING_COMPLETED;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int RequestPermissionCode = 1;
    Toolbar toolbar;
    BluetoothAdapter deviceadapter;
    BluetoothDevice bluetoothDevice;
    AnalogicsThermalPrinter conn;
    ProgressDialog printing;
    Bluetooth_Printer_3inch_prof_ThermalAPI api;
    String address = "";
    Button bt_print_text, bt_print_image, bt_print_report, bt_bitmap_print;
    boolean text_print = false, image_print = false, printer_connected = false;
    FunctionCalls fcall;
    //Canvas canvas = new Canvas();
    Paint paint;
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MAIN_PRINTER_CONNECTED:
                    printer_connected = true;
                    buttons_enable(true);
                    break;

                case MAIN_PRINTER_DISCONNECTED:
                    printer_connected = false;
                    buttons_enable(false);
                    handler.sendEmptyMessage(MAIN_PRINTER_PAIRED);
                    break;

                case MAIN_PRINTING_COMPLETED:
                    printing.dismiss();
                    break;

                case MAIN_PRINTER_PAIRED:
                    try {
                        Log.d("debug", "Address: " + bluetoothDevice.getAddress());
                        address = bluetoothDevice.getAddress();
                        conn.openBT(bluetoothDevice.getAddress());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    break;
            }
            return false;
        }
    });

    /*private final Handler handler;
    {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MAIN_PRINTER_CONNECTED:
                        buttons_enable(true);
                        break;

                    case MAIN_PRINTER_DISCONNECTED:
                        buttons_enable(false);
                        break;

                    case MAIN_PRINTING_COMPLETED:
                        printing.dismiss();
                        break;
                }
            }
        };
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fcall = new FunctionCalls();
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        deviceadapter = BluetoothAdapter.getDefaultAdapter();
        deviceadapter.enable();

        conn = new AnalogicsThermalPrinter();
        api = new Bluetooth_Printer_3inch_prof_ThermalAPI();
        paint = new Paint();
        bt_print_text = (Button) findViewById(R.id.bt_print_text);
        bt_print_text.setOnClickListener(this);
      /*  bt_print_image = (Button) findViewById(R.id.bt_print_image);
        bt_print_image.setOnClickListener(this);
        bt_print_report = (Button) findViewById(R.id.bt_print_report);
        bt_print_report.setOnClickListener(this);*/

        bt_print_report = (Button) findViewById(R.id.bt_print_report);
        bt_print_report.setOnClickListener(this);

        bt_bitmap_print = findViewById(R.id.bt_bitmap_print);
        bt_bitmap_print.setOnClickListener(this);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkPermissionsMandAbove();
            }
        }, 1000);
    }

    private void startBroadcast() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                deviceadapter.startDiscovery();
                IntentFilter filter = new IntentFilter();
                filter.addAction(BluetoothDevice.ACTION_FOUND);
                filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
                filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
                registerReceiver(mReceiver, filter);
            }
        }, 2500);
    }

    @TargetApi(23)
    private void checkPermissionsMandAbove() {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= 23) {
            if (!checkPermission()) {
                requestPermission();
            } else startBroadcast();
        } else startBroadcast();
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]
                {
                        ACCESS_FINE_LOCATION
                }, RequestPermissionCode);
    }

    private boolean checkPermission() {
        int FirstPermissionResult = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        return FirstPermissionResult == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length > 0) {
                    boolean ReadLocationPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (!ReadLocationPermission)
                        checkPermissionsMandAbove();
                    else startBroadcast();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        deviceadapter.disable();
        unregisterReceiver(mReceiver);
        handler.removeCallbacksAndMessages(null);
        if (printer_connected)
            try {
                conn.closeBT();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                handler.sendEmptyMessage(MAIN_PRINTER_CONNECTED);
                Toast.makeText(MainActivity.this, "Bluetooth Printer Connected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                handler.sendEmptyMessage(MAIN_PRINTER_DISCONNECTED);
                Toast.makeText(MainActivity.this, "Bluetooth Printer Disconnected", Toast.LENGTH_SHORT).show();
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    bluetoothDevice = device;
                    handler.sendEmptyMessage(MAIN_PRINTER_PAIRED);
                }
            }
        }
    };

    private void buttons_enable(boolean enable) {
        bt_print_text.setEnabled(enable);
       /* bt_print_image.setEnabled(enable);
        bt_print_report.setEnabled(enable);*/
        bt_print_report.setEnabled(enable);
        bt_bitmap_print.setEnabled(enable);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_print_text:
                text_print = true;
                image_print = false;
                printing = ProgressDialog.show(MainActivity.this, "Printing", "Printing Please wait to Complete");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        handler.sendEmptyMessage(MAIN_PRINTING_COMPLETED);
                    }
                }, 3000);
                //printanalogics();
                //Prinit_Ticket(canvas);
                break;

            case R.id.bt_print_image:
                image_print = true;
                text_print = false;
                printing = ProgressDialog.show(MainActivity.this, "Printing", "Printing Please wait to Complete");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        handler.sendEmptyMessage(MAIN_PRINTING_COMPLETED);
                    }
                }, 5000);
                //printanalogics();
                //Prinit_Ticket();
                break;

            case R.id.bt_print_report:
                Write_Image();
                break;
            case R.id.bt_bitmap_print:
                Bitmap_Print();
                break;
        }
    }

    private String alignright(String msg, int len) {
        for (int i = 0; i < len - msg.length(); i++) {
            msg = " " + msg;
        }
        msg = String.format("%" + len + "s", msg);
        return msg;
    }

    private String aligncenter(String msg, int len) {
        int count = msg.length();
        int value = len - count;
        int append = (value / 2);
        return space(" ", append) + msg + space(" ", append);
    }

    private String line(int length) {
        StringBuilder sb5 = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb5.append("-");
        }
        return (sb5.toString());
    }

    private String space(String s, int len) {
        int temp;
        StringBuilder spaces = new StringBuilder();
        temp = len - s.length();
        for (int i = 0; i < temp; i++) {
            spaces.append(" ");
        }
        return (s + spaces);
    }

    private String currentDateandTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String cdt = sdf.format(new Date());
        return cdt;
    }

    private void printanalogics() {
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/DroidSansMono.ttf");
        String address = bluetoothDevice.getAddress();
        /*print_contrast(5);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(line(30) + "\n");
        stringBuilder.append(aligncenter("Belagavi", 30) + "\n");
        stringBuilder.append(aligncenter("(" + "540038" + ")", 30));
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_CENTER);
        text_line_spacing(5);
        stringBuilder.setLength(0);
        stringBuilder.append(space("RRNO", 8) + ":" + " " + "BA12345" + "\n");
        stringBuilder.append(space("Account ID", 16) + ":" + " " + "1234567890");
        if (text_print)
            analogics_double_print(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 30, tf, ALIGN_NORMAL);
        text_line_spacing(5);
        stringBuilder.setLength(0);
        stringBuilder.append(space("Mtr.Rdr.Code", 16) + ":" + " " + "54008301" + "\n");
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);
        text_line_spacing(3);
        stringBuilder.setLength(0);
        stringBuilder.append("Transvision Software" + "\n");
        stringBuilder.append("Peenya 2nd Stage" + "\n");
        stringBuilder.append("Bengaluru");
        if (text_print)
            analogics_48_print(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 20, tf, ALIGN_NORMAL);
        text_line_spacing(8);
        stringBuilder.setLength(0);
        stringBuilder.append(space("Tariff", 16) + ":" + " " + "5LT2A2" + "\n");
        stringBuilder.append(space("Sanct Load", 16) + ":" + "HP:" + alignright("0", 5) + " " + "KW:" + alignright("2", 5) + "\n");
        stringBuilder.append(space("Billing Period", 15) + ":" + "10/10/2017" + "-" + "10/11/2017");
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);
        text_line_spacing(8);
        stringBuilder.setLength(0);
        stringBuilder.append(space("Reading Date", 12) + ":" + " " + "10/11/2017");
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);
        text_line_spacing(9);
        stringBuilder.setLength(0);
        stringBuilder.append(space("BillNo", 10) + ":" + " " + "1234567890" + "-" + "10/11/2017" + "\n");
        stringBuilder.append(space("Meter SlNo.", 16) + ":" + " " + "5000101010");
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);
        text_line_spacing(4);
        stringBuilder.setLength(0);
        stringBuilder.append(space("Pres Rdg", 11) + ":" + " " + "1234" + "\n");
        stringBuilder.append(space("Prev Rdg", 11) + ":" + " " + "2345");
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 35, tf, ALIGN_NORMAL);
        text_line_spacing(3);
        stringBuilder.setLength(0);
        stringBuilder.append(space("Constant", 16) + ":" + " " + "1" + "\n");
        stringBuilder.append(space("Consumption", 16) + ":" + " " + "250" + "\n");
        stringBuilder.append(space("Average", 16) + ":" + " " + "250" + "\n");
        stringBuilder.append(space("Recorded MD", 16) + ":" + " " + "1" + "\n");
        stringBuilder.append(space("Power Factor", 16) + ":" + " " + "0.75"+ "\n");
        if (text_print) {
            stringBuilder.append("\n");
            analogicsprint(stringBuilder.toString());
        } else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);
        stringBuilder.setLength(0);*/
        /*stringBuilder.append(alignright("1.0", 8) + "  " + "x" + alignright("40.00", 8) + alignright("40.00", 18) + "\n");
        stringBuilder.append(alignright("1.0", 8) + "  " + "x" + alignright("50.00", 8) + alignright("50.00", 18) + "\n");
        if (text_print) {
            stringBuilder.append("\n");
            analogicsprint(stringBuilder.toString());
        } else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);
        stringBuilder.setLength(0);
        stringBuilder.append(alignright("30.0", 8) + "  " + "x" + alignright("3.25", 8) + alignright("97.50", 18) + "\n");
        stringBuilder.append(alignright("70.0", 8) + "  " + "x" + alignright("4.70", 8) + alignright("329.00", 18) + "\n");
        stringBuilder.append(alignright("100.0", 8) + "  " + "x" + alignright("6.25", 8) + alignright("518.75", 18) + "\n");
        stringBuilder.append(alignright("150.0", 8) + "  " + "x" + alignright("7.15", 8) + alignright("1072.50", 18));
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);
        stringBuilder.setLength(0);
		stringBuilder.append(space("Rebates/TOD", 16) + ":" + " " + alignright("0.00", 19) + "\n");
        stringBuilder.append(space("PF Penalty", 16) + ":" + " " + alignright("0.00", 19));
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);
        stringBuilder.setLength(0);
        stringBuilder.append(space("MD Penalty", 16) + ":" + " " + alignright("0.00", 19) + "\n");
        stringBuilder.append(space("Interest", 16) + ":" + " " + alignright("1.00", 19) + "\n");
        stringBuilder.append(space("Others", 16) + ":" + " " + alignright("0.00", 19));
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);
        stringBuilder.setLength(0);
        stringBuilder.append(space("Tax", 16) + ":" + " " + alignright("50.00", 19) + "\n");
        stringBuilder.append(space("Cur Bill Amt", 16) + ":" + " " + alignright("2107.75", 19) + "\n");
        stringBuilder.append(space("Arrears", 16) + ":" + " " + alignright("", 19));
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);
        stringBuilder.setLength(0);
        stringBuilder.append(space("Credits & Adj", 16) + ":" + " " + alignright("0.00", 19) + "\n");
        stringBuilder.append(space("GOK Subsidy", 16) + ":" + " " + alignright("0.00", 19));
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);
        stringBuilder.setLength(0);
        stringBuilder.append(space("Net Amt Due", 11) + ":" + " " + alignright("2102.00", 14));
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 35, tf, ALIGN_NORMAL);
        stringBuilder.setLength(0);
        stringBuilder.append(space("Due Date", 16) + ":" + " " + alignright("25/11/2017", 19) + "\n");
        stringBuilder.append(space("Printed On", 16) + ":" + " " + alignright(currentDateandTime(), 16));
        if (text_print)
            analogicsprint(stringBuilder.toString());
        else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);
        Bluetooth_Printer_3inch_prof_ThermalAPI api = new Bluetooth_Printer_3inch_prof_ThermalAPI();
        conn.printData(api.barcode_Code_128_Only_Numerics_VIP("1234567890123"));
        stringBuilder.setLength(0);
        stringBuilder.append("\n");
        stringBuilder.append("\n");*/
        /*if (text_print) {
            stringBuilder.append("\n");
            stringBuilder.append("\n");
            analogicsprint(stringBuilder.toString());
        } else analogics_Image_print(address, stringBuilder.toString(), 25, tf, ALIGN_NORMAL);*/
        StringBuilder stringBuilder = new StringBuilder();
        analogicsprint(aligncenter("Belagavi", 30), 6);
        analogicsprint(aligncenter("(" + "540038" + ")", 30), 6);
        stringBuilder.append(space("RRNO", 16) + ":" + " " + "BA12345" + "\n");
        stringBuilder.append(space("Account ID", 16) + ":" + " " + "1234567890");
        analogics_double_print(stringBuilder.toString(), 6);
        analogicsprint(space("Mtr.Rdr.Code", 16) + ":" + " " + "54008301", 6);
        stringBuilder.setLength(0);
        stringBuilder.append("\n");
        analogicsprint(stringBuilder.toString(), 6);
        analogics_48_print("Transvision Software", 3);
        analogics_48_print("Peenya 2nd Stage", 3);
        analogics_48_print("Bengaluru", 6);
        analogicsprint(space("Tariff", 16) + ":" + " " + "5LT2A2", 6);
        analogicsprint(space("Sanct Load", 14) + ":" + "HP:" + alignright("0", 4) + " " + "KW:" + alignright("2", 4), 6);
        analogicsprint(space("Billing", 8) + ":" + "10/10/2017" + "-" + "10/11/2017", 6);
        analogicsprint(space("Reading Date", 12) + ":" + " " + "10/11/2017", 6);
        analogicsprint(space("BillNo", 7) + ":" + " " + "1234567890" + "-" + "10/11/2017", 6);
        analogicsprint(space("Meter SlNo.", 16) + ":" + " " + "5000101010", 6);
        analogicsprint(space("Pres Rdg", 11) + ":" + " " + "1234", 6);
        analogicsprint(space("Prev Rdg", 11) + ":" + " " + "2345", 6);
        analogicsprint(space("Constant", 16) + ":" + " " + "1", 6);
        analogicsprint(space("Consumption", 16) + ":" + " " + "250", 6);
        analogicsprint(space("Average", 16) + ":" + " " + "250", 6);
        analogicsprint(space("Recorded MD", 16) + ":" + " " + "1", 6);
        analogicsprint(space("Power Factor", 16) + ":" + " " + "0.75", 6);
        stringBuilder.setLength(0);
        stringBuilder.append("\n");
        analogicsprint(stringBuilder.toString(), 6);
        analogicsprint(alignright("1.0", 6) + " " + "x" + alignright("40.00", 6) + alignright("40.00", 16), 6);
        analogicsprint(alignright("1.0", 6) + " " + "x" + alignright("50.00", 6) + alignright("50.00", 16), 6);
        stringBuilder.setLength(0);
        stringBuilder.append("\n");
        analogicsprint(stringBuilder.toString(), 6);
        analogicsprint(alignright("30.0", 6) + " " + "x" + alignright("3.25", 6) + alignright("97.50", 16), 6);
        analogicsprint(alignright("70.0", 6) + " " + "x" + alignright("4.70", 6) + alignright("329.00", 16), 6);
        analogicsprint(alignright("100.0", 6) + " " + "x" + alignright("6.25", 6) + alignright("518.75", 16), 6);
        analogicsprint(alignright("150.0", 6) + " " + "x" + alignright("7.15", 6) + alignright("1072.50", 16), 6);
        analogicsprint(space("Rebates/TOD", 12) + ":" + " " + alignright("0.00", 16), 5);
        analogicsprint(space("PF Penalty", 12) + ":" + " " + alignright("0.00", 16), 5);
        analogicsprint(space("MD Penalty", 12) + ":" + " " + alignright("0.00", 16), 5);
        analogicsprint(space("Interest", 12) + ":" + " " + alignright("1.00", 16), 5);
        analogicsprint(space("Others", 12) + ":" + " " + alignright("0.00", 16), 5);
        analogicsprint(space("Tax", 12) + ":" + " " + alignright("50.00", 16), 5);
        analogicsprint(space("Cur Bill Amt", 12) + ":" + " " + alignright("2107.75", 16), 6);
        analogicsprint(space("Arrears", 12) + ":" + " " + alignright("0.00", 16), 4);
        analogicsprint(space("Credits&Adj", 12) + ":" + " " + alignright("0.00", 16), 4);
        analogicsprint(space("GOK Subsidy", 12) + ":" + " " + alignright("0.00", 16), 0);
        analogics_double_print(space("Net Amt Due", 12) + ":" + " " + alignright("2102.00", 16), 0);
        analogicsprint(space("Due Date", 12) + ":" + " " + alignright("25/11/2017", 16), 4);
        print_bar_code("ANA1234567");
        stringBuilder.setLength(0);
        stringBuilder.append("\n");
        stringBuilder.append("\n");
        stringBuilder.append("\n");
        analogicsprint(stringBuilder.toString(), 4);
    }

    /*private void analogics_Image_print(String address, String Printdata, int text_size, Typeface typeface, Layout.Alignment alignment) {
        AnalogicsUtil utils = new AnalogicsUtil();
        Bitmap bmp = textAsBitmap(Printdata, (float)text_size, 9.0F, -16711681, typeface, alignment);
        try {
            Bitmap e = bmp.copy(Bitmap.Config.ARGB_4444, true);
            byte[] c1 = utils.prepareDataToPrint(address, e);
            conn.printData(c1);
        } catch (InterruptedException var13) {
            var13.printStackTrace();
        }
    }*/


    public void analogicsprint(String Printdata, int feed_line) {
        conn.printData(api.font_Courier_30_VIP(Printdata));
        text_line_spacing(feed_line);
    }

    public void analogics_double_print(String Printdata, int feed_line) {
        conn.printData(api.font_Double_Height_On_VIP());
        analogicsprint(Printdata, feed_line);
        conn.printData(api.font_Double_Height_Off_VIP());
    }

    public void analogics_48_print(String Printdata, int feed_line) {
        conn.printData(api.font_Courier_48_VIP(Printdata));
        text_line_spacing(feed_line);
    }

    public void text_line_spacing(int space) {
        conn.printData(api.variable_Size_Line_Feed_VIP(space));
    }

    private void print_bar_code(String msg) {
        String feeddata = "";
        feeddata = api.barcode_Code_128_Alpha_Numerics_VIP(msg);
        conn.printData(feeddata);
    }

    public void print_contrast(int n) {
        conn.printData(api.print_Contrast_VIP(n));
    }

    /*private Bitmap textAsBitmap(String text, float textSize, float stroke, int color, Typeface typeface, Layout.Alignment alignment) {
        TextPaint paint = new TextPaint();
        paint.setColor(color);
        paint.setDither(false);
        paint.setTextSize(textSize);
        paint.setStrokeWidth(stroke);
        paint.setTypeface(typeface);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = (float)((int)(-paint.ascent() + 1.0F));
        StaticLayout staticLayout = new StaticLayout(text, 0, text.length(), paint, 576, alignment, 1.0F, 1.0F, false);
        int linecount = staticLayout.getLineCount();
        int height = (int)(baseline + paint.descent()) * linecount;
        Bitmap image = Bitmap.createBitmap(576, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(image);
        staticLayout.draw(canvas);
        return image;
    }*/

    private void Prinit_Ticket(Canvas canvas) {
        int textsize = 22, rightspace = 14;
        /*canvas.CanvasBegin(576, 500);
        canvas.SetPrintDirection(0);*/
        conn.multiLinguallinePrint(address, fcall.aligncenter("HUBLI ELECTRICITY SUPPLY COMPANY LTD", 42), 28, Typeface.defaultFromStyle(Typeface.BOLD), 3);
        conn.multiLinguallinePrint(address, "ಉಪ ವಿಭಾಗ/Sub Division" + " " + fcall.empty(3) + ": " + "540038", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಆರ್.ಆರ್.ಸಂಖ್ಯೆ/RRNO" + " " + fcall.empty(9) + ": " + "IP57.228", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಖಾತೆ ಸಂಖ್ಯೆ/Account ID" + " " + fcall.empty(6) + ": " + "9913164549", 28, Typeface.defaultFromStyle(Typeface.BOLD), 3);
        conn.multiLinguallinePrint(address, "ಜಕಾತಿ/Tariff" + " " + fcall.empty(14) + ": " + "5LT6B-M", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಮಂ.ಪ್ರಮಾಣ/Sanct Load" + " " + fcall.empty(5) + ": " + "HP: 3  KW 2", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "Billing Period" + " " + fcall.empty(4) + ": " + "01/07/2018" + "-" + "01/08/2018", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ರೀಡಿಂಗ ದಿನಾಂಕ/Reading Date" + " " + fcall.empty(1) + ": " + "01/08/2018", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಮೀಟರ್ ಸಂಖ್ಯೆ/Meter SlNo" + " " + fcall.empty(3) + ": " + "500010281098", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಇಂದಿನ ಮಾಪನ/Pres Rdg" + " " + fcall.empty(5) + ": " + "658 / NOR", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಹಿಂದಿನ ಮಾಪನ/Prev Rdg" + " " + fcall.empty(5) + ": " + "600 / NOR", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಮಾಪನ ಸ್ಥಿರಾಂಕ/Constant" + " " + fcall.empty(5) + ": " + "1", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಬಳಕೆ/Consumption" + " " + fcall.empty(10) + ": " + "58", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಸರಾಸರಿ/Average" + " " + fcall.empty(12) + ": " + "51", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ದಾಖಲಿತ ಬೇಡಿಕೆ/Recorded MD" + " " + fcall.empty(2) + ": " + "10", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಪವರ ಫ್ಯಾಕ್ಟರ/Power Factor" + " " + fcall.empty(3) + ": " + "0.85", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, " ", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, fcall.empty(8) + "ನಿಗದಿತ ಶುಲ್ಕ/Fixed Charges", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, fcall.empty(4) + "3.0" + fcall.empty(3) + " x " + fcall.empty(3) + "60.00" + " " + fcall.empty(4) + "= " + fcall.rightAppend("180.00", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, fcall.empty(4) + "2.0" + fcall.empty(3) + " x " + fcall.empty(3) + "80.00" + " " + fcall.empty(4) + "= " + fcall.rightAppend("160.00", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, " ", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, fcall.empty(8) + "ವಿದ್ಯುತ್ ಶುಲ್ಕ/Energy Charges", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, fcall.empty(4) + "2.0" + fcall.empty(3) + " x " + fcall.empty(3) + "40.00" + " " + fcall.empty(4) + "= " + fcall.rightAppend("80.00", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, fcall.empty(4) + "2.0" + fcall.empty(3) + " x " + fcall.empty(3) + "50.00" + " " + fcall.empty(4) + "= " + fcall.rightAppend("100.00", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, fcall.empty(4) + "2.0" + fcall.empty(3) + " x " + fcall.empty(3) + "50.00" + " " + fcall.empty(4) + "= " + fcall.rightAppend("100.00", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, fcall.empty(4) + "2.0" + fcall.empty(3) + " x " + fcall.empty(3) + "50.00" + " " + fcall.empty(4) + "= " + fcall.rightAppend("100.00", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, " ", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಎಫ್.ಎ.ಸಿ/FAC" + " " + fcall.empty(14) + ": " + fcall.rightAppend("1258.00", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ರಿಯಾಯಿತಿ/Rebates/TOD" + " " + fcall.empty(5) + ": " + fcall.rightAppend("10.00", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಪಿ.ಎಫ್ ದಂಡ/PF Penalty" + " " + fcall.empty(4) + ": " + fcall.rightAppend("200.00", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಎಂ.ಡಿ.ದಂಡ/MD Penalty" + " " + fcall.empty(6) + ": " + fcall.rightAppend("0.00", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಬಡ್ಡಿ/Interest @1%" + " " + fcall.empty(9) + ": " + fcall.rightAppend("3.64", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಇತರೆ/Others" + " " + fcall.empty(14) + ": " + fcall.rightAppend("0.00", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ತೆರಿಗೆ/Tax @9%" + " " + fcall.empty(14) + ": " + fcall.rightAppend("25.47", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಬಿಲ್ ಮೊತ್ತ/Cur Bill Amt" + " " + fcall.empty(4) + ": " + fcall.rightAppend("620.01", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಬಾಕಿ/Arrears" + " " + fcall.empty(14) + ": " + fcall.rightAppend("1258.00", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಜಮಾ/Credits & Adj" + " " + fcall.empty(7) + ": " + fcall.rightAppend("320.01", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಐ.ಒ.ಡಿ/IOD" + " " + fcall.empty(16) + ": " + fcall.rightAppend("520.01", rightspace), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಸರ್ಕಾರದ ಸಹಾಯಧನ/GOK" + " " + fcall.empty(5) + ": " + fcall.rightAppend("3.64", rightspace), textsize, Typeface.MONOSPACE, 3);
        // conn.multiLinguallinePrint(address,"Net Amt Due"+ " " + fcall.empty( 22) + ": " + fcall.rightAppend(getResources().getString(R.string.rupee) +" "+ "978950.00", 16),30,Typeface.defaultFromStyle(Typeface.BOLD),3);
        conn.multiLinguallinePrint(address, "ಪಾವತಿ ಕೊನೆ ದಿನಾಂಕ/Due Date" + " " + ": " + "15/08/2018", textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಬಿಲ್ ದಿನಾಂಕ/Billed On" + " " + fcall.empty(5) + ": " + currentDateandTime(), textsize, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "ಮಾ.ಓ.ಸಂಕೇತ/MRCode" + ":" + "54003818 DANIAL DISOUZA", 20, Typeface.MONOSPACE, 3);
        Bluetooth_Printer_3inch_prof_ThermalAPI printer = new Bluetooth_Printer_3inch_prof_ThermalAPI();
        conn.printData(printer.barcode_Code_128_Alpha_Numerics_VIP("9913164549978950"));
        conn.multiLinguallinePrint(address, fcall.empty(12) + "9913164549978950", 16, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "\n", 20, Typeface.MONOSPACE, 3);
        conn.multiLinguallinePrint(address, "\n", 20, Typeface.MONOSPACE, 3);
        conn.printData(printer.Reset_VIP());

    }

    private void Bitmap_Print() {
        File file = new File(android.os.Environment.getExternalStorageDirectory(), "alg_image.jpg");
        if (file.exists()) {
            Bluetooth_Printer_3inch_prof_ThermalAPI btapi = new Bluetooth_Printer_3inch_prof_ThermalAPI();
            Bitmap bmp = BitmapFactory.decodeFile("/sdcard/alg_image.jpg");
            Bitmap mBitmap = btapi.prepareReciptImageDataToPrint_VIP(bmp);
            conn.printRecipt(address, mBitmap);
        } else
            Toast.makeText(this, "Please Convert Text into Canvas First and then take print..", Toast.LENGTH_SHORT).show();

    }

    private void Write_Image() {
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/RobotoMono-Regular.ttf");
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        // paint.setARGB(255, 0, 0, 0);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(typeface);

        Bitmap image = Bitmap.createBitmap(576, 1350, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(image);
        canvas.drawColor(Color.WHITE);
        Typeface bold = Typeface.create(typeface, Typeface.BOLD);
        paint.setTypeface(bold);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img5);
        float left = 10;
        float top = 10;
        RectF dst = new RectF(left, top, left + 560, top + 120); // width=100, height=120
        canvas.drawBitmap(bitmap, null, dst, null);

       /* paint.setTextSize(27);
        canvas.drawText("ಹುಬ್ಬಳ್ಳಿ ವಿದ್ಯುತ್ ಸರಬರಾಜು ಕಂಪನಿ ನಿಯಮಿತ", 10, 50, paint);*/
        paint.setTextSize(23);
        canvas.drawText("ಉಪ ವಿಭಾಗ/Sub Division", 10, 150, paint);
        canvas.drawText(":", 340, 150, paint);
        canvas.drawText(" 540038", 350, 150, paint);
        canvas.drawText("ಆರ್.ಆರ್.ಸಂಖ್ಯೆ/RRNO", 10, 190, paint);
        canvas.drawText(":", 340, 190, paint);
        canvas.drawText(" IP57.228", 350, 190, paint);
        paint.setTextSize(28);
        canvas.drawText("ಖಾತೆ ಸಂಖ್ಯೆ/AccountID", 10, 230, paint);
        canvas.drawText(":", 340, 230, paint);
        canvas.drawText(" 1234567890", 350, 230, paint);
        paint.setTextSize(23);
        canvas.drawText("ಜಕಾತಿ/Tariff", 10, 270, paint);
        canvas.drawText(":", 340, 270, paint);
        canvas.drawText(" 5LT6B-M", 350, 270, paint);
        canvas.drawText("ಮಂ.ಪ್ರಮಾಣ/Sanct Load", 10, 310, paint);
        canvas.drawText(":", 340, 310, paint);
        canvas.drawText(" HP:3 KW:2", 350, 310, paint);
        canvas.drawText("Billing Period", 10, 350, paint);
        canvas.drawText(":", 210, 350, paint);
        canvas.drawText(" 01-10-2018" + " - " + "01-11-2018", 220, 350, paint);
        // canvas.drawText("ೀಡಿಂಗ ದಿನಾಂಕ/Reading Date:", 10, 330, paint);
        //canvas.drawText(" 01-11-2018", 310, 330, paint);
        canvas.drawText("ಮೀಟರ್ ಸಂಖ್ಯೆ/Meter SlNo", 10, 390, paint);
        canvas.drawText(":", 340, 390, paint);
        canvas.drawText(" 12345678", 350, 390, paint);
        canvas.drawText("ಇಂದಿನ ಮಾಪನ/Pres Rdg", 10, 430, paint);
        canvas.drawText(":", 340, 430, paint);
        canvas.drawText(" 100", 350, 430, paint);
        canvas.drawText("ಹಿಂದಿನ ಮಾಪನ/Prev Rdg", 10, 470, paint);
        canvas.drawText(":", 340, 470, paint);
        canvas.drawText(" 50", 350, 470, paint);

        canvas.drawText("ಮಾಪನ ಸ್ಥಿರಾಂಕ/Constant", 10, 510, paint);
        canvas.drawText(":", 340, 510, paint);
        canvas.drawText(" 1", 350, 510, paint);
        canvas.drawText("ಬಳಕೆ/Consumption", 10, 550, paint);
        canvas.drawText(":", 340, 550, paint);
        canvas.drawText(" 50", 350, 550, paint);
        canvas.drawText("ಸರಾಸರಿ/Average", 10, 590, paint);
        canvas.drawText(":", 340, 590, paint);
        canvas.drawText(" 51", 350, 590, paint);

        canvas.drawText("ದಾಖಲಿತ ಬೇಡಿಕೆ/Recorded MD", 10, 630, paint);
        canvas.drawText(":", 340, 630, paint);
        canvas.drawText(" 10", 350, 630, paint);
        canvas.drawText("ಪವರ ಫ್ಯಾಕ್ಟರ/Power Factor", 10, 670, paint);
        canvas.drawText(":", 340, 670, paint);
        canvas.drawText(" 0.85", 350, 670, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ನಿಗದಿತ ಶುಲ್ಕ/Fixed Charges", 290, 710, paint);

        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(centeralign2("3.0 x 60.00", 10) + rightspacing("180.00", 29), 10, 750, paint);

        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ವಿದ್ಯುತ್ ಶುಲ್ಕ/Energy Charges", 290, 790, paint);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(centeralign2("3.0 x 60.00", 10) + rightspacing("180.00", 29), 10, 830, paint);

        canvas.drawText("ಎಫ್.ಎ.ಸಿ/FAC", 10, 870, paint);
        canvas.drawText(":", 340, 870, paint);
        canvas.drawText(rightspacing("10.00", 16), 350, 870, paint);

        canvas.drawText("ರಿಯಾಯಿತಿ/Rebates/TOD", 10, 910, paint);
        canvas.drawText(":", 340, 910, paint);
        canvas.drawText(rightspacing("10.00", 16), 910, 950, paint);

        canvas.drawText("ಪಿ.ಎಫ್ ದಂಡ/PF Penalty", 10, 950, paint);
        canvas.drawText(":", 340, 950, paint);
        canvas.drawText(rightspacing("10.00", 16), 350, 950, paint);

        canvas.drawText("ಎಂ.ಡಿ.ದಂಡ/MD Penalty", 10, 990, paint);
        canvas.drawText(":", 340, 990, paint);
        canvas.drawText(rightspacing("100.00", 16), 350, 990, paint);

        canvas.drawText("ಬಡ್ಡಿ/Interest @1%", 10, 1030, paint);
        canvas.drawText(":", 340, 1030, paint);
        canvas.drawText(rightspacing("1000.00", 16), 350, 1030, paint);

        canvas.drawText("ಇತರೆ/Others:", 10, 1070, paint);
        canvas.drawText(":", 340, 1070, paint);
        canvas.drawText(rightspacing("600.00", 16), 350, 1070, paint);

        canvas.drawText("ತೆರಿಗೆ/Tax @9%", 10, 1110, paint);
        canvas.drawText(":", 340, 1110, paint);
        canvas.drawText(rightspacing("6000.00", 16), 350, 1110, paint);

        canvas.drawText("ಬಿಲ್ ಮೊತ್ತ/Cur Bill Amt", 10, 1150, paint);
        canvas.drawText(":", 340, 1150, paint);
        canvas.drawText(rightspacing("60000.00", 16), 350, 1150, paint);

        canvas.drawText("ಬಾಕಿ/Arrears", 10, 1190, paint);
        canvas.drawText(":", 340, 1190, paint);
        canvas.drawText(rightspacing("1258.00", 16), 350, 1190, paint);

        canvas.drawText("ಜಮಾ/Credits & Adj", 10, 1230, paint);
        canvas.drawText(":", 340, 1230, paint);
        canvas.drawText(rightspacing("320.01", 16), 350, 1230, paint);

        canvas.drawText("ಐ.ಒ.ಡಿ/IOD", 10, 1270, paint);
        canvas.drawText(":", 340, 1270, paint);
        canvas.drawText(rightspacing("520.01", 16), 350, 1270, paint);

        paint.setTextSize(28);
        canvas.drawText("Net Amt Due", 10, 1310, paint);
        canvas.drawText(":", 220, 1310, paint);
        canvas.drawText(rightspacing("5000000.00", 16), 300, 1310, paint);


        try {
            savebitmap(image);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static File savebitmap(Bitmap bmp) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        File f = new File(Environment.getExternalStorageDirectory()
                + File.separator + "alg_image.jpg");
        f.createNewFile();
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(bytes.toByteArray());
        fo.close();
        return f;
    }

    private String centeralign2(String text, int width) {
        int count = text.length();
        int value = width - count;
        int append = (value / 2);
        //return space1(" ", append) + text;
        return space1(String.format("%s", ""), append) + text;
    }

    private String space1(String s, int length) {
        int temp;
        StringBuilder spaces = new StringBuilder();
        temp = length - s.length();
        for (int i = 0; i < temp; i++) {
            spaces.insert(0, String.format("%" + i + "s", ""));
        }
        return (s + spaces);
    }

    private String rightspacing(String s1, int len) {
        int i;
        StringBuilder s1Builder = new StringBuilder(s1);
        for (i = 0; i < len - s1Builder.length(); i++) {
        }
        s1Builder.insert(0, String.format("%" + i + "s", ""));
        s1 = s1Builder.toString();
        return (s1);
    }
   /* private void printtext2(com.lvrenyang.io.Canvas canvas, String text, String text2, String text3, Typeface tfNumber, float textsize,float yaxis) {
        yaxis++;
        canvas.DrawText(text, 10, yaxis, 0, tfNumber, textsize, com.lvrenyang.io.Canvas.DIRECTION_LEFT_TO_RIGHT);
        canvas.DrawText(text2, 310, yaxis, 0, tfNumber, textsize, com.lvrenyang.io.Canvas.DIRECTION_LEFT_TO_RIGHT);
        canvas.DrawText(text3 + "\r\n", 350, yaxis, 0, tfNumber, textsize, com.lvrenyang.io.Canvas.DIRECTION_LEFT_TO_RIGHT);
        if (textsize == 20) {
            yaxis = yaxis + textsize + 8;
        } else yaxis = yaxis + textsize + 6;
        yaxis = yaxis + axis;
    }*/

}
