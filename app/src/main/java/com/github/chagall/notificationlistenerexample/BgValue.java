package com.github.chagall.notificationlistenerexample;

import android.util.Log;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class BgValue {
    public double bgValue;
    public Timestamp timestamp;
    public String direction;
    public String delta;

    private static String TAG = "NOTI_LISTENER";

    public void setBgValue(String value, String unit) {
        try {
            if (unit.equals("mmol")) {
                bgValue = Double.parseDouble(value) * 18.0;
            } else if (unit == "mgdl") {
                bgValue = Double.parseDouble(value);
            }
        } catch (Exception e) {
            Log.e(TAG, "setBgValue failed: " + value + " " + unit);
        }
    }

    private static DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static DateFormat day = new SimpleDateFormat("yyyy-MM-dd");
    private static DateFormat ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void setTimestamp(String hhmm) {
        long now = System.currentTimeMillis();
        String today = day.format(new Timestamp((now)));
        try {
            this.timestamp = Timestamp.valueOf(ts.format(formatter.parse(today + " " + hhmm)));
            if (this.timestamp.getTime() > now && hhmm.startsWith("23")) {
                // Set it to yesterday.
                this.timestamp.setTime(this.timestamp.getTime() - 86400000L);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parse " + hhmm);
            timestamp = new Timestamp(now);
        }
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("bg: ");
        builder.append(bgValue);
        builder.append(" ts: ");
        builder.append(timestamp);
        builder.append(" direction: ");
        builder.append(direction);
        builder.append(" delta: ");
        builder.append(delta);
        return builder.toString();
    }
}
