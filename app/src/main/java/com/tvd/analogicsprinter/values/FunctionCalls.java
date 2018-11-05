package com.tvd.analogicsprinter.values;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class FunctionCalls {
    public void logstatus(String value) {
        Log.d("debug", value+" length : "+value.length());
    }

    public String aligncenter(String msg, int len) {
        int count = msg.length();
        int value = len - count;
        int append = (value / 2);
        return space(" ", append) + msg + space(" ", append);
    }
    public String space(String s, int len) {
        int temp;
        StringBuilder spaces = new StringBuilder();
        temp = len - s.length();
        for (int i = 0; i < temp; i++) {
            spaces.append(" ");
        }
        return (s + spaces);
    }
    public String alignright(String msg, int len) {
        for (int i = 0; i < len - msg.length(); i++) {
            msg = " " + msg;
        }
        msg = String.format("%" + len + "s", msg);
        return msg;
    }
    public String alignright3(String msg, int len) {
        int i;
        StringBuilder stringBuilder = new StringBuilder(msg);
        for ( i = 0; i < len - msg.length(); i++) {
            //msg = String.format("%s"+msg," ");
        }
        stringBuilder.insert(0,String.format("%" + i + "s", " "));
        msg = stringBuilder.toString();
        return msg;
    }

    public String currentDateandTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
        return sdf.format(new Date());
    }


    public String leftAppend(String str, int maxlen){
        StringBuilder retStr= new StringBuilder();
        if(str.length() < maxlen){
            for(int i=0;i<maxlen-str.length();i++){
                retStr.append(" ");
            }
            retStr.insert(0, str);
        }
        return retStr.toString();

    }


    public String rightAppend(String str, int maxlen){
        StringBuilder retStr= new StringBuilder();
        if(str.length() < maxlen){
            for(int i=0;i<maxlen-str.length();i++){
                retStr.append(" ");
            }
            retStr.append(str);
        }
        return retStr.toString();

    }

    public String line(int length) {
        StringBuilder sb5 = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb5.append("-");
        }
        return (sb5.toString());
    }

    public String empty(int length) {
        StringBuilder sb5 = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb5.append(" ");
        }
        return (sb5.toString());
    }

}
