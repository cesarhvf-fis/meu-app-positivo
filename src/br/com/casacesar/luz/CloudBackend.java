package br.com.casacesar.luz;

import java.time.Instant;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/** Network calls run on the application's single worker, never on the UI thread. */
final class CloudBackend {
    final CloudApi api;
    final PresetStore presets;
    String sid = "", email = "";
    private JSONArray homes = new JSONArray(), devices = new JSONArray(), warnings = new JSONArray();
    private final Map<String, Long> timerQueries = new HashMap<>();
    CloudBackend(CloudApi api, PresetStore store) { this.api = api; presets = store; }
    JSONObject state() throws Exception { return new JSONObject().put("loggedIn", !sid.isEmpty()).put("email", email).put("homes", homes).put("devices", devices).put("warnings", warnings); }
    void restoreState(JSONObject saved) { homes = saved.optJSONArray("homes") == null ? new JSONArray() : saved.optJSONArray("homes"); devices = saved.optJSONArray("devices") == null ? new JSONArray() : saved.optJSONArray("devices"); }
    void logout() { sid = ""; email = ""; homes = new JSONArray(); devices = new JSONArray(); warnings = new JSONArray(); }
    void login(String mail, String password) throws Exception {
        if (!mail.contains("@") || password.isEmpty()) throw new Exception("Informe e-mail e senha.");
        logout(); JSONObject token = object(api.call("tuya.m.user.email.token.create", new JSONObject().put("countryCode", 55).put("email", mail), null, null));
        String encrypted = MobileCrypto.encryptPassword(token.getString("publicKey"), token.getString("exponent"), password);
        JSONObject result = object(api.call("tuya.m.user.email.password.login", new JSONObject().put("countryCode", "55").put("email", mail).put("ifencrypt", 1)
                .put("options", "{\"group\":1}").put("passwd", encrypted).put("token", token.getString("token")), null, null));
        sid = result.getString("sid"); email = mail;
        if (sid.isEmpty()) throw new Exception("Login sem sessão.");
    }
    JSONObject refresh() throws Exception {
        requireSession(); JSONArray locations = list(api.call("tuya.m.location.list", null, sid, null));
        JSONArray next = new JSONArray(), nextHomes = new JSONArray(), nextWarnings = new JSONArray();
        for (int i = 0; i < locations.length(); i++) {
            JSONObject home = locations.getJSONObject(i); String gid = string(home, "groupId", "gid", "id"); if (gid.isEmpty()) throw new Exception("Casa sem identificador.");
            String name = home.optString("name", "Minha casa"); nextHomes.put(new JSONObject().put("id", gid).put("name", name));
            JSONArray members = list(api.call("tuya.m.my.group.device.list", null, sid, gid));
            for (int j = 0; j < members.length(); j++) next.put(enrich(members.getJSONObject(j), gid, false).put("home", name));
        }
        try {
            JSONArray shared = list(api.call("tuya.m.my.shared.device.list", null, sid, null));
            for (int i = 0; i < shared.length(); i++) { JSONObject item = shared.getJSONObject(i); if (find(next, item.optString("devId")) == null) next.put(enrich(item, "", true)); }
        } catch (Exception e) { nextWarnings.put("Não foi possível carregar as lâmpadas compartilhadas."); }
        homes = nextHomes; devices = next; warnings = nextWarnings; return state();
    }
    private JSONObject enrich(JSONObject item, String gid, boolean shared) throws Exception {
        JSONObject info = new JSONObject(item.toString());
        try { JSONObject extra = object(api.call("tuya.m.device.get", new JSONObject().put("devId", item.getString("devId")), sid, null)); for (Iterator<String> it = extra.keys(); it.hasNext();) { String k = it.next(); info.put(k, extra.get(k)); } } catch (Exception ignored) { }
        JSONObject dps = objectOrEmpty(info.opt("dps")); String id = info.optString("devId", item.optString("devId")); Object online = online(info);
        return new JSONObject().put("id", id).put("gatewayId", info.optString("gwId", id)).put("gid", gid).put("name", info.optString("name", "Dispositivo"))
                .put("home", shared ? "Compartilhado" : "Minha casa").put("shared", shared).put("online", online).put("category", info.optString("category"))
                .put("lastOnline", lastOnline(info)).put("controls", controls(info)).put("dps", dps);
    }
    JSONObject readDevice(String id) throws Exception {
        JSONObject old = member(id), info = object(api.call("tuya.m.device.get", new JSONObject().put("devId", id), sid, null));
        old.put("dps", info.has("dps") ? objectOrEmpty(info.opt("dps")) : old.getJSONObject("dps"));
        if (online(info) instanceof Boolean) old.put("online", online(info));
        Object last = lastOnline(info); if (last != JSONObject.NULL) old.put("lastOnline", last);
        return state();
    }
    JSONObject member(String id) throws Exception { requireSession(); JSONObject d = find(devices, id); if (d == null) throw new Exception("Dispositivo não pertence à sessão atual."); return d; }
    static JSONObject find(JSONArray list, String id) { for (int i = 0; i < list.length(); i++) { JSONObject d = list.optJSONObject(i); if (d != null && d.optString("id").equals(id)) return d; } return null; }
    static JSONObject control(JSONObject d, String regex) throws Exception {
        JSONArray all = d.getJSONArray("controls"); for (int i = 0; i < all.length(); i++) { JSONObject c = all.getJSONObject(i); if (c.optString("code").matches(regex)) return c; } return null;
    }
    static boolean isLamp(JSONObject d) { try { return control(d, "switch_led|bright_value(_v2)?") != null; } catch (Exception e) { return false; } }
    JSONObject toggle(String id, boolean power) throws Exception {
        JSONObject d = member(id); if (!isLamp(d)) throw new Exception("Este aplicativo controla apenas lâmpadas.");
        JSONObject c = control(d, "switch_led|switch|switch_1"); if (c == null) throw new Exception("Sem controle liga/desliga.");
        return publish(id, new JSONObject().put(c.getString("id"), power));
    }
    JSONObject lamp(String id, JSONObject values) throws Exception { return publish(id, buildLamp(id, values)); }
    JSONObject buildLamp(String id, JSONObject input) throws Exception {
        JSONObject d = member(id), changes = new JSONObject();
        if (input.has("power")) add(changes, control(d, "switch_led|switch|switch_1"), input.get("power"));
        if (input.has("mode")) {
            String mode = input.getString("mode"); if (!mode.matches("white|colour|scene")) throw new Exception("Modo não suportado."); add(changes, control(d, "work_mode"), mode);
            if (mode.equals("white")) { if (input.has("bright")) numeric(changes, control(d, "bright_value(_v2)?"), input.getDouble("bright")); if (input.has("temp")) numeric(changes, control(d, "temp_value(_v2)?"), input.getDouble("temp")); }
            if (mode.equals("colour")) { for (String k : new String[]{"h", "s", "v"}) if (!(input.get(k) instanceof Number) || input.getDouble(k) != Math.rint(input.getDouble(k))) throw new Exception("Cor inválida."); add(changes, control(d, "colour_data(_v2)?"), PresetStore.color(input.getInt("h"), input.getInt("s"), input.getInt("v"))); }
            if (mode.equals("scene")) { double n = input.getDouble("effect"); if (n != Math.rint(n) || n < 0 || n > 7) throw new Exception("Efeito inválido."); add(changes, control(d, "scene_data(_v2)?"), PresetStore.scene((int) n, presets.load().getJSONArray("presets").getJSONObject((int) n))); }
        }
        return changes;
    }
    private static void add(JSONObject out, JSONObject c, Object value) throws Exception { if (c == null) throw new Exception("A lâmpada não oferece esse controle."); out.put(c.getString("id"), value); }
    private static void numeric(JSONObject out, JSONObject c, double pct) throws Exception {
        if (c == null || !c.has("min") || !c.has("max") || !Double.isFinite(pct) || pct < 0 || pct > 100) throw new Exception("Faixa de controle inválida.");
        int min = c.getInt("min"), max = c.getInt("max"), step = c.optInt("step", 1); if (step <= 0) throw new Exception("Passo inválido.");
        add(out, c, Math.max(min, Math.min(max, min + Math.round((pct * max / 100 - min) / step) * step)));
    }
    void validate(String id, JSONObject changes, boolean allowOffline) throws Exception {
        JSONObject d = member(id); if (!allowOffline && d.opt("online") == Boolean.FALSE) throw new Exception("Lâmpada offline.");
        if (changes.length() < 1 || changes.length() > 8) throw new Exception("Comando inválido."); JSONArray schema = d.getJSONArray("controls");
        for (Iterator<String> it = changes.keys(); it.hasNext();) {
            String idDp = it.next(); Object value = changes.get(idDp); JSONObject c = null;
            for (int i = 0; i < schema.length(); i++) if (schema.getJSONObject(i).optString("id").equals(idDp)) c = schema.getJSONObject(i);
            if (c == null || !c.optString("mode").matches("rw|wr") || !c.optString("code").matches("switch|switch_led|switch_[1-8]|bright_value(_v2)?|temp_value(_v2)?|work_mode|colour_data(_v2)?|scene_data(_v2)?|power_memory|do_not_disturb")) throw new Exception("Controle não permitido.");
            String type = c.optString("type"), code = c.optString("code");
            if (code.matches("power_memory|do_not_disturb") && !isLamp(d)) throw new Exception("Estado inicial é exclusivo de lâmpadas.");
            if (type.equals("bool")) { if (!(value instanceof Boolean)) throw new Exception("Valor booleano inválido."); }
            else if (type.equals("value")) { if (!(value instanceof Number)) throw new Exception("Valor inválido."); double n = ((Number) value).doubleValue(); int min = c.getInt("min"), max = c.getInt("max"), step = c.optInt("step", 1); if (!Double.isFinite(n) || n != Math.rint(n) || n < min || n > max || step <= 0 || (n - min) % step != 0) throw new Exception("Valor fora da faixa."); }
            else if (type.equals("enum")) { JSONArray choices = c.getJSONArray("range"); boolean found = false; for (int i = 0; i < choices.length(); i++) if (choices.get(i).equals(value)) found = true; if (!found) throw new Exception("Modo inválido."); }
            else if (type.equals("string") && code.matches("colour_data(_v2)?")) { if (!(value instanceof String) || !PresetStore.validColor((String) value) || !PresetStore.validColor(d.getJSONObject("dps").optString(idDp))) throw new Exception("Formato de cor não reconhecido."); }
            else if (type.equals("string") && code.matches("scene_data(_v2)?")) { if (!(value instanceof String) || !PresetStore.validScene((String) value) || !PresetStore.validScene(d.getJSONObject("dps").optString(idDp))) throw new Exception("Formato de efeito não reconhecido."); }
            else if (type.equals("raw") && code.equals("power_memory")) { if (!(value instanceof String)) throw new Exception("Memória inválida."); DeviceExtras.decodeMemory((String)value); }
            else throw new Exception("Controle não suportado.");
        }
    }
    JSONObject publish(String id, JSONObject changes) throws Exception {
        validate(id, changes, false); JSONObject d = member(id);
        Object receipt = api.call("tuya.m.device.dp.publish", new JSONObject().put("devId", id).put("gwId", d.getString("gatewayId")).put("dps", changes.toString()), sid, d.optString("gid"));
        if (receipt == null || receipt == JSONObject.NULL || Boolean.FALSE.equals(receipt)) throw new Exception("Servidor não confirmou o comando.");
        JSONObject cached = d.getJSONObject("dps"); for (Iterator<String> it = changes.keys(); it.hasNext();) { String key = it.next(); cached.put(key, changes.get(key)); }
        return new JSONObject().put("accepted", true);
    }
    JSONObject timers(String id) throws Exception {
        JSONObject d = member(id); if (d.optBoolean("shared")) throw new Exception("Agendamento disponível apenas para sua própria lâmpada.");
        long delay = 2200 - (System.currentTimeMillis() - timerQueries.getOrDefault(id, 0L)); if (delay > 0) Thread.sleep(delay);
        JSONArray groups = list(api.call("tuya.m.timer.all.list", new JSONObject().put("bizId", id).put("bizType", 0).put("devId", id), sid, d.optString("gid"))), result = new JSONArray(); timerQueries.put(id, System.currentTimeMillis());
        for (int i = 0; i < groups.length(); i++) {
            JSONObject category = groups.getJSONObject(i); JSONArray nested = category.optJSONArray("groups"); if (nested == null) continue;
            for (int j = 0; j < nested.length(); j++) { JSONArray all = nested.getJSONObject(j).optJSONArray("timers"); if (all == null) continue;
                for (int k = 0; k < all.length(); k++) { JSONObject t = all.getJSONObject(k); result.put(new JSONObject().put("id", string(t, "timerId", "id")).put("time", t.optString("time"))
                        .put("loops", t.optString("loops", "0")).put("status", t.optInt("status")).put("name", t.optString("aliasName", "Agendamento"))
                        .put("dps", objectOrEmpty(t.opt("dps"))).put("isAppPush", t.optBoolean("isAppPush")).put("timezone", t.optString("timezoneId", "America/Sao_Paulo"))); }
            }
        }
        return new JSONObject().put("timers", result).put("timezone", "America/Sao_Paulo");
    }
    JSONObject timerSave(String id, JSONObject input) throws Exception {
        JSONObject d = member(id); if (d.optBoolean("shared")) throw new Exception("Sem permissão para agendar este dispositivo.");
        if (!input.optString("time").matches("([01]\\d|2[0-3]):[0-5]\\d") || !input.optString("loops").matches("0|[01]{7}")) throw new Exception("Horário ou dias inválidos.");
        JSONObject previous = null; String timerId = input.optString("id");
        if (!timerId.isEmpty()) { JSONArray all = timers(id).getJSONArray("timers"); for (int i = 0; i < all.length(); i++) if (all.getJSONObject(i).optString("id").equals(timerId)) previous = all.getJSONObject(i); if (previous == null) throw new Exception("Agendamento não pertence à lâmpada."); }
        JSONObject changes = buildLamp(id, input.getJSONObject("values")); validate(id, changes, true);
        String name = input.optString("name", "Agendamento Android"); if (name.length() > 40) name = name.substring(0, 40);
        JSONObject data = new JSONObject().put("bizId", id).put("bizType", 0).put("loops", input.getString("loops")).put("status", input.optBoolean("enabled", true) ? 1 : 0)
                .put("actions", new JSONObject().put("dps", changes).put("time", input.getString("time")).toString()).put("isAppPush", previous != null && previous.optBoolean("isAppPush")).put("aliasName", name);
        if (previous == null) data.put("category", "switch_led"); else data.put("id", timerId);
        Object receipt = api.call(previous == null ? "tuya.m.clock.dps.add" : "tuya.m.clock.dps.update", data, sid, d.optString("gid"));
        if (receipt == null || receipt == JSONObject.NULL || Boolean.FALSE.equals(receipt)) throw new Exception("Servidor não confirmou o agendamento.");
        return new JSONObject().put("saved", true);
    }
    JSONObject timerStatus(String id, String timerId, int status) throws Exception {
        if (status < 0 || status > 2) throw new Exception("Status inválido."); JSONArray all = timers(id).getJSONArray("timers"); boolean found = false;
        for (int i = 0; i < all.length(); i++) if (all.getJSONObject(i).optString("id").equals(timerId)) found = true;
        if (!found) throw new Exception("Agendamento não pertence à lâmpada.");
        Object receipt = api.call("tuya.m.clock.batch.status.update", new JSONObject().put("bizId", id).put("bizType", 0).put("ids", timerId).put("status", status), sid, null);
        if (!Boolean.TRUE.equals(receipt)) throw new Exception("Servidor não confirmou a alteração."); return new JSONObject().put("saved", true);
    }
    private void requireSession() throws Exception { if (sid.isEmpty()) throw new Exception("Entre na sua conta Positivo."); }
    static JSONObject object(Object value) throws Exception { if (value instanceof JSONObject) return (JSONObject) value; if (value instanceof String) return new JSONObject((String) value); throw new Exception("Resposta não reconhecida."); }
    static JSONObject objectOrEmpty(Object value) { try { return object(value); } catch (Exception e) { return new JSONObject(); } }
    static JSONArray list(Object value) throws Exception { if (value instanceof String) value = new JSONArray((String) value); if (value instanceof JSONArray) return (JSONArray) value; if (value instanceof JSONObject) for (String k : new String[]{"list", "devices", "groups", "result"}) { JSONArray a = ((JSONObject) value).optJSONArray(k); if (a != null) return a; } throw new Exception("Lista não reconhecida."); }
    private static String string(JSONObject object, String... keys) { for (String key : keys) if (object.has(key) && !object.isNull(key)) return String.valueOf(object.opt(key)); return ""; }
    private static Object online(JSONObject info) { Object value = info.opt("isOnline"); if (value instanceof Boolean) return value; JSONObject wifi = objectOrEmpty(objectOrEmpty(info.opt("moduleMap")).opt("wifi")); return wifi.opt("isOnline") instanceof Boolean ? wifi.opt("isOnline") : JSONObject.NULL; }
    static Object lastOnline(JSONObject info) {
        Object value = info.opt("lastOnlineTime"); if (value == null || value == JSONObject.NULL) value = info.opt("lastOnlineAt"); if (value == null || value == JSONObject.NULL) value = info.opt("last_online_time");
        try { String raw = String.valueOf(value); long ms; if (value instanceof Number || raw.matches("\\d{10,13}")) { long n = Long.parseLong(raw); ms = n < 1000000000000L ? n * 1000 : n; } else { if (!raw.matches("\\d{4}-\\d\\d-\\d\\dT.*(Z|[+-]\\d\\d:\\d\\d)")) return JSONObject.NULL; ms = Instant.parse(raw).toEpochMilli(); } return ms >= 946684800000L && ms <= System.currentTimeMillis() + 60000L ? Instant.ofEpochMilli(ms).toString() : JSONObject.NULL; } catch (Exception e) { return JSONObject.NULL; }
    }
    private static JSONArray controls(JSONObject info) throws Exception {
        Object raw = info.opt("schema"); if (raw == null) raw = info.opt("productSchema"); if (raw == null) raw = info.opt("schemaInfo");
        if (raw instanceof String) { try { raw = ((String) raw).trim().startsWith("[") ? new JSONArray((String) raw) : new JSONObject((String) raw); } catch (Exception ignored) { raw = new JSONArray(); } }
        if (raw instanceof JSONObject) raw = ((JSONObject) raw).optJSONArray("schema"); JSONArray schema = raw instanceof JSONArray ? (JSONArray) raw : new JSONArray(), out = new JSONArray();
        for (int i = 0; i < schema.length(); i++) {
            JSONObject dp = schema.getJSONObject(i), prop = objectOrEmpty(dp.opt("property")); if (!dp.has("id")) continue;
            JSONObject c = new JSONObject().put("id", String.valueOf(dp.get("id"))).put("code", dp.optString("code")).put("name", dp.optString("name", dp.optString("code")))
                    .put("type", prop.optString("type", dp.optString("type"))).put("mode", dp.optString("mode", "ro"));
            for (String k : new String[]{"min", "max", "step", "scale", "unit", "range"}) if (prop.has(k)) c.put(k, prop.get(k));
            if (!c.has("step")) c.put("step", 1); if (!c.has("range")) c.put("range", new JSONArray()); out.put(c);
        }
        return out;
    }
}
