package com.aizuoti.app;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class KeyVault {
    private static final String ALIAS = "ai_zuiti_api_key_v1";
    private static final String PREFS = "ai_zuiti_private_prefs";
    private KeyVault() {}
    private static SecretKey getKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        java.security.Key existing = ks.getKey(ALIAS,null);
        if(existing instanceof SecretKey) return (SecretKey)existing;
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
            ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
         .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
         .setKeySize(256).build();
        gen.init(spec);
        return gen.generateKey();
    }
    public static void save(Context ctx,String value) throws Exception {
        if(value == null || value.trim().isEmpty()){
            ctx.getSharedPreferences(PREFS,0).edit().remove("key").remove("iv").apply();
            return;
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,getKey());
        String encrypted = Base64.encodeToString(cipher.doFinal(value.trim().getBytes("UTF-8")),Base64.NO_WRAP);
        String iv=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP);
        ctx.getSharedPreferences(PREFS,0).edit().putString("key",encrypted).putString("iv",iv).apply();
    }
    public static String load(Context ctx) {
        try{
            String key=ctx.getSharedPreferences(PREFS,0).getString("key","");
            String iv=ctx.getSharedPreferences(PREFS,0).getString("iv","");
            if(key.isEmpty()||iv.isEmpty())return "";
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,getKey(),new GCMParameterSpec(128,Base64.decode(iv,Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(key,Base64.NO_WRAP)),"UTF-8");
        }catch(Exception e){return "";}
    }
}