package com.aizuoti.app;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class OpenRouterApi {
    private OpenRouterApi() {}
    private static String readText(InputStream stream) throws Exception {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append('\n');
                if (result.length() > 300000) break;
            }
            return result.toString();
        }
    }
    public static String generate(String key, String chat, int style, int intensity, String custom) throws Exception {
        URL url = new URL("https://openrouter.ai/api/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("Authorization", "Bearer " + key.trim());
            conn.setRequestProperty("X-Title", "AI ZuiTi Personal Android");
            JSONObject body = new JSONObject();
            body.put("model", "openrouter/free");
            body.put("max_tokens", 550);
            body.put("temperature", 0.85);
            JSONArray messages = new JSONArray();
            messages.put(new JSONObject().put("role","system").put("content",ReplyBrain.prompt(style,intensity,custom)));
            messages.put(new JSONObject().put("role","user").put("content","以下是用户主动提供的聊天记录，最后一条为需要回复的消息：\n\n"+chat));
            body.put("messages",messages);
            byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = conn.getOutputStream()) {os.write(bytes);}
            int status = conn.getResponseCode();
            String response = readText(status>=200&&status<300?conn.getInputStream():conn.getErrorStream());
            if (status == 401 || status == 403) throw new Exception("Key 无效或账户无权限，请检查 OpenRouter API Key。");
            if (status == 429) throw new Exception("免费请求次数或频率已达到限制，稍后再试，或者用离线模式。");
            if (status < 200 || status >= 300) {
                String err = "";
                try {err = new JSONObject(response).optJSONObject("error").optString("message","");} catch(Exception ignore) {}
                throw new Exception("接口返回 "+status+(err.isEmpty()?"，请稍后重试。":"："+err));
            }
            JSONObject json = new JSONObject(response);
            JSONArray choices = json.optJSONArray("choices");
            if (choices == null || choices.length() == 0) throw new Exception("模型没有返回有效回复，请重试。");
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            if (message == null) throw new Exception("模型返回格式不支持。");
            Object content = message.opt("content");
            String result = "";
            if (content instanceof String) result = ((String) content).trim();
            else if (content instanceof JSONArray) {
                JSONArray parts = (JSONArray) content;
                StringBuilder sb = new StringBuilder();
                for (int i=0;i<parts.length();i++) {
                    JSONObject part = parts.optJSONObject(i);
                    if(part != null) sb.append(part.optString("text", ""));
                }
                result = sb.toString().trim();
            }
            if(result.isEmpty()) throw new Exception("本次模型没有产生文字回复，换个话题再试。");
            return result;
        } finally {conn.disconnect();}
    }
}