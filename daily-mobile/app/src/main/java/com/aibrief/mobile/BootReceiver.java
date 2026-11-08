package com.aibrief.mobile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent i){
        if("android.intent.action.BOOT_COMPLETED".equals(i.getAction()))Scheduler.schedule(c);
    }
}
