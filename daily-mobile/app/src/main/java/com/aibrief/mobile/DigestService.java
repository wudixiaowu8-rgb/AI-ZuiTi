package com.aibrief.mobile;
import android.app.Service;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Build;
import java.util.concurrent.Executors;
public final class DigestService extends Service {
    public static void launch(Context c){Intent i=new Intent(c,DigestService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent i,int flags,int startId){
        startForeground(11,Notice.create(this,"正在整理 AI 日报","正在读取公开 RSS 新闻源……"));
        Executors.newSingleThreadExecutor().execute(()->{
            try{
                String report=NewsFetcher.fetch();
                SharedPreferences sp=getSharedPreferences("prefs",0);
                sp.edit().putString("last_report",report).putLong("last_report_time",System.currentTimeMillis()).apply();
                if(sp.getBoolean("auto",false)){
                    KeyguardManager km=(KeyguardManager)getSystemService(KEYGUARD_SERVICE);
                    if(!km.isDeviceLocked() && WechatAccessService.active!=null){
                        WechatAccessService.active.sendToClone(sp.getString("recipient",""),report);
                    }else Notice.show(this,"AI日报已生成","手机锁定或辅助功能未连接：打开应用后可手动分享微信",12);
                }else Notice.show(this,"AI日报已生成","点击查看并分享到微信",12);
            }catch(Exception ex){Notice.show(this,"AI日报获取失败","请确认网络和 RSS 源可用，打开应用重试",12);}
            finally{stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId);}
        });
        return START_NOT_STICKY;
    }
}
