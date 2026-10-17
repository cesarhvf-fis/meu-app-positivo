package br.com.casacesar.luz;
import org.json.*;import java.io.File;import java.nio.file.*;import java.nio.charset.StandardCharsets;
class SocketPowerChecks {
 interface Checked{void run()throws Exception;}static void check(boolean b,String m){if(!b)throw new AssertionError(m);}static void rejects(Checked r)throws Exception{try{r.run();}catch(Exception e){return;}throw new AssertionError("Invalid target accepted");}
 public static void main(String[] args)throws Exception {
  CloudApi api=new CloudApi("fixture");CloudBackend cloud=new CloudBackend(api,new PresetStore(new File(args[1]),new String(Files.readAllBytes(Paths.get(args[0])),StandardCharsets.UTF_8)));cloud.sid="test";
  JSONArray controls=new JSONArray().put(new JSONObject().put("id","1").put("code","switch_1").put("type","bool").put("mode","rw")).put(new JSONObject().put("id","19").put("code","cur_power").put("mode","ro").put("scale",1).put("unit","W"));
  JSONObject socket=new JSONObject().put("id","pc").put("gatewayId","gateway").put("gid","home").put("shared",false).put("online",true).put("controls",controls).put("dps",new JSONObject().put("1",true).put("19",600));
  JSONObject shared=new JSONObject(socket.toString()).put("id","shared").put("shared",true),lamp=new JSONObject(socket.toString()).put("id","lamp").put("controls",new JSONArray().put(new JSONObject().put("id","1").put("code","switch_led")));
  cloud.restoreState(new JSONObject().put("devices",new JSONArray().put(socket).put(shared).put(lamp)));int[] writes={0};boolean[] receipt={true},expected={false};
  api.testTransport=(action,data,sid,gid)->{check(action.equals("tuya.m.device.dp.publish"),"Unexpected action");check(data.getString("devId").equals("pc")&&data.getString("gwId").equals("gateway")&&gid.equals("home"),"Wrong target");JSONObject dps=new JSONObject(data.getString("dps"));check(dps.length()==1&&dps.getBoolean("1")==expected[0],"Unexpected payload");writes[0]++;return receipt[0];};
  DeviceExtras extras=new DeviceExtras(cloud);check(!extras.socketPower("pc",false).getBoolean("power")&&!cloud.member("pc").getJSONObject("dps").getBoolean("1"),"Off result");expected[0]=true;check(extras.socketPower("pc",true).getBoolean("accepted")&&cloud.member("pc").getJSONObject("dps").getBoolean("1"),"On result");
  int before=writes[0];rejects(()->extras.socketPower("shared",false));rejects(()->extras.socketPower("lamp",false));rejects(()->extras.socketPower("outside",false));cloud.member("pc").put("online",false);rejects(()->extras.socketPower("pc",false));cloud.member("pc").put("online",true);cloud.member("pc").getJSONArray("controls").getJSONObject(0).put("mode","ro");rejects(()->extras.socketPower("pc",false));check(writes[0]==before,"Invalid target sent command");cloud.member("pc").getJSONArray("controls").getJSONObject(0).put("mode","rw");receipt[0]=false;expected[0]=false;rejects(()->extras.socketPower("pc",false));check(cloud.member("pc").getJSONObject("dps").getBoolean("1"),"Rejected receipt changed state");
  System.out.println("PASS: mock on/off uses only owned socket switch; exact payload, offline/shared/lamp/readonly rejection and failed receipt preserve state. No real device calls.");
 }
}
