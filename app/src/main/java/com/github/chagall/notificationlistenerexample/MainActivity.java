package com.github.chagall.notificationlistenerexample;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

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
public class MainActivity extends AppCompatActivity {

    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

//    private ImageView interceptedNotificationImageView;
    private ArrayList<String> listLog = new ArrayList();

    private TextView logTextView;
    private LogBroadcastReceiver logBroadcastReceiver = new LogBroadcastReceiver();
    private AlertDialog enableNotificationListenerAlertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logTextView = (TextView) this.findViewById(R.id.log_text_view);

        // If the user did not turn the notification listener service on we prompt him to do so
        if (!isNotificationServiceEnabled()) {
            enableNotificationListenerAlertDialog = buildNotificationServiceAlertDialog();
            enableNotificationListenerAlertDialog.show();
        }

        // Finally we register a receiver to tell the MainActivity when a notification has been received
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.github.chagall.notificationlistenerexample");
        registerReceiver(logBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(logBroadcastReceiver);
    }

    /**
     * Is Notification Service Enabled.
     * Verifies if the notification listener service is enabled.
     * Got it from: https://github.com/kpbird/NotificationListenerService-Example/blob/master/NLSExample/src/main/java/com/kpbird/nlsexample/NLService.java
     * @return True if enabled, false otherwise.
     */
    private boolean isNotificationServiceEnabled(){
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Image Change Broadcast Receiver.
     * We use this Broadcast Receiver to notify the Main Activity when
     * a new notification has arrived, so it can properly change the
     * notification image
     * */
    public class LogBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String log = intent.getStringExtra("log");
            if (log == null) return;
            synchronized (listLog) {
                listLog.add(log);
                if (listLog.size() > 100) {
                    listLog.remove(0);
                }
            }
            updateView();
        }

        private void updateView() {
            StringBuilder stringBuilder = new StringBuilder();
            android.util.Log.e("NOTI_LISTENER", String.format("updating view: %d", listLog.size()));
            synchronized (listLog) {
                for (int i = listLog.size() - 1; i >= 0; --i) {
                    stringBuilder.append(String.format("%s\n", listLog.get(i)));
                }
            }
            android.util.Log.e("NOTI_LISTENER", String.format("updating view: %s", stringBuilder.toString().substring(0, 10)));
            logTextView.setText(stringBuilder.toString());
        }
    }


    /**
     * Build Notification Listener Alert Dialog.
     * Builds the alert dialog that pops up if the user has not turned
     * the Notification Listener Service on yet.
     * @return An alert dialog which leads to the notification enabling screen
     */
    private AlertDialog buildNotificationServiceAlertDialog(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle(R.string.notification_listener_service);
        alertDialogBuilder.setMessage(R.string.notification_listener_service_explanation);
        alertDialogBuilder.setPositiveButton(R.string.yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS));
                    }
                });
        alertDialogBuilder.setNegativeButton(R.string.no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // If you choose to not enable the notification listener
                        // the app. will not work as expected
                    }
                });
        return(alertDialogBuilder.create());
    }
}
