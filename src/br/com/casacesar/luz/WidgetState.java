package br.com.casacesar.luz;
import org.json.JSONObject;

/** A cached lamp observation, never a claim that an offline light is currently on. */
final class WidgetState {
    final String status; final boolean lit;
    private WidgetState(String label, boolean on) { status = label; lit = on; }
    static WidgetState from(JSONObject snapshot, String preferred) {
        if (!snapshot.optBoolean("loggedIn")) return new WidgetState("Entre na conta", false);
        JSONObject device = snapshot.optJSONArray("devices") == null ? null : CloudBackend.find(snapshot.optJSONArray("devices"), preferred);
        if (device == null || !CloudBackend.isLamp(device)) return new WidgetState("Escolha uma lâmpada", false);
        if (Boolean.FALSE.equals(device.opt("online"))) return new WidgetState("Offline", false);
        try {
            JSONObject c = CloudBackend.control(device, "switch_led|switch|switch_1");
            Object value = c == null ? null : device.getJSONObject("dps").opt(c.getString("id"));
            if (!(value instanceof Boolean)) return new WidgetState("Estado não informado", false);
            boolean on = (Boolean) value;
            if (!on) return new WidgetState("Desligada", false);
            JSONObject modeControl = CloudBackend.control(device, "work_mode");
            String mode = modeControl == null ? "" : device.getJSONObject("dps").optString(modeControl.getString("id"), "");
            String label;
            switch (mode) {
                case "white": label = "Branco"; break;
                case "colour": label = "Cores"; break;
                case "scene": label = "Efeitos"; break;
                case "music": label = "Música"; break;
                default: label = "não informado";
            }
            return new WidgetState("Ligada: modo " + label, true);
        } catch (Exception e) { return new WidgetState("Estado não informado", false); }
    }
}
