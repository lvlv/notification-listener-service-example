package com.github.chagall.notificationlistenerexample;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MIT License
 *
 *  Copyright (c) 2016 Fábio Alves Martins Pereira (Chagall)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
public class NotificationListenerExampleService extends NotificationListenerService {

    private static String TAG = "NOTI_LISTENER";
    private LOG Log = new LOG(this);

    /*
        These are the package names of the apps. for which we want to
        listen the notifications
     */
    private static final class ApplicationPackageNames {
        public static final String CGMCARE_PACK_NAME = "cn.cgmcare.app";
        public static final String XDRIP_PACK_NAME = "com.eveningoutpost.dexdrip";
    }

    /*
        These are the return codes we use in the method which intercepts
        the notifications, to decide whether we should do something or not
     */
    public static final class InterceptedNotificationCode {
        public static final int OTHER_NOTIFICATIONS_CODE = 0; // We ignore all notification with code == 0
        public static final int CGMCARE_CODE = 4;
        public static final int XDRIP_CODE = 5;
    }

    private static Map<String, String> trendMap = new HashMap<String, String>();

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onListenerConnected() {
        trendMap.put("\u21c8", "DoubleUp");
        trendMap.put("\u2191", "SingleUp");
        trendMap.put("\u2197", "FortyFiveUp");
        trendMap.put("\u2192", "Flat");
        trendMap.put("\u2198", "FortyFiveDown");
        trendMap.put("\u2193", "SingleDown");
        trendMap.put("\u21ca", "DoubleDown");

        StatusBarNotification[] activeNotifications = getActiveNotifications();
        Log.e(TAG, "v09181820 Current active notifications: " + activeNotifications.length);
        for (int i = 0; i < activeNotifications.length; ++i) {
            onNotificationPosted(activeNotifications[i]);
        }
    }

    private static class LOG {
        private NotificationListenerExampleService service;

        public LOG(NotificationListenerExampleService service) {
            this.service = service;
        }

        private void sendLogBroadcast(String log) {
            Intent intent = new Intent("com.github.chagall.notificationlistenerexample");
            intent.putExtra("log", log);
            service.sendBroadcast(intent);
        }

        public void e(String tag, String log) {
            DateFormat ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String now = ts.format(new Timestamp(System.currentTimeMillis()));
            android.util.Log.e(tag, log);
            sendLogBroadcast(String.format("%s %s", now, log));
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        int notificationCode = matchNotificationCode(sbn);
        if (notificationCode == InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE) return;
        Log.e(TAG, "onNotificationPosted: " + sbn.getNotification().toString());

        if (notificationCode == InterceptedNotificationCode.CGMCARE_CODE &&
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            Bundle extras = sbn.getNotification().extras;
            String notifyText = extras.getString(Notification.EXTRA_TEXT);
            Log.e(TAG, "Notification android.text: " + extras.getString(Notification.EXTRA_TEXT));
            BgValue bgValue = processCgmCareNotify(notifyText);
            if (bgValue != null) {
                sendBroadcastToAAPS(bgValue);
                sendBroadcastToxDrip(bgValue);
            } else {
                Log.e(TAG, "FAIL setting bg value");
            }
        }
    }

    private void sendBroadcastToxDrip(BgValue bgValue) {
        Bundle bundle = new Bundle();
        bundle.putString("collection", "entries");
        String direction = trendMap.get(bgValue.direction);
        if (direction == null) direction = "Flat";
        String jsonStr = String.format("[{type:sgv, direction:%s, date:%d, sgv:%f}]",
                direction, bgValue.timestamp.getTime(), bgValue.bgValue);
        bundle.putString("data", jsonStr);

        Intent intent = new Intent("com.eveningoutpost.dexdrip.NS_EMULATOR");
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        sendBroadcast(intent);
        Log.e(TAG,"intent sent: " + intent.toString());
    }

    private void sendBroadcastToAAPS(BgValue bgValue) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("doMgdl", true);
        bundle.putDouble("com.eveningoutpost.dexdrip.Extras.BgEstimate", bgValue.bgValue);
        bundle.putDouble("com.eveningoutpost.dexdrip.Extras.Raw", bgValue.bgValue);
        bundle.putLong("com.eveningoutpost.dexdrip.Extras.Time", bgValue.timestamp.getTime());
        String trend = trendMap.get(bgValue.direction);
        if (trend != null) {
            bundle.putString("com.eveningoutpost.dexdrip.Extras.BgSlopeName", trend);
        }
        Intent intent = new Intent("com.eveningoutpost.dexdrip.BgEstimate");
        intent.setPackage("info.nightscout.androidaps");
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        sendBroadcast(intent);
        Log.e(TAG,"intent sent: " + intent.toString());
    }

    private BgValue processCgmCareNotify(String notifyText) {
        // Example: "时间：15:48 血糖值：9.0 ↓-0.9 达标"
        Pattern p = Pattern.compile(".*时间：([0-9:\\.]+) 血糖值：([0-9\\.]+) ([^+-0-9]+)([+\\-0-9\\.]+)");
        Matcher m = p.matcher(notifyText);
        if (!m.find()) {
            Log.e(TAG, "TEXT NOT EXTRACTED: " + notifyText);
            return null;
        }
        BgValue bgValue = new BgValue();
        bgValue.setBgValue(m.group(2), "mmol");
        bgValue.setTimestamp(m.group(1));
        bgValue.setDirection(m.group(3));
        bgValue.setDelta(m.group(4));
        Log.e(TAG, "bgValue: " + bgValue.toString());
        return bgValue;
    }


    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) { }

    private int matchNotificationCode(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        Log.e(TAG, "matchNotificationCode: " + packageName);

        if(packageName.equals(ApplicationPackageNames.CGMCARE_PACK_NAME)){
            return(InterceptedNotificationCode.CGMCARE_CODE);
        }
        else if(packageName.equals(ApplicationPackageNames.XDRIP_PACK_NAME)){
            return(InterceptedNotificationCode.XDRIP_CODE);
        }
        else{
            return(InterceptedNotificationCode.OTHER_NOTIFICATIONS_CODE);
        }
    }
}
