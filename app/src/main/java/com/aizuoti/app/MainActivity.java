package com.aizuoti.app;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private EditText input;
    private TextView answer;
    private int mode = 0;
    private final String[] names = {"高情商", "幽默", "抽象发疯", "冷淡反击"};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(28, 38, 28, 24);
        layout.setBackgroundColor(Color.rgb(247,248,253));
        scroll.addView(layout);
        TextView title = new TextView(this);
        title.setText("😼 AI 嘴替");
        title.setTextSize(26);
        title.setTextTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.rgb(30,30,55));
        layout.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("免费离线体验版 · 手动粘贴聊天内容 · 不自动发送");
        subtitle.setTextSize(13);
        subtitle.setPadding(0, 10, 0, 25);
        layout.addView(subtitle);
        input = new EditText(this);
        input.setHint("在这里粘贴微信聊天内容，最后一行最好是对方说的话…");
        input.setTextSize(16);
        input.setGravity(Gravity.TOP);
        input.setMinLines(5);
        input.setBackgroundColor(Color.WHITE);
        input.setPadding(18,18,18,18);
        layout.addView(input,new LinearLayout.LayoutParams(-1,300));
        for (int i=0;i<names.length;i++) {
            final int selected=i;
            Button b=new Button(this);
            b.setText(names[i]+" · 生成回复");
            b.setAllCaps(false);
            layout.addView(b,new LinearLayout.LayoutParams(-1,-2));
            b.setOnClickListener(v->{mode=selected;generate();});
        }
        answer=new TextView(this);
        answer.setText("选择一种风格，生成可复制的回复。\n\n说明：此版本使用离线模板，不是假装通过 AI 理解聊天。");
        answer.setTextSize(17);
        answer.setTextColor(Color.rgb(20,30,50));
        answer.setPadding(20,20,20,20);
        answer.setBackgroundColor(Color.WHITE);
        layout.addView(answer,new LinearLayout.LayoutParams(-1,-2));
        Button copy=new Button(this);
        copy.setText("复制当前回复");
        layout.addView(copy);
        copy.setOnClickListener(v->{
            ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("AI 嘴替",answer.getText()));
            Toast.makeText(this,"已复制，回微信粘贴即可",Toast.LENGTH_SHORT).show();
        });
        Button bubble=new Button(this);
        bubble.setText("开启屏幕侧边悬浮球");
        layout.addView(bubble);
        bubble.setOnClickListener(v->startBubble());
        Button stop=new Button(this);
        stop.setText("关闭悬浮球");
        layout.addView(stop);
        stop.setOnClickListener(v->stopService(new Intent(this,OverlayService.class)));
        TextView foot=new TextView(this);
        foot.setText("隐私：聊天内容只留在本机页面中。\n当前版本不读取微信界面，也没有联网 AI；后续版本需要单独测试兼容性。");
        foot.setPadding(0,16,0,30);
        layout.addView(foot);
        setContentView(scroll);
    }
    private void generate(){
        String text=input.getText().toString().trim();
        if(text.isEmpty()){Toast.makeText(this,"请先粘贴聊天内容",Toast.LENGTH_SHORT).show();return;}
        String last=text.substring(Math.max(0,text.lastIndexOf('\n')+1)).trim();
        String result;
        switch(mode){
            case 0: result="我看到你说的了，也理解你的意思。刚刚没及时回应，现在我们好好聊聊？";break;
            case 1: result="收到！我大脑刚才在后台更新，现在终于把你的消息加载出来了😂";break;
            case 2: result="别急，我刚刚在跟宇宙申请聊天许可证，审批通过了，现在正式上线。";break;
            default: result="嗯，我知道了。等我有空再认真聊。";
        }
        answer.setText("【"+names[mode]+" · 离线模板】\n"+result+"\n\n【参考的最后一句】\n"+last);
    }
    private void startBubble(){
        if(Build.VERSION.SDK_INT>=23 && !Settings.canDrawOverlays(this)){
            Intent it=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));
            startActivity(it);
            Toast.makeText(this,"请允许悬浮窗，返回后再点开启",Toast.LENGTH_LONG).show();
            return;
        }
        try {
            Intent it=new Intent(this,OverlayService.class);
            if(Build.VERSION.SDK_INT>=26)startForegroundService(it);else startService(it);
            Toast.makeText(this,"悬浮球已启动",Toast.LENGTH_SHORT).show();
        }catch(Exception ex){Toast.makeText(this,"启动失败："+ex.getMessage(),Toast.LENGTH_LONG).show();}
    }
}