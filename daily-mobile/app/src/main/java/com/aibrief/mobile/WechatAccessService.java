package com.aibrief.mobile;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import java.util.ArrayList;
/** Best-effort, consent-based UI helper. Does NOT bypass lock screen or use private WeChat APIs. */
public final class WechatAccessService extends AccessibilityService {
    public static volatile WechatAccessService active;
    private final Handler h=new Handler(Looper.getMainLooper());
    private int stage=-1,tries=0;private String recipient="",report="";private long lastAction=0;
    @Override public void onServiceConnected(){super.onServiceConnected();active=this;}
    @Override public void onDestroy(){active=null;stage=-1;super.onDestroy();}
    @Override public void onAccessibilityEvent(AccessibilityEvent e){if(stage>=0)h.post(this::advance);}
    @Override public void onInterrupt(){}
    public void sendToClone(String dest,String message){
        h.post(()->{
            if(TextUtils.isEmpty(dest)||TextUtils.isEmpty(message)){fail("没有设置好友昵称或日报内容");return;}
            if(stage>=0){fail("已有发送任务在进行");return;}
            recipient=dest.trim();report=message;tries=0;stage=0;lastAction=0;
            if(!performGlobalAction(GLOBAL_ACTION_HOME)){fail("无法返回桌面");return;}
            stage=1;h.postDelayed(this::advance,1500);
        });
    }
    private static String value(CharSequence s){return s==null?"":s.toString();}
    private void collect(AccessibilityNodeInfo n,String wanted,boolean contains,ArrayList<AccessibilityNodeInfo> out,int depth){
        if(n==null||depth>25)return;
        String a=value(n.getText()),b=value(n.getContentDescription());
        if((contains&&(a.contains(wanted)||b.contains(wanted)))||(!contains&&(a.equals(wanted)||b.equals(wanted))))out.add(n);
        for(int i=0;i<n.getChildCount();i++)collect(n.getChild(i),wanted,contains,out,depth+1);
    }
    private AccessibilityNodeInfo get(AccessibilityNodeInfo root,String term,boolean contains){
        ArrayList<AccessibilityNodeInfo> nodes=new ArrayList<>();collect(root,term,contains,nodes,0);
        return nodes.isEmpty()?null:nodes.get(0);
    }
    private AccessibilityNodeInfo edit(AccessibilityNodeInfo n,int depth){
        if(n==null||depth>25)return null;
        if("android.widget.EditText".equals(value(n.getClassName()))&&n.isEnabled())return n;
        for(int i=0;i<n.getChildCount();i++){
            AccessibilityNodeInfo found=edit(n.getChild(i),depth+1);
            if(found!=null)return found;
        }
        return null;
    }
    private boolean click(AccessibilityNodeInfo n){
        for(int i=0;n!=null&&i<7;i++,n=n.getParent())
            if(n.isClickable())return n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        return false;
    }
    private boolean set(AccessibilityNodeInfo n,String content){
        if(n==null)return false;Bundle b=new Bundle();
        b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,content);
        return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b);
    }
    private void next(int s){stage=s;lastAction=System.currentTimeMillis();h.postDelayed(this::advance,1200);}
    private void fail(String why){stage=-1;Notice.show(this,"微信自动发送未完成",why+"；日报保存在应用里，可手动分享。",20);}
    private void advance(){
        if(stage<0)return;
        if(++tries>38){fail("操作超时：锁屏、微信图标位置变化或界面变化");return;}
        if(System.currentTimeMillis()-lastAction<750){h.postDelayed(this::advance,800);return;}
        AccessibilityNodeInfo root=getRootInActiveWindow();
        if(root==null){h.postDelayed(this::advance,700);return;}
        if(stage>=2&&stage<=6&&!"com.tencent.mm".equals(value(root.getPackageName()))){
            h.postDelayed(this::advance,700);return;
        }
        switch(stage){
            case 1:{
                AccessibilityNodeInfo icon=get(root,"Ⅱ·微信",false);
                if(icon==null)icon=get(root,"Ⅱ.微信",false);
                if(icon==null)icon=get(root,"微信分身",false);
                if(icon==null)icon=get(root,"Ⅱ",true);
                if(icon!=null&&click(icon)){next(2);return;}break;
            }
            case 2:{
                AccessibilityNodeInfo search=get(root,"搜索",false);
                if(search!=null&&click(search)){next(3);return;}break;
            }
            case 3:{
                AccessibilityNodeInfo field=edit(root,0);
                if(field!=null&&set(field,recipient)){next(4);return;}break;
            }
            case 4:{
                // Only choose an EXACT contact name; no fuzzy matches.
                AccessibilityNodeInfo target=get(root,recipient,false);
                if(target!=null&&click(target)){next(5);return;}break;
            }
            case 5:{
                AccessibilityNodeInfo title=get(root,recipient,false),field=edit(root,0);
                if(title!=null&&field!=null&&set(field,report)){next(6);return;}break;
            }
            case 6:{
                AccessibilityNodeInfo title=get(root,recipient,false),send=get(root,"发送",false);
                if(title!=null&&send!=null&&click(send)){
                    stage=-1;
                    Notice.show(this,"已点击微信发送","请在主号确认是否收到，自动操作无法验证送达。",21);return;
                }
                break;
            }
            default:break;
        }
        h.postDelayed(this::advance,800);
    }
}
