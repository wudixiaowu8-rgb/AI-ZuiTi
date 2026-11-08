package com.aibrief.mobile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public final class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        Scheduler.schedule(c);
        try{DigestService.launch(c);}catch(Exception e){Notice.show(c,"AI日报启动失败","请打开应用手动生成今日日报",88);}
    }
}
