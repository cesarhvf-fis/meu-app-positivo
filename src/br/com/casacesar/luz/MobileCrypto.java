package br.com.casacesar.luz;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Compatibility with the same mobile protocol verified by the Windows client. */
public final class MobileCrypto {
    public static final String CLIENT_ID = LocalProtocolConfig.CLIENT_ID;
    private static final String APP_SECRET = LocalProtocolConfig.APP_SECRET;
    private static final String SIGNED = "|a|v|lat|lon|lang|deviceId|imei|imsi|appVersion|ttid|isH5|h5Token|os|clientId|postData|time|requestId|n4h5|sid|sp|et|";
    public static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return out.toString();
    }
    public static String md5(String text) throws Exception {
        return hex(MessageDigest.getInstance("MD5").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    public static String mobileHash(String text) throws Exception {
        String s = md5(text);
        return s.substring(8, 16) + s.substring(0, 8) + s.substring(24, 32) + s.substring(16, 24);
    }
    public static String sign(Map<String, String> parameters, String json) throws Exception {
        TreeMap<String, String> sorted = new TreeMap<>(parameters);
        if (json != null && !json.isEmpty()) sorted.put("postData", json);
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> item : sorted.entrySet()) {
            if (!SIGNED.contains("|" + item.getKey() + "|") || item.getValue() == null || item.getValue().isEmpty()) continue;
            if (text.length() > 0) text.append("||");
            text.append(item.getKey()).append('=').append(item.getKey().equals("postData") ? mobileHash(item.getValue()) : item.getValue());
        }
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return hex(mac.doFinal(text.toString().getBytes(StandardCharsets.UTF_8)));
    }
    public static String encryptPassword(String modulus, String exponent, String password) throws Exception {
        BigInteger n = new BigInteger(modulus.replaceFirst("^0x", ""), 16);
        BigInteger e = new BigInteger(exponent.replaceFirst("^0x", ""), 16);
        BigInteger m = new BigInteger(1, md5(password).getBytes(StandardCharsets.UTF_8));
        String encrypted = m.modPow(e, n).toString(16);
        if (encrypted.length() % 2 != 0) encrypted = "0" + encrypted;
        return "0000000000000000000000000000000000000000000000000000000000000000" + encrypted;
    }
}
