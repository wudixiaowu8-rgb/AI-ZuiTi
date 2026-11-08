package com.aizuoti.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private final int INK=Color.rgb(32,34,50);
    private final int MUTED=Color.rgb(99,103,122);
    private final int PURPLE=Color.rgb(108,85,220);
    private EditText chatInput, toneInput, keyInput;
    private TextView result, styleLabel, intensityLabel;
    private Button generateButton;
    private Button[] styleButtons;
    private Button[] choiceButtons;
    private final String[] choices = new String[3];
    private int style = 2;
    private int intensity = 3;

    private int dp(int n) {return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    private TextView text(String value,int size,int color,boolean bold){
        TextView t=new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if(bold)t.setTypeface(null,Typeface.BOLD);
        return t;
    }
    private Button button(String label){
        Button b=new Button(this);
        b.setAllCaps(false);
        b.setText(label);
        b.setTextSize(15);
        return b;
    }
    private void gap(LinearLayout parent,int pixels){
        View spacer=new View(this);
        parent.addView(spacer,new LinearLayout.LayoutParams(1,dp(pixels)));
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        ScrollView scroll=new ScrollView(this);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(17),dp(30),dp(17),dp(32));
        root.setBackgroundColor(Color.rgb(247,247,252));
        scroll.addView(root);

        root.addView(text("😼 AI 嘴替 · V0.2",25,INK,true));
        gap(root,6);
        root.addView(text("认真接话，也能抽象发疯｜先选风格，再生成三条回复",13,MUTED,false));
        gap(root,20);
        root.addView(text("① 粘贴聊天记录",17,INK,true));
        gap(root,7);
        root.addView(text("最好写明“我：”“对方：”，最后一条是对方说的。",12,MUTED,false));
        chatInput=new EditText(this);
        chatInput.setGravity(Gravity.TOP);
        chatInput.setTextSize(16);
        chatInput.setTextColor(INK);
        chatInput.setHint("对方：你刚刚在线，为什么不回我？\n我：刚才在忙\n对方：那你现在呢？");
        chatInput.setHintTextColor(MUTED);
        chatInput.setBackgroundColor(Color.WHITE);
        chatInput.setPadding(dp(14),dp(14),dp(14),dp(14));
        chatInput.setMinLines(5);
        chatInput.setMaxLines(13);
        root.addView(chatInput,new LinearLayout.LayoutParams(-1,dp(145)));
        gap(root,18);

        root.addView(text("② 选择嘴替人格",17,INK,true));
        gap(root,6);
        styleLabel=text("",13,PURPLE,true);
        root.addView(styleLabel);
        HorizontalScrollView styleScroller=new HorizontalScrollView(this);
        styleScroller.setHorizontalScrollBarEnabled(false);
        LinearLayout styleRow=new LinearLayout(this);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);
        styleButtons=new Button[ReplyBrain.STYLES.length];
        for(int i=0;i<ReplyBrain.STYLES.length;i++){
            final int selected=i;
            Button b=button(ReplyBrain.STYLES[i]);
            b.setTextSize(14);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(49));
            lp.rightMargin=dp(7);
            styleRow.addView(b,lp);
            b.setOnClickListener(v->{style=selected;updateStyle();});
            styleButtons[i]=b;
        }
        styleScroller.addView(styleRow);
        root.addView(styleScroller);
        updateStyle();
        gap(root,12);

        intensityLabel=text("放飞强度：3 / 4",14,INK,true);
        root.addView(intensityLabel);
        SeekBar strength=new SeekBar(this);
        strength.setMax(3);
        strength.setProgress(2);
        root.addView(strength);
        strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar s,int p,boolean fromUser){
                intensity=p+1;
                intensityLabel.setText("放飞强度："+intensity+" / 4"+
                    (intensity==1?" · 克制":intensity==2?" · 有梗":intensity==3?" · 放飞":" · 地狱级抽象"));
            }
            @Override public void onStartTrackingTouch(SeekBar s){}
            @Override public void onStopTrackingTouch(SeekBar s){}
        });
        gap(root,8);

        root.addView(text("③ 自定义你平时说话的味道（选填）",15,INK,true));
        toneInput=new EditText(this);
        toneInput.setSingleLine(false);
        toneInput.setMinLines(2);
        toneInput.setMaxLines(3);
        toneInput.setTextSize(14);
        toneInput.setHint("例如：不加表情包，嘴毒但不长篇大论，像熟人互损，15字以内");
        toneInput.setBackgroundColor(Color.WHITE);
        toneInput.setPadding(dp(12),dp(9),dp(12),dp(9));
        toneInput.setText(getPreferences(0).getString("tone",""));
        root.addView(toneInput);
        gap(root,14);

        root.addView(text("④ 生成回复",17,INK,true));
        generateButton=button("✨ 用免费 AI 分析并生成 3 条");
        generateButton.setTextColor(Color.WHITE);
        generateButton.setBackgroundTintList(ColorStateList.valueOf(PURPLE));
        root.addView(generateButton,new LinearLayout.LayoutParams(-1,dp(53)));
        generateButton.setOnClickListener(v->generateAi());
        Button offline=button("不联网：按场景生成离线参考");
        root.addView(offline,new LinearLayout.LayoutParams(-1,dp(51)));
        offline.setOnClickListener(v->generateOffline());
        Button chatgpt=button("免申请 Key：复制调教好的提问给 ChatGPT");
        root.addView(chatgpt,new LinearLayout.LayoutParams(-1,dp(51)));
        chatgpt.setOnClickListener(v->{
            String s=chat();
            if(s==null)return;
            String prompt=ReplyBrain.prompt(style,intensity,toneInput.getText().toString())+
                "\n【聊天内容】\n"+s+"\n请按上面规则分析并给出3条直接可发送的回复。";
            ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE))
                .setPrimaryClip(ClipData.newPlainText("AI嘴替调教提示词",prompt));
            toast("已复制高质量提示词，打开 ChatGPT 粘贴发送即可，不用额外 API Key。");
        });
        gap(root,12);

        result=text("还没有生成回复。\n\n真正的上下文分析需要连接免费 AI；离线功能只会匹配常见聊天场景。",16,INK,false);
        result.setTextIsSelectable(true);
        result.setBackgroundColor(Color.WHITE);
        result.setPadding(dp(14),dp(16),dp(14),dp(16));
        root.addView(result,new LinearLayout.LayoutParams(-1,-2));
        gap(root,8);
        choiceButtons=new Button[3];
        for(int i=0;i<3;i++){
            final int n=i;
            Button b=button("复制第 "+(i+1)+" 条");
            b.setVisibility(View.GONE);
            root.addView(b);
            b.setOnClickListener(v->copy(choices[n]));
            choiceButtons[i]=b;
        }
        Button copyAll=button("复制全部结果");
        root.addView(copyAll);
        copyAll.setOnClickListener(v->copy(result.getText().toString()));

        gap(root,22);
        root.addView(text("🔑 免费 AI 接入（仅设置一次）",17,INK,true));
        gap(root,7);
        root.addView(text("用你自己的 OpenRouter Key，模型固定为 openrouter/free，不会自动使用付费模型。免费次数有限，实际可用性取决于服务。",13,MUTED,false));
        gap(root,8);
        keyInput=new EditText(this);
        keyInput.setSingleLine(true);
        keyInput.setTextSize(14);
        keyInput.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
        keyInput.setHint("粘贴 OpenRouter API Key（sk-or-…）");
        keyInput.setBackgroundColor(Color.WHITE);
        keyInput.setPadding(dp(12),dp(12),dp(12),dp(12));
        keyInput.setText(KeyVault.load(this));
        root.addView(keyInput,new LinearLayout.LayoutParams(-1,dp(52)));
        Button keySave=button("保存 Key（手机系统加密）");
        root.addView(keySave);
        keySave.setOnClickListener(v->{
            String key=keyInput.getText().toString().trim();
            try{KeyVault.save(this,key);toast(key.isEmpty()?"Key 已清除":"Key 已在手机加密保存");}
            catch(Exception e){toast("保存失败："+e.getMessage());}
        });
        Button openKey=button("获取免费 OpenRouter Key");
        root.addView(openKey);
        openKey.setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://openrouter.ai/settings/keys"))));
        gap(root,14);
        root.addView(text("隐私与费用说明",15,INK,true));
        root.addView(text("• 离线：聊天记录只在手机上处理。\n• 点击“用免费 AI”后会先征求同意，再把输入的聊天记录发送到 OpenRouter 及其分配的模型提供方；请先去除姓名、手机号等敏感信息。\n• API Key 只在手机加密保存，不会上传到 GitHub。免费接口有每日额度限制。\n• 本版不会自动读取微信，也不会替你发送消息。",12,MUTED,false));
        gap(root,15);

        Button bubble=button("开启侧边悬浮球");
        root.addView(bubble);
        bubble.setOnClickListener(v->startBubble());
        Button stop=button("关闭悬浮球");
        root.addView(stop);
        stop.setOnClickListener(v->{stopService(new Intent(this,OverlayService.class));toast("悬浮球已关闭");});

        setContentView(scroll);
    }

    private void updateStyle(){
        styleLabel.setText("当前：" + ReplyBrain.STYLES[style]);
        for(int i=0;i<styleButtons.length;i++){
            Button b=styleButtons[i];
            boolean checked=i==style;
            b.setTextColor(checked?Color.WHITE:INK);
            b.setBackgroundTintList(ColorStateList.valueOf(checked?PURPLE:Color.rgb(229,231,241)));
        }
    }
    private String chat(){
        String s=chatInput.getText().toString().trim();
        if(s.isEmpty()){toast("先粘贴聊天记录，我才能帮你回。");return null;}
        return s.length()>5000?s.substring(s.length()-5000):s;
    }
    private void generateOffline(){
        String s=chat();
        if(s==null)return;
        String output=ReplyBrain.offline(s,style,intensity);
        showResult(output);
    }
    private void generateAi(){
        String s=chat();
        if(s==null)return;
        String key=keyInput.getText().toString().trim();
        if(key.isEmpty()){toast("先在下方填入免费的 OpenRouter API Key，也可以先用离线模式。");return;}
        final int selectedStyle=style;
        final int selectedIntensity=intensity;
        final String custom=toneInput.getText().toString().trim();
        new AlertDialog.Builder(this)
          .setTitle("发送聊天内容给第三方 AI？")
          .setMessage("这次会把输入框内最多5000字的聊天记录发送给 OpenRouter 及其选中的免费模型提供方。请确认内容不含不愿分享的隐私。发送不会触发微信消息发送。")
          .setNegativeButton("取消",null)
          .setPositiveButton("同意并生成",(d,w)->{
              getPreferences(0).edit().putString("tone",custom).apply();
              generateButton.setEnabled(false);
              generateButton.setText("正在分析上下文…");
              new Thread(()->{
                  String output=null;
                  String error=null;
                  try{output=OpenRouterApi.generate(key,s,selectedStyle,selectedIntensity,custom);}
                  catch(Exception e){error=e.getMessage();}
                  final String answer=output, problem=error;
                  runOnUiThread(()->{
                      generateButton.setEnabled(true);
                      generateButton.setText("✨ 用免费 AI 分析并生成 3 条");
                      if(problem!=null){showResult("AI 请求失败："+problem+"\n\n你仍可以点击“离线参考”使用本地模板。");}
                      else {showResult("【AI · "+ReplyBrain.STYLES[selectedStyle]+"】\n"+answer);}
                  });
              }).start();
          }).show();
    }
    private void showResult(String output){
        result.setText(output);
        for(int i=0;i<3;i++){choices[i]=null;choiceButtons[i].setVisibility(View.GONE);}
        Matcher m=Pattern.compile("(?m)^\\s*([123])[\\.、)）:]\\s*(.+)$").matcher(output);
        while(m.find()){
            int n=Integer.parseInt(m.group(1))-1;
            if(n>=0&&n<3){
                String line=m.group(2).trim();
                if(!line.isEmpty()){choices[n]=line;choiceButtons[n].setVisibility(View.VISIBLE);}
            }
        }
    }
    private void copy(String value){
        if(value==null||value.trim().isEmpty()){toast("先生成回复");return;}
        ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE))
          .setPrimaryClip(ClipData.newPlainText("AI嘴替回复",value));
        toast("已复制，去微信粘贴后自己决定发送。");
    }
    private void startBubble(){
        if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this)){
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:"+getPackageName())));
            toast("请先授予悬浮窗权限，返回后再次点击开启。");
            return;
        }
        try{
            Intent intent=new Intent(this,OverlayService.class);
            if(Build.VERSION.SDK_INT>=26)startForegroundService(intent);
            else startService(intent);
            toast("悬浮球已启动");
        }catch(Exception e){toast("启动悬浮球失败："+e.getMessage());}
    }
}