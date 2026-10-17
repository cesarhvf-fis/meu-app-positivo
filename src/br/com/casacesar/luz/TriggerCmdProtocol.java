package br.com.casacesar.luz;
import java.net.*;import java.io.*;import java.nio.charset.StandardCharsets;import org.json.*;

/** Fixed HTTPS endpoints and fixed configured command. No shell or free-form parameters. */
final class TriggerCmdProtocol {
 interface Transport { Object post(String route,String token,JSONObject body)throws Exception; }
 private final Transport transport;
 TriggerCmdProtocol(){transport=TriggerCmdProtocol::https;}
 TriggerCmdProtocol(Transport mock){transport=mock;}
 static void validateConfig(JSONObject c)throws Exception{
  String token=c.optString("token").trim();if(token.length()<20||token.length()>4096||token.matches("(?s).*[\\s\\p{Cntrl}].*"))throw new Exception("Informe o token do TRIGGERcmd.");
  for(String k:new String[]{"computer","trigger"}){String text=c.optString(k).trim();if(text.isEmpty()||text.length()>100||text.matches("(?s).*[\\p{Cntrl}].*"))throw new Exception("Informe o computador e o comando cadastrados.");c.put(k,text);}c.put("token",token);
 }
 JSONObject check(JSONObject config)throws Exception{
  validateConfig(config);Object raw=transport.post("/api/command/commandlist",config.getString("token"),new JSONObject());
  JSONArray commands=raw instanceof JSONArray?(JSONArray)raw:raw instanceof JSONObject?((JSONObject)raw).optJSONArray("commands"):null;
  if(commands==null)throw new Exception("O TRIGGERcmd não retornou a lista de comandos esperada.");
  boolean found=false;for(int i=0;i<commands.length();i++){JSONObject command=commands.optJSONObject(i);if(command!=null&&command.optString("name").equals(config.getString("trigger")))found=true;}
  if(!found)throw new Exception("O comando configurado não apareceu na sua conta TRIGGERcmd.");
  return new JSONObject().put("verified",true).put("message","Comando encontrado. Nenhum comando foi executado.");
 }
 JSONObject shutdown(JSONObject config)throws Exception{
  validateConfig(config);JSONObject body=new JSONObject().put("computer",config.getString("computer")).put("trigger",config.getString("trigger"));
  Object raw=transport.post("/api/run/trigger",config.getString("token"),body);
  if(Boolean.FALSE.equals(raw)||raw==null||raw==JSONObject.NULL)throw new Exception("O TRIGGERcmd não confirmou o envio.");
  if(raw instanceof JSONObject){JSONObject receipt=(JSONObject)raw;if(receipt.has("error")||receipt.has("errorCode")||(receipt.has("success")&&!receipt.optBoolean("success")))throw new Exception("O TRIGGERcmd recusou o comando.");}
  return new JSONObject().put("accepted",true).put("message","Pedido de desligamento enviado ao TRIGGERcmd. A confirmação de envio não garante que o Windows já desligou.");
 }
 private static Object https(String route,String token,JSONObject body)throws Exception{
  if(!route.equals("/api/command/commandlist")&&!route.equals("/api/run/trigger"))throw new Exception("Consulta não permitida.");
  HttpURLConnection connection=(HttpURLConnection)new URL("https://www.triggercmd.com"+route).openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(12000);connection.setReadTimeout(15000);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Authorization","Bearer "+token);connection.setRequestProperty("Content-Type","application/json");
  try{byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);connection.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=connection.getOutputStream()){out.write(bytes);}int code=connection.getResponseCode();if(code==401||code==403)throw new Exception("O token TRIGGERcmd foi recusado. Confira a configuração.");if(code==429)throw new Exception("O TRIGGERcmd pediu uma pausa. Aguarde antes de tentar novamente.");if(code<200||code>=300)throw new Exception("O TRIGGERcmd não confirmou a operação (HTTP "+code+").");
   ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=connection.getInputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>131072)throw new Exception("Resposta TRIGGERcmd muito grande.");out.write(buffer,0,n);}}
   String text=new String(out.toByteArray(),StandardCharsets.UTF_8).trim();if(text.startsWith("["))return new JSONArray(text);if(text.startsWith("{"))return new JSONObject(text);throw new Exception("Resposta TRIGGERcmd não reconhecida.");
  }finally{connection.disconnect();}
 }
}
