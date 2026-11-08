package com.aibrief.mobile;
import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
public final class MainActivity extends Activity {
    private SharedPreferences sp;private EditText recipient;private CheckBox enabled;private TextView status,preview;
    private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setPadding(0,12,0,12);return t;}
    private Button button(String s,View.OnClickListener fn){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(fn);return b;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onCreate(Bundle b){
        super.onCreate(b);sp=getSharedPreferences("prefs",0);
        ScrollView sv=new ScrollView(this);LinearLayout l=new LinearLayout(this);
        l.setPadding(32,32,32,32);l.setOrientation(LinearLayout.VERTICAL);sv.addView(l);
        l.addView(text("🌍 全球 AI 日报 · 微信手机版",24));
        l.addView(text("每天早上 08:00 生成日报（北京时间）\n需要联网；手机不依赖电脑。",15));
        l.addView(text("微信未提供个人好友自动发送接口。分身自动发送是高风险实验；只有屏幕解锁、双号在线、微信界面匹配时才可能成功，无法保证。",14));
        status=text("",14);l.addView(status);
        l.addView(button("① 授予通知与 08:00 定时权限",v->permissions()));
        l.addView(button("② 开启辅助功能（仅实验发送）",v->{try{startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(Exception ignored){toast("无法打开设置");}}));
        l.addView(text("③ 填写主微信在分身号的好友备注名（精确一致）",15));
        recipient=new EditText(this);recipient.setSingleLine(true);recipient.setHint("主号微信备注名");recipient.setText(sp.getString("recipient",""));l.addView(recipient);
        enabled=new CheckBox(this);
        enabled.setText("我了解微信自动化账号风险，同意解锁时尝试发送（默认关闭）");
        enabled.setChecked(sp.getBoolean("auto",false));l.addView(enabled);
        l.addView(button("保存设置并安排每日08:00",v->save()));
        l.addView(button("立即生成最新日报",v->{savePrefsOnly();DigestService.launch(this);toast("开始读取公开新闻，完成后通知你");}));
        l.addView(button("手动分享到微信（更可靠）",v->share()));
        l.addView(button("解锁状态下测试发送到分身好友",v->testAuto()));
        l.addView(text("最近一次日报：",18));
        preview=text("",14);preview.setTextIsSelectable(true);l.addView(preview);
        l.addView(text("当前来源：IT之家、TechCrunch AI、The Verge AI。48小时 RSS 标题和原文链接；暂未接入 ChatGPT 每日任务的全文或模型总结。",13));
        setContentView(sv);
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);
        Scheduler.schedule(this);refresh();
    }
    @Override protected void onResume(){super.onResume();if(sp!=null)refresh();}
    private void refresh(){
        if(status==null)return;
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
        boolean exact=Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms();
        status.setText("定时：每天 08:00（"+(exact?"精确闹钟已授权":"系统可能延迟")+"）\n自动尝试："+(sp.getBoolean("auto",false)?"开启":"关闭"));
        preview.setText(sp.getString("last_report","还没有生成，请点击“立即生成最新日报”。"));
    }
    private void permissions(){
        if(Build.VERSION.SDK_INT>=31){
            AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);
            if(!am.canScheduleExactAlarms()){
                try{startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}
                catch(Exception e){toast("请在系统设置中允许时钟和提醒");}
            }
        }
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);
        Scheduler.schedule(this);refresh();
    }
    private void savePrefsOnly(){
        String name=recipient.getText().toString().trim();
        sp.edit().putString("recipient",name).putBoolean("auto",enabled.isChecked()&&!TextUtils.isEmpty(name)).apply();
    }
    private void save(){
        if(enabled.isChecked()&&recipient.getText().toString().trim().isEmpty()){toast("请先填写好友昵称");return;}
        savePrefsOnly();boolean exact=Scheduler.schedule(this);
        toast(exact?"已保存：每日 08:00":"已保存，但精确闹钟未授权，可能延迟");refresh();
    }
    private void share(){
        String s=sp.getString("last_report","");
        if(s.isEmpty()){toast("请先生成日报");return;}
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,s);
        startActivity(Intent.createChooser(i,"选择微信或微信分身分享日报"));
    }
    private void testAuto(){
        savePrefsOnly();String s=sp.getString("last_report","");
        if(s.isEmpty()){toast("先生成日报");return;}
        if(!sp.getBoolean("auto",false)){toast("先阅读风险提示并勾选实验自动发送");return;}
        if(WechatAccessService.active==null){toast("请先开启辅助服务");return;}
        WechatAccessService.active.sendToClone(sp.getString("recipient",""),s);
        toast("尝试操作微信分身，匹配失败会通知");
    }
}
