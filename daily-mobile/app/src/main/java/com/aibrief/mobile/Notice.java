package com.aibrief.mobile;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
public final class Notice {
    private Notice(){}
    static final String CHANNEL="aibrief_daily";
    public static Notification create(Context c,String title,String info){
        NotificationManager manager=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,"AI 情报日报",NotificationManager.IMPORTANCE_DEFAULT));
        PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(c,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(info).setStyle(new Notification.BigTextStyle().bigText(info)).setAutoCancel(true).setContentIntent(open).build();
    }
    public static void show(Context c,String title,String info,int id){
        if(Build.VERSION.SDK_INT>=33&&c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        ((NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id,create(c,title,info));
    }
}
