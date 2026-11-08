package com.aibrief.mobile;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Collections;
public final class NewsFetcher {
    private static final String[] URLS={
      "https://www.ithome.com/rss/",
      "https://techcrunch.com/category/artificial-intelligence/feed/",
      "https://www.theverge.com/rss/ai-artificial-intelligence/index.xml"
    };
    static class Story {String title,link,site;long at;Story(String a,String b,String c,long t){title=a;link=b;site=c;at=t;}}
    private static String strip(String s){return s==null?"":s.replaceAll("<[^>]*>","").replace("&amp;","&").replace("&quot;","\"").trim();}
    private static long date(String s){
        if(s==null)return 0;
        String[] f={"EEE, dd MMM yyyy HH:mm:ss Z","EEE, dd MMM yyyy HH:mm Z","yyyy-MM-dd'T'HH:mm:ssXXX","yyyy-MM-dd'T'HH:mm:ss'Z'"};
        for(String x:f){try{SimpleDateFormat d=new SimpleDateFormat(x,Locale.ENGLISH);d.setTimeZone(TimeZone.getTimeZone("UTC"));return d.parse(s).getTime();}catch(Exception ignored){}}
        return 0;
    }
    private static void load(String endpoint,ArrayList<Story> out){
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL(endpoint).openConnection();c.setConnectTimeout(6000);c.setReadTimeout(7000);
            c.setRequestProperty("User-Agent","AI-Brief-Mobile/0.1 (personal RSS reader)");
            try(InputStream in=c.getInputStream()){
                XmlPullParser p=Xml.newPullParser();p.setInput(in,"UTF-8");
                boolean item=false;int depth=0;String title="",link="",date="";
                String site=endpoint.contains("ithome")?"IT之家":endpoint.contains("techcrunch")?"TechCrunch":"The Verge";
                while(p.next()!=XmlPullParser.END_DOCUMENT){
                    int e=p.getEventType();String tag=p.getName();
                    if(e==XmlPullParser.START_TAG){
                        if("item".equals(tag)||"entry".equals(tag)){item=true;depth=p.getDepth();title="";link="";date="";continue;}
                        if(!item)continue;
                        if("link".equals(tag)&&p.getAttributeValue(null,"href")!=null){link=p.getAttributeValue(null,"href");continue;}
                        if("title".equals(tag)||"link".equals(tag)||"pubDate".equals(tag)||"published".equals(tag)||"updated".equals(tag)){
                            String val=p.nextText();if("title".equals(tag))title=strip(val);else if("link".equals(tag))link=strip(val);else date=val;
                        }
                    }else if(e==XmlPullParser.END_TAG&&item&&("item".equals(tag)||"entry".equals(tag))&&p.getDepth()==depth){
                        if(!title.isEmpty()&&link.startsWith("http")){
                            long ts=date(date);
                            if(ts>0&&System.currentTimeMillis()-ts<=48L*60*60*1000&&ts<=System.currentTimeMillis()+300000L)out.add(new Story(title,link,site,ts));
                        }
                        item=false;
                    }
                }
            }
        }catch(Exception ignored){}finally{if(c!=null)c.disconnect();}
    }
    public static String fetch(){
        ArrayList<Story> all=new ArrayList<>();for(String u:URLS)load(u,all);
        Collections.sort(all,(a,b)->Long.compare(b.at,a.at));
        HashSet<String> seen=new HashSet<>();StringBuilder b=new StringBuilder();
        SimpleDateFormat day=new SimpleDateFormat("yyyy年MM月dd日",Locale.CHINA);day.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        b.append("【全球 AI 情报日报】").append(day.format(new Date())).append("\n");
        b.append("来源：公开 RSS，过去48小时；非 ChatGPT 原定时任务的全文转发。\n\n");
        int i=0;for(Story s:all){
            if(!seen.add(s.title.toLowerCase(Locale.ROOT)))continue;
            if("IT之家".equals(s.site)&&!s.title.matches("(?s).*(AI|人工智能|大模型|机器人|算力|模型|豆包|千问|智谱|DeepSeek|OpenAI|Gemini|Claude|英伟达|谷歌|智能体|可灵|视频生成|生成式).*"))continue;
            b.append(++i).append(". ").append(s.title).append("（").append(s.site).append("）\n").append(s.link).append("\n\n");
            if(i>=12)break;
        }
        if(i==0)b.append("最近48小时暂无可从当前源核实的 AI 条目，请检查网络或扩充来源。\n");
        b.append("说明：标题可能保留英文，未接入大模型翻译/深度摘要；请点击原文核实，谨防旧闻。");
        return b.toString();
    }
}
