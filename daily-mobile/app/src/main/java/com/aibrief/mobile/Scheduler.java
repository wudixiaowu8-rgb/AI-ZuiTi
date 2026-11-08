package com.aibrief.mobile;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.Calendar;
import java.util.TimeZone;
public final class Scheduler {
    private Scheduler() {}
    public static boolean schedule(Context c) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(c, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, 800, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Calendar next = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
        next.set(Calendar.HOUR_OF_DAY,8);next.set(Calendar.MINUTE,0);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
        if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DAY_OF_MONTH,1);
        if(Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms()){
            try{am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);return true;}catch(SecurityException ignored){}
        }
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pi);
        return false;
    }
}
