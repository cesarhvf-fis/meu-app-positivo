package br.com.casacesar.luz;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Stores only a remembered session, never the user's password. Backup is disabled. */
final class SessionStore {
    private final String alias;
    private final SharedPreferences prefs;
    SessionStore(Context context) { this(context,"session","CasaLampada.Session.v1"); }
    SessionStore(Context context,String name,String keyAlias) { prefs=context.getSharedPreferences(name,Context.MODE_PRIVATE);alias=keyAlias; }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (store.containsAlias(alias)) return (SecretKey) store.getKey(alias, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    JSONObject load() throws Exception {
        String iv = prefs.getString("iv", null), value = prefs.getString("value", null);
        if (iv == null || value == null) return new JSONObject();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
        return new JSONObject(new String(cipher.doFinal(Base64.decode(value, Base64.NO_WRAP)), "UTF-8"));
    }
    void save(String sid,String email) throws Exception { saveObject(new JSONObject().put("sid",sid).put("email",email)); }
    void saveObject(JSONObject value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] bytes = value.toString().getBytes("UTF-8");
        try {
            byte[] encrypted = cipher.doFinal(bytes);
            if (!prefs.edit().putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .putString("value", Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) throw new Exception("Não foi possível lembrar a sessão.");
        } finally { java.util.Arrays.fill(bytes, (byte) 0); }
    }
    void clear() { prefs.edit().clear().apply(); }
}
