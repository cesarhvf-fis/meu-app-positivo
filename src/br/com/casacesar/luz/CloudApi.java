package br.com.casacesar.luz;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;

final class CloudApi {
    interface Transport { Object call(String action, JSONObject data, String sid, String gid) throws Exception; }
    private final String installationId;
    Transport testTransport;
    CloudApi(String id) { installationId = id; }
    Object call(String action, JSONObject data, String sid, String gid) throws Exception {
        return call(action, data, sid, gid, "1.0");
    }
    Object call(String action, JSONObject data, String sid, String gid, String version) throws Exception {
        if (testTransport != null) return testTransport.call(action, data, sid, gid);
        String json = data == null ? null : data.toString();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("appVersion", "1.1.6"); params.put("appRnVersion", "5.14"); params.put("channel", "oem");
        params.put("deviceId", installationId); params.put("platform", "Linux"); params.put("requestId", UUID.randomUUID().toString());
        params.put("lang", "pt"); params.put("a", action); params.put("clientId", MobileCrypto.CLIENT_ID);
        params.put("osSystem", "9"); params.put("os", "Android"); params.put("timeZoneId", "America/Sao_Paulo");
        params.put("ttid", "sdk_tuya@" + MobileCrypto.CLIENT_ID); params.put("et", "0.0.1"); params.put("v", version);
        params.put("sdkVersion", "3.10.0"); params.put("time", Long.toString(System.currentTimeMillis() / 1000));
        if (gid != null && !gid.isEmpty()) params.put("gid", gid);
        if (sid != null && !sid.isEmpty()) params.put("sid", sid);
        params.put("sign", MobileCrypto.sign(params, json));
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> p : params.entrySet()) { if (query.length() > 0) query.append('&'); query.append(encode(p.getKey())).append('=').append(encode(p.getValue())); }
        HttpURLConnection connection = (HttpURLConnection) new URL("https://a1.tuyaus.com/api.json?" + query).openConnection();
        connection.setConnectTimeout(12000); connection.setReadTimeout(20000); connection.setRequestMethod("POST");
        connection.setInstanceFollowRedirects(false); connection.setRequestProperty("User-Agent", "TY-UA=APP/Android/1.1.6/SDK/null");
        try {
            if (json != null) {
                byte[] body = ("postData=" + encode(json)).getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true); connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setFixedLengthStreamingMode(body.length);
                try (java.io.OutputStream out = connection.getOutputStream()) { out.write(body); }
            }
            if (connection.getResponseCode() != 200) throw new Exception("O servidor respondeu HTTP " + connection.getResponseCode() + ".");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[8192]; int n;
                while ((n = in.read(buffer)) != -1) { if (out.size() + n > 3 * 1024 * 1024) throw new Exception("Resposta muito grande."); out.write(buffer, 0, n); }
            }
            JSONObject response = new JSONObject(out.toString("UTF-8"));
            if (!response.optBoolean("success")) throw new Exception(response.optString("errorCode", "API") + ": " + response.optString("errorMsg", "Falha na nuvem."));
            return response.opt("result");
        } finally { connection.disconnect(); }
    }
    private static String encode(String value) throws Exception { return URLEncoder.encode(value, "UTF-8"); }
}
