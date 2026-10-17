package br.com.casacesar.luz;
import org.json.*;
class TriggerCmdChecks {
 interface Checked{void run()throws Exception;}static void check(boolean b,String m){if(!b)throw new AssertionError(m);}static void rejects(Checked r)throws Exception{try{r.run();}catch(Exception e){return;}throw new AssertionError("Unexpected acceptance");}
 public static void main(String[] args)throws Exception{
  JSONObject config=new JSONObject().put("token","fake-test-token-not-a-real-credential").put("computer","Computador de teste").put("trigger","Shut down");int[] reads={0},writes={0};boolean[] reject={false};
  TriggerCmdProtocol api=new TriggerCmdProtocol((route,token,body)->{check(token.equals(config.getString("token")),"Token not passed to transport");if(route.equals("/api/command/commandlist")){reads[0]++;check(body.length()==0,"Validation executed parameters");return new JSONArray().put(new JSONObject().put("name","Shut down"));}check(route.equals("/api/run/trigger"),"Unexpected endpoint");check(body.length()==2&&body.getString("computer").equals("Computador de teste")&&body.getString("trigger").equals("Shut down")&&!body.has("params"),"Wrong or free-form command");writes[0]++;return reject[0]?new JSONObject().put("success",false):new JSONObject().put("message","Trigger sent");});
  check(api.check(config).getBoolean("verified")&&reads[0]==1&&writes[0]==0,"Validation triggered a command");check(api.shutdown(config).getBoolean("accepted")&&writes[0]==1,"Mock shutdown payload");reject[0]=true;rejects(()->api.shutdown(config));rejects(()->api.check(new JSONObject(config.toString()).put("trigger","Missing")));rejects(()->TriggerCmdProtocol.validateConfig(new JSONObject(config.toString()).put("token","abc\r\nX-Test:value")));rejects(()->TriggerCmdProtocol.validateConfig(new JSONObject(config.toString()).put("computer","")));
  System.out.println("PASS: read-only command validation, fixed shutdown target/payload without parameters, rejected receipts and credential/name validation. All transports mocked; no shutdown or real network call.");
 }
}
