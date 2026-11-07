package com.aizuoti.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager wm;
    private View bubble;
    private WindowManager.LayoutParams params;
    @Override public void onCreate(){
        super.onCreate();
        String channel="ai_zuiti_float";
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26)manager.createNotificationChannel(new NotificationChannel(channel,"AI嘴替悬浮球",NotificationManager.IMPORTANCE_LOW));
        Intent launch=new Intent(this,MainActivity.class);
        PendingIntent pending=PendingIntent.getActivity(this,0,launch,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification note=new Notification.Builder(this,channel)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("AI嘴替运行中")
            .setContentText("点击悬浮球打开回复助手")
            .setContentIntent(pending).build();
        startForeground(1,note);
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){stopSelf();return;}
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        TextView icon=new TextView(this);
        icon.setText("😼");
        icon.setTextSize(26);
        icon.setGravity(Gravity.CENTER);
        icon.setBackgroundColor(Color.rgb(103,86,232));
        icon.setElevation(16);
        bubble=icon;
        params=new WindowManager.LayoutParams(136,136,
            Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.END;
        params.x=0;params.y=350;
        try { wm.addView(bubble,params); }catch(Exception e){stopSelf();return;}
        bubble.setOnTouchListener(new View.OnTouchListener(){
            float downX,downY;
            int originalX,originalY;
            boolean moved;
            @Override public boolean onTouch(View v,MotionEvent e){
                switch(e.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        downX=e.getRawX();downY=e.getRawY();originalX=params.x;originalY=params.y;moved=false;return true;
                    case MotionEvent.ACTION_MOVE:
                        if(Math.abs(e.getRawX()-downX)>12||Math.abs(e.getRawY()-downY)>12)moved=true;
                        if(moved){params.x=originalX-(int)(e.getRawX()-downX);params.y=originalY+(int)(e.getRawY()-downY);try{wm.updateViewLayout(bubble,params);}catch(Exception ex){}}
                        return true;
                    case MotionEvent.ACTION_UP:
                        if(!moved){
                            Intent open=new Intent(OverlayService.this,MainActivity.class);
                            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(open);
                        }
                        return true;
                }
                return false;
            }
        });
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){return START_STICKY;}
    @Override public void onDestroy(){if(wm!=null&&bubble!=null){try{wm.removeView(bubble);}catch(Exception e){}}super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}