package br.com.casacesar.luz;

import android.app.Application;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.service.quicksettings.TileService;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public final class CasaApp extends Application {
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    private SessionStore sessions;
    private SharedPreferences preferences;
    CloudBackend cloud;
    private DeviceExtras extras;
    private TriggerCmd triggerCmd;
    private volatile String snapshot = "{\"loggedIn\":false,\"devices\":[],\"homes\":[]}";
    private volatile String preferred = "";
    @Override public void onCreate() {
        super.onCreate(); sessions = new SessionStore(this); preferences = getSharedPreferences("lamp", MODE_PRIVATE);
        preferred = preferences.getString("preferred", "");
        String installationId = preferences.getString("installationId", "");
        if (installationId.isEmpty()) { installationId = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().substring(0, 8); preferences.edit().putString("installationId", installationId).apply(); }
        final String id = installationId;
        worker.execute(() -> {
            try {
                PresetStore presets = new PresetStore(getFilesDir(), asset("defaults.json"));
                cloud = new CloudBackend(new CloudApi(id), presets); extras = new DeviceExtras(cloud); triggerCmd = new TriggerCmd(this);
                try {
                    JSONObject saved = sessions.load(); cloud.sid = saved.optString("sid"); cloud.email = saved.optString("email");
                    if (!cloud.sid.isEmpty()) cloud.restoreState(new JSONObject(preferences.getString("snapshot", "{}")));
                } catch (Exception invalidSession) { sessions.clear(); }
                updateSnapshot();
                PowerCycle.arm(this);
            } catch (Exception ignored) { snapshot = "{\"loggedIn\":false,\"devices\":[],\"homes\":[],\"error\":\"Não foi possível preparar o aplicativo.\"}"; }
        });
    }
    String asset(String name) throws Exception {
        try (InputStream in = getAssets().open(name); ByteArrayOutputStream out = new ByteArrayOutputStream()) { byte[] buf = new byte[8192]; int n; while ((n = in.read(buf)) != -1) out.write(buf, 0, n); return new String(out.toByteArray(), StandardCharsets.UTF_8); }
    }
    JSONObject snapshot() { try { return new JSONObject(snapshot); } catch (Exception ignored) { return new JSONObject(); } }
    String preferred() { return preferred; }
    Object dispatch(String route, JSONObject data) throws Exception {
        if (cloud == null) throw new Exception("Não foi possível iniciar os controles.");
        Object result;
        switch (route) {
            case "state": result = cloud.state(); break;
            case "login":
                try { cloud.login(data.getString("email").trim().toLowerCase(java.util.Locale.ROOT), data.getString("password")); sessions.save(cloud.sid, cloud.email); }
                catch (Exception e) { cloud.logout(); sessions.clear(); throw e; }
                result = cloud.state(); break;
            case "logout": if(PowerCycle.status(this).getBoolean("pending"))throw new Exception("Aguarde o religamento da tomada antes de sair da conta.");cloud.logout(); sessions.clear(); result = cloud.state(); break;
            case "refresh": result = cloud.refresh(); chooseDefault(); break;
            case "device": result = cloud.readDevice(data.getString("deviceId")); break;
            case "rename":
                String renameId=data.getString("deviceId"),newName=DeviceExtras.name(data.getString("name"));cloud.member(renameId);
                if(data.optBoolean("local")){getSharedPreferences("device_names",MODE_PRIVATE).edit().putString(renameId,newName).apply();result=new JSONObject().put("saved",true).put("name",newName).put("local",true);}
                else{result=extras.rename(renameId,newName);getSharedPreferences("device_names",MODE_PRIVATE).edit().remove(renameId).apply();}break;
            case "shares": result=extras.shares(data.getString("deviceId"));break;
            case "share-add": result=extras.shareAdd(data.getString("deviceId"),data.getString("email"));break;
            case "share-remove": result=extras.shareRemove(data.getString("deviceId"),data.getString("relationId"));break;
            case "startup": result=extras.startup(data.getString("deviceId"));break;
            case "startup-save": result=extras.startupSave(data.getString("deviceId"),data.getJSONObject("values"));break;
            case "energy": result=extras.energy(data.getString("deviceId"));break;
            case "energy-history": result=extras.history(data.getString("deviceId"),data.getString("month"));break;
            case "triggercmd-status": result=triggerCmd.status();break;
            case "triggercmd-save": result=triggerCmd.save(data);break;
            case "triggercmd-check": result=triggerCmd.check();break;
            case "triggercmd-pair-prepare": result=triggerCmd.preparePair();break;
            case "triggercmd-pair-import": result=triggerCmd.importPair();break;
            case "triggercmd-forget": result=triggerCmd.clear();break;
            case "triggercmd-shutdown":
                if(PowerCycle.status(this).getBoolean("pending"))throw new Exception("Aguarde a conclusão do reinício da tomada.");
                android.app.KeyguardManager lock=(android.app.KeyguardManager)getSystemService(KEYGUARD_SERVICE);
                if(lock!=null&&lock.isDeviceLocked())throw new Exception("Desbloqueie o celular para desligar o Windows.");
                result=triggerCmd.shutdown();break;
            case "socket-power":
                if(!(data.get("power") instanceof Boolean))throw new Exception("Estado de energia inválido.");
                if(PowerCycle.status(this).getBoolean("pending"))throw new Exception("Aguarde o religamento programado antes de alterar a energia.");
                result=extras.socketPower(data.getString("deviceId"),data.getBoolean("power"));break;
            case "pc-cycle": result=PowerCycle.start(this,data.getString("deviceId"));break;
            case "cycle-status": result=PowerCycle.status(this);break;
            case "lamp": requireLamp(data.getString("deviceId")); result = cloud.lamp(data.getString("deviceId"), data.getJSONObject("values")); break;
            case "toggle":
                requireLamp(data.getString("deviceId")); if (!(data.get("power") instanceof Boolean)) throw new Exception("Estado inválido.");
                result = cloud.toggle(data.getString("deviceId"), data.getBoolean("power")); break;
            case "effects": result = data.has("index") ? cloud.presets.save(data.getInt("index"), data.optJSONObject("preset"), data.optBoolean("restore")) : cloud.presets.load(); break;
            case "timers": requireLamp(data.getString("deviceId")); result = cloud.timers(data.getString("deviceId")); break;
            case "timer-save": requireLamp(data.getString("deviceId")); result = cloud.timerSave(data.getString("deviceId"), data.getJSONObject("timer")); break;
            case "timer-status": requireLamp(data.getString("deviceId")); result = cloud.timerStatus(data.getString("deviceId"), data.getString("id"), data.getInt("status")); break;
            case "preferred":
                if (data.has("deviceId")) { requireLamp(data.getString("deviceId")); preferred = data.getString("deviceId"); preferences.edit().putString("preferred", preferred).apply(); }
                result = new JSONObject().put("deviceId", preferred); break;
            default: throw new Exception("Ação não permitida.");
        }
        // Telemetry/history do not change the lamp widget. Avoid disk writes and
        // launcher redraws for every one-second measurement.
        if(!route.startsWith("triggercmd-")&&!route.equals("energy")&&!route.equals("energy-history")&&!route.equals("cycle-status"))updateSnapshot();
        return result;
    }
    private void requireLamp(String id) throws Exception { if (!CloudBackend.isLamp(cloud.member(id))) throw new Exception("A tomada não pode ser controlada por este aplicativo de lâmpadas."); }
    private void chooseDefault() throws Exception {
        JSONArray list = cloud.state().getJSONArray("devices"); JSONObject previous = CloudBackend.find(list, preferred);
        if (previous != null && CloudBackend.isLamp(previous)) return;
        for (int i = 0; i < list.length(); i++) { JSONObject d = list.getJSONObject(i); if (CloudBackend.isLamp(d) && !d.optBoolean("shared")) { preferred = d.getString("id"); preferences.edit().putString("preferred", preferred).apply(); return; } }
        for (int i = 0; i < list.length(); i++) { JSONObject d = list.getJSONObject(i); if (CloudBackend.isLamp(d)) { preferred = d.getString("id"); preferences.edit().putString("preferred", preferred).apply(); return; } }
        preferred = ""; preferences.edit().remove("preferred").apply();
    }
    void updateSnapshot() throws Exception {
        JSONObject current = cloud.state(); snapshot = current.toString();
        android.content.SharedPreferences names=getSharedPreferences("device_names",MODE_PRIVATE);
        JSONArray named=current.getJSONArray("devices");for(int i=0;i<named.length();i++){JSONObject d=named.getJSONObject(i);String alias=names.getString(d.getString("id"),null);if(alias!=null){d.put("name",alias);d.put("localName",true);}}
        snapshot=current.toString();
        JSONObject disk = new JSONObject(current.toString()); disk.remove("email");
        preferences.edit().putString("snapshot", disk.toString()).apply();
        LampWidgetProvider.renderAll(this);
    }
    void sessionFailure(Exception e) {
        if (String.valueOf(e.getMessage()).contains("USER_SESSION")) {
            cloud.logout(); sessions.clear(); try { updateSnapshot(); } catch (Exception ignored) { }
        }
    }
    static String message(Exception e) {
        String text = String.valueOf(e.getMessage());
        if (text.contains("USER_SESSION")) return "Sua sessão expirou. Abra o aplicativo e entre novamente.";
        if (text.matches(".*(USER_PASSWD_WRONG|USER_NOT_EXIST).*")) return "Confira o e-mail e a senha da conta Positivo.";
        if (text.matches(".*(SIGN|SING_VALIDATE|CLIENT_ID).*")) return "A nuvem recusou esta integração. Ela precisa de atualização.";
        if (e instanceof java.io.IOException) return "Não foi possível acessar a nuvem. Verifique a internet.";
        return text.length() > 220 ? text.substring(0, 220) : text;
    }
    void notifyTile() { try { TileService.requestListeningState(this, new ComponentName(this, LampTileService.class)); } catch (Exception ignored) { } }
    void togglePreferred() throws Exception {
        if (preferred.isEmpty()) { cloud.refresh(); chooseDefault(); }
        requireLamp(preferred); cloud.readDevice(preferred);
        JSONObject d = cloud.member(preferred), c = CloudBackend.control(d, "switch_led|switch|switch_1");
        Object on = c == null ? null : d.getJSONObject("dps").opt(c.getString("id"));
        if (!(on instanceof Boolean)) throw new Exception("O servidor não informou se a lâmpada está ligada.");
        cloud.toggle(preferred, !((Boolean) on)); updateSnapshot();
    }
    void readPreferred() throws Exception { if (cloud.sid.isEmpty()) return; if (preferred.isEmpty()) { cloud.refresh(); chooseDefault(); } else if (CloudBackend.find(cloud.state().getJSONArray("devices"), preferred) == null) cloud.refresh(); if (!preferred.isEmpty()) cloud.readDevice(preferred); updateSnapshot(); }
}
