package br.com.casacesar.luz;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.json.JSONArray;
import org.json.JSONObject;

final class PresetStore {
    private final File file;
    private final JSONObject defaults;
    PresetStore(File directory, String initial) throws Exception { file = new File(directory, "effects.json"); defaults = new JSONObject(initial); }
    JSONObject load() throws Exception {
        JSONObject all = file.exists() ? new JSONObject(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)) : new JSONObject(defaults.toString());
        if (all.optInt("version") != 1 || all.optJSONArray("presets") == null || all.getJSONArray("presets").length() != 8) throw new Exception("Arquivo de presets incompatível.");
        JSONArray normalized = new JSONArray();
        for (int i = 0; i < 8; i++) {
            JSONObject p = all.getJSONArray("presets").getJSONObject(i);
            if (!p.has("icon")) p.put("icon", defaults.getJSONArray("presets").getJSONObject(i).getString("icon"));
            normalized.put(normalize(p));
        }
        return new JSONObject().put("version", 1).put("presets", normalized);
    }
    JSONObject save(int index, JSONObject value, boolean restore) throws Exception {
        if (index < 0 || index > 7) throw new Exception("Preset inválido.");
        JSONObject all = load(); all.getJSONArray("presets").put(index, normalize(restore ? defaults.getJSONArray("presets").getJSONObject(index) : value));
        File temp = new File(file.getParentFile(), "effects.tmp");
        Files.write(temp.toPath(), all.toString(2).getBytes(StandardCharsets.UTF_8));
        if (file.exists()) Files.copy(file.toPath(), new File(file.getParentFile(), "effects.backup.json").toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.move(temp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return all;
    }
    static JSONObject normalize(JSONObject p) throws Exception {
        if (p == null) throw new Exception("Preset ausente.");
        String name = p.optString("name").trim(), transition = p.optString("transition"), icon = p.optString("icon", "soft");
        if (name.isEmpty() || name.length() > 32) throw new Exception("Nome deve ter de 1 a 32 caracteres.");
        if (!transition.matches("static|jump|gradient")) throw new Exception("Transição inválida.");
        double speed = p.optDouble("speed", Double.NaN);
        if (!Double.isFinite(speed) || speed != Math.rint(speed) || speed < 1 || speed > 100) throw new Exception("Velocidade deve estar entre 1 e 100.");
        JSONArray raw = p.optJSONArray("units"), units = new JSONArray();
        if (raw == null || raw.length() < 1 || raw.length() > 8 || (transition.equals("static") && raw.length() != 1)) throw new Exception("Use de 1 a 8 etapas; luz fixa usa apenas uma.");
        for (int i = 0; i < raw.length(); i++) {
            JSONObject u = raw.getJSONObject(i), n = new JSONObject(); String kind = u.optString("kind"); n.put("kind", kind);
            if (kind.equals("white")) { n.put("bright", percent(u, "bright", 1)); n.put("temp", percent(u, "temp", 0)); }
            else if (kind.equals("colour")) {
                double h = u.optDouble("h", Double.NaN); if (!Double.isFinite(h) || h < 0 || h > 360) throw new Exception("Matiz deve estar entre 0 e 360.");
                n.put("h", Math.round(h)); n.put("s", percent(u, "s", 0)); n.put("v", percent(u, "v", 1));
            } else throw new Exception("Tipo de etapa inválido.");
            units.put(n);
        }
        if (!icon.matches("night|read|work|leisure|soft|party|fun|rainbow|photo")) throw new Exception("Ícone inválido.");
        JSONObject out = new JSONObject().put("name", name).put("transition", transition).put("speed", (int) speed).put("units", units).put("icon", icon);
        if (icon.equals("photo")) { String image = p.optString("image"); if (!image.matches("[a-f0-9]{32}\\.png")) throw new Exception("Imagem inválida."); out.put("image", image); }
        return out;
    }
    private static double percent(JSONObject value, String key, double min) throws Exception {
        double n = value.optDouble(key, Double.NaN);
        if (!Double.isFinite(n) || n < min || n > 100) throw new Exception("Percentual fora da faixa.");
        return Math.round(n * 100) / 100.0;
    }
    static String color(int h, int s, int v) throws Exception {
        if (h < 0 || h > 360 || s < 0 || s > 1000 || v < 0 || v > 1000) throw new Exception("Cor fora da faixa.");
        return hex(h, 4) + hex(s, 4) + hex(v, 4);
    }
    static String scene(int index, JSONObject preset) throws Exception {
        if (index < 0 || index > 7) throw new Exception("Preset inválido.");
        JSONObject p = normalize(preset); String result = hex(index, 2); int mode = p.getString("transition").equals("static") ? 0 : p.getString("transition").equals("jump") ? 1 : 2;
        int duration = 101 - p.getInt("speed"); JSONArray units = p.getJSONArray("units");
        for (int i = 0; i < units.length(); i++) {
            JSONObject u = units.getJSONObject(i); boolean white = u.getString("kind").equals("white");
            result += hex(duration, 2) + hex(duration, 2) + hex(mode, 2)
                    + (white ? color(0, 0, 0) : color(u.getInt("h"), (int) Math.round(u.getDouble("s") * 10), (int) Math.round(u.getDouble("v") * 10)))
                    + hex(white ? (int) Math.round(u.getDouble("bright") * 10) : 0, 4) + hex(white ? (int) Math.round(u.getDouble("temp") * 10) : 0, 4);
        }
        return result;
    }
    static boolean validColor(String s) {
        return s != null && s.matches("[a-fA-F0-9]{12}") && Integer.parseInt(s.substring(0, 4), 16) <= 360
                && Integer.parseInt(s.substring(4, 8), 16) <= 1000 && Integer.parseInt(s.substring(8), 16) <= 1000;
    }
    static boolean validScene(String s) {
        if (s == null || !s.matches("[a-fA-F0-9]+") || s.length() < 28 || s.length() > 210 || (s.length() - 2) % 26 != 0 || Integer.parseInt(s.substring(0, 2), 16) > 7) return false;
        for (int i = 2; i < s.length(); i += 26) {
            if (Integer.parseInt(s.substring(i, i + 2), 16) > 100 || Integer.parseInt(s.substring(i + 2, i + 4), 16) > 100
                    || Integer.parseInt(s.substring(i + 4, i + 6), 16) > 2 || !validColor(s.substring(i + 6, i + 18))
                    || Integer.parseInt(s.substring(i + 18, i + 22), 16) > 1000 || Integer.parseInt(s.substring(i + 22, i + 26), 16) > 1000) return false;
        }
        return true;
    }
    private static String hex(int n, int width) { return String.format(java.util.Locale.ROOT, "%0" + width + "x", n); }
}
