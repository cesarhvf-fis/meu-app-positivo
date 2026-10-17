package br.com.casacesar.luz;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.time.YearMonth;
import org.json.JSONArray;
import org.json.JSONObject;

/** Explicit device-scoped operations; socket telemetry is read-only. */
final class DeviceExtras {
    private final CloudBackend cloud;
    private final java.util.Map<String,JSONObject> energySummaries=new java.util.HashMap<>();
    private final java.util.Map<String,Long> energyAttempts=new java.util.HashMap<>();
    private final java.util.Map<String,String> energyWarnings=new java.util.HashMap<>();
    private String energySession="";
    private JSONObject summary(JSONObject d,boolean force) throws Exception {
        if(!cloud.sid.equals(energySession)){energySummaries.clear();energyAttempts.clear();energyWarnings.clear();energySession=cloud.sid;}
        String id=d.getString("id");long now=System.currentTimeMillis();Long last=energyAttempts.get(id);
        if(!force&&last!=null&&now-last<60000)return energySummaries.get(id);
        JSONObject c=CloudBackend.control(d,"add_ele|total_energy|total_forward_energy|total_ele");
        if(c==null)return null;
        energyAttempts.put(id,now);
        JSONObject raw=CloudBackend.object(cloud.api.call("tuya.m.dp.stat.month.list",new JSONObject().put("devId",id).put("dpId",Integer.parseInt(c.getString("id"))).put("type","sum"),cloud.sid,d.optString("gid")));
        if(!kwh(raw.opt("sum")).optBoolean("available"))throw new Exception("Resumo de consumo incompleto.");
        energyWarnings.remove(id);energySummaries.put(id,raw);return raw;
    }
    static JSONObject kwh(Object raw) throws Exception {
        if(raw==null||raw==JSONObject.NULL)return new JSONObject().put("available",false);
        double value;try{value=Double.parseDouble(raw.toString());}catch(NumberFormatException e){return new JSONObject().put("available",false);}
        if(!Double.isFinite(value)||value<0)return new JSONObject().put("available",false);
        // Statistics endpoints already return kWh; do not reuse the DP's scale.
        return new JSONObject().put("available",true).put("value",value).put("unit","kWh");
    }
    DeviceExtras(CloudBackend backend) { cloud = backend; }
    static void requireSocket(CloudBackend cloud,String id) throws Exception {JSONObject d=cloud.member(id);if(d.optBoolean("shared")||CloudBackend.isLamp(d)||CloudBackend.control(d,"cur_current|cur_power|cur_voltage")==null)throw new Exception("A rotina exige uma tomada com medição da sua conta.");}
    private JSONObject lamp(String id, boolean owned) throws Exception {
        JSONObject d=cloud.member(id);
        if(!CloudBackend.isLamp(d) || (owned && d.optBoolean("shared"))) throw new Exception("Esta ação exige uma lâmpada da sua conta.");
        return d;
    }
    static String name(String raw) throws Exception {
        String n=raw.trim(); if(n.isEmpty()||n.length()>40||n.matches("(?s).*[\\p{Cntrl}].*"))throw new Exception("Use um nome de 1 a 40 caracteres.");return n;
    }
    JSONObject rename(String id,String raw) throws Exception {
        JSONObject d=cloud.member(id);String n=name(raw);
        Object result=cloud.api.call("tuya.m.device.name.update",new JSONObject().put("devId",id).put("name",n),cloud.sid,d.optString("gid"));
        if(!Boolean.TRUE.equals(result))throw new Exception("A nuvem não confirmou o novo nome. Você pode salvar um nome somente neste aplicativo.");
        d.put("name",n);d.remove("localName");return new JSONObject().put("saved",true).put("name",n).put("local",false);
    }
    JSONObject shares(String id) throws Exception {
        lamp(id,true);JSONArray raw=CloudBackend.list(cloud.api.call("tuya.m.sharing.device.member.list",new JSONObject().put("devId",id),cloud.sid,null)),out=new JSONArray();
        for(int i=0;i<raw.length();i++){JSONObject row=raw.getJSONObject(i);String relation=row.optString("id",row.optString("relationId"));if(relation.isEmpty())continue;out.put(new JSONObject().put("id",relation).put("name",row.optString("name",row.optString("username","Pessoa compartilhada"))).put("email",row.optString("username",row.optString("mobile"))));}
        return new JSONObject().put("deviceId",id).put("members",out);
    }
    JSONObject shareAdd(String id,String email) throws Exception {
        JSONObject d=lamp(id,true);String account=email.trim().toLowerCase(java.util.Locale.ROOT);
        if(account.length()>254||!account.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new Exception("Informe um e-mail válido da conta Positivo.");
        Object receipt=cloud.api.call("tuya.m.sharing.device.account.add",new JSONObject().put("countryCode","55").put("userAccount",account).put("groupId",d.getString("gid")).put("devIds",new JSONArray().put(id)),cloud.sid,null,"3.0");
        confirmed(receipt);return new JSONObject().put("saved",true);
    }
    JSONObject shareRemove(String id,String relation) throws Exception {
        lamp(id,true);JSONArray members=shares(id).getJSONArray("members");boolean found=false;
        for(int i=0;i<members.length();i++)if(members.getJSONObject(i).getString("id").equals(relation))found=true;
        if(!found)throw new Exception("Compartilhamento não pertence à lâmpada selecionada.");
        confirmed(cloud.api.call("tuya.m.sharing.device.remove",new JSONObject().put("devId",id).put("relationId",relation),cloud.sid,null,"3.0"));
        return new JSONObject().put("saved",true);
    }
    private static void confirmed(Object value) throws Exception {if(value==null||value==JSONObject.NULL||Boolean.FALSE.equals(value))throw new Exception("A nuvem não confirmou a alteração.");}
    static byte[] decodeMemory(String text) throws Exception {
        byte[] bytes;try{bytes=Base64.getDecoder().decode(text);}catch(Exception e){throw new Exception("Formato de estado inicial não reconhecido.");}
        if(bytes.length!=12||bytes[0]!=0||bytes[1]<0||bytes[1]>2)throw new Exception("Versão de estado inicial não compatível.");
        ByteBuffer b=ByteBuffer.wrap(bytes);b.position(2);int[] max={360,1000,1000,1000,1000};for(int m:max)if((b.getShort()&65535)>m)throw new Exception("Parâmetros de estado inicial inválidos.");return bytes;
    }
    JSONObject startup(String id) throws Exception {
        JSONObject d=lamp(id,false);cloud.readDevice(id);JSONObject c=CloudBackend.control(d,"power_memory"),disturb=CloudBackend.control(d,"do_not_disturb");JSONObject result=new JSONObject().put("deviceId",id).put("name",d.optString("name")).put("supported",false).put("disturbSupported",disturb!=null&&disturb.optString("mode").matches("rw|wr"));
        if(disturb!=null)result.put("doNotDisturb",d.getJSONObject("dps").opt(disturb.getString("id")));
        if(c!=null&&c.optString("type").equals("raw")&&c.optString("mode").matches("rw|wr")){try{byte[] bytes=decodeMemory(d.getJSONObject("dps").getString(c.getString("id")));ByteBuffer b=ByteBuffer.wrap(bytes);b.position(2);int h=b.getShort()&65535,s=b.getShort()&65535,v=b.getShort()&65535,bright=b.getShort()&65535,temp=b.getShort()&65535;result.put("supported",true).put("mode",bytes[1]).put("custom",new JSONObject().put("kind",bright==0?"colour":"white").put("h",h).put("s",s/10.0).put("v",Math.max(1,v/10.0)).put("bright",Math.max(1,bright/10.0)).put("temp",temp/10.0));}catch(Exception e){result.put("reason",e.getMessage());}}
        return result;
    }
    JSONObject startupSave(String id,JSONObject input) throws Exception {
        JSONObject d=lamp(id,false),changes=new JSONObject();
        if(input.has("doNotDisturb")){if(!(input.get("doNotDisturb") instanceof Boolean))throw new Exception("Estado inválido.");JSONObject c=CloudBackend.control(d,"do_not_disturb");if(c==null)throw new Exception("Não perturbe indisponível.");changes.put(c.getString("id"),input.getBoolean("doNotDisturb"));}
        if(input.has("mode")){int mode=input.getInt("mode");if(mode<0||mode>2||input.getDouble("mode")!=mode)throw new Exception("Modo inicial inválido.");JSONObject c=CloudBackend.control(d,"power_memory");if(c==null)throw new Exception("Estado inicial indisponível.");byte[] bytes=decodeMemory(d.getJSONObject("dps").getString(c.getString("id")));bytes[1]=(byte)mode;
            if(mode==2){JSONObject custom=input.getJSONObject("custom");boolean white=custom.getString("kind").equals("white");if(!white&&!custom.getString("kind").equals("colour"))throw new Exception("Tipo inválido.");ByteBuffer b=ByteBuffer.wrap(bytes);b.position(2);b.putShort((short)(white?0:integer(custom,"h",0,360))).putShort((short)(white?0:percent(custom,"s",0))).putShort((short)(white?0:percent(custom,"v",1))).putShort((short)(white?percent(custom,"bright",1):0)).putShort((short)(white?percent(custom,"temp",0):0));}
            changes.put(c.getString("id"),Base64.getEncoder().encodeToString(bytes));}
        return cloud.publish(id,changes);
    }
    private static int integer(JSONObject o,String k,int min,int max) throws Exception {double n=o.getDouble(k);if(!Double.isFinite(n)||n!=Math.rint(n)||n<min||n>max)throw new Exception("Parâmetro fora da faixa: "+k);return (int)n;}
    private static int percent(JSONObject o,String key,int min) throws Exception {double n=o.getDouble(key);if(!Double.isFinite(n)||n<min||n>100)throw new Exception("Percentual inválido.");return (int)Math.round(n*10);}
    static JSONObject metric(JSONObject d,String regex,String fallbackUnit) throws Exception {
        JSONObject c=CloudBackend.control(d,regex);if(c==null)return new JSONObject().put("available",false);
        Object raw=d.getJSONObject("dps").opt(c.getString("id"));int scale=c.optInt("scale",0);if(!(raw instanceof Number)||scale<0||scale>6||!Double.isFinite(((Number)raw).doubleValue()))return new JSONObject().put("available",false);
        String unit=c.optString("unit",fallbackUnit);if(unit.isEmpty())unit=fallbackUnit;
        if(unit.equals("度")||unit.equals("kwh"))unit="kWh";
        return new JSONObject().put("available",true).put("value",((Number)raw).doubleValue()/Math.pow(10,scale)).put("unit",unit);
    }
    private static Object socketState(JSONObject d) throws Exception {JSONObject c=CloudBackend.control(d,"switch_1|switch");Object value=c==null?null:d.getJSONObject("dps").opt(c.getString("id"));return value instanceof Boolean?value:JSONObject.NULL;}
    JSONObject socketPower(String id,boolean power) throws Exception {
        requireSocket(cloud,id);JSONObject d=cloud.member(id);if(Boolean.FALSE.equals(d.opt("online")))throw new Exception("A tomada está offline.");
        JSONObject c=CloudBackend.control(d,"switch_1|switch");if(c==null)throw new Exception("A tomada não informou um controle de energia.");
        cloud.publish(id,new JSONObject().put(c.getString("id"),power));return new JSONObject().put("accepted",true).put("power",power);
    }
    JSONObject energy(String id) throws Exception {
        JSONObject d=cloud.member(id);if(CloudBackend.isLamp(d)||d.optBoolean("shared"))throw new Exception("Selecione uma tomada da sua conta.");cloud.readDevice(id);
        JSONObject total=metric(d,"total_energy|total_forward_energy|total_ele","kWh"),today=new JSONObject().put("available",false);String warning="";
        try{JSONObject data=summary(d,false);if(data!=null){JSONObject cloudTotal=kwh(data.opt("sum"));if(cloudTotal.optBoolean("available"))total=cloudTotal;today=kwh(data.opt("thisDay"));}}catch(Exception e){warning="Total da última leitura · nova consulta em até 1 minuto.";energyWarnings.put(id,warning);JSONObject cached=energySummaries.get(id);if(cached!=null){total=kwh(cached.opt("sum"));today=kwh(cached.opt("thisDay"));}}
        warning=energyWarnings.containsKey(id)?energyWarnings.get(id):"";
        return new JSONObject().put("current",metric(d,"cur_current","mA")).put("voltage",metric(d,"cur_voltage","V")).put("power",metric(d,"cur_power","W")).put("reported",metric(d,"add_ele","kWh")).put("total",total).put("socketPower",socketState(d)).put("today",today).put("consumptionWarning",warning).put("offline",Boolean.FALSE.equals(d.opt("online")));
    }
    JSONObject history(String id,String month) throws Exception {
        JSONObject d=cloud.member(id);if(CloudBackend.isLamp(d)||d.optBoolean("shared"))throw new Exception("Selecione sua tomada.");if(!month.matches("20\\d\\d-(0[1-9]|1[0-2])"))throw new Exception("Mês inválido.");YearMonth ym=YearMonth.parse(month);JSONObject c=CloudBackend.control(d,"add_ele|total_energy|total_forward_energy|total_ele");if(c==null)throw new Exception("A tomada não informou um contador de energia.");
        String compact=month.replace("-","");
        JSONObject params=new JSONObject().put("devId",id).put("dpId",Integer.parseInt(c.getString("id"))).put("type","sum").put("startDay",compact+"01").put("endDay",compact+String.format(java.util.Locale.ROOT,"%02d",ym.lengthOfMonth()));
        JSONObject raw=CloudBackend.object(cloud.api.call("tuya.m.dp.stat.days.list",params,cloud.sid,d.optString("gid")));
        JSONArray days=raw.optJSONArray("days"),values=raw.optJSONArray("values"),records=new JSONArray();
        if(days==null||values==null||days.length()!=values.length())throw new Exception("A nuvem não retornou os registros diários em um formato compatível.");
        for(int i=0;i<days.length();i++){
            String date=days.getString(i);if(!date.matches(month.substring(5)+"-\\d{2}"))throw new Exception("Registro fora do mês solicitado.");
            int day=Integer.parseInt(date.substring(3));if(day<1||day>ym.lengthOfMonth())throw new Exception("Data inválida no histórico.");
            JSONObject value=kwh(values.opt(i));if(!value.optBoolean("available"))throw new Exception("Valor inválido no histórico.");
            records.put(new JSONObject().put("day",date.substring(3)).put("value",value.getDouble("value")));
        }
        JSONObject total=kwh(raw.opt("total"));String totalSource="daily";JSONArray availableMonths=new JSONArray();
        try{JSONObject all=summary(d,true),years=all==null?null:all.optJSONObject("years"),year=years==null?null:years.optJSONObject(month.substring(0,4));JSONObject monthly=kwh(year==null?null:year.opt(month.substring(5)));if(monthly.optBoolean("available")){total=monthly;totalSource="monthly";}if(years!=null){java.util.TreeSet<String> options=new java.util.TreeSet<>(java.util.Collections.reverseOrder());for(java.util.Iterator<String> yi=years.keys();yi.hasNext();){String y=yi.next();JSONObject months=years.optJSONObject(y);if(!y.matches("20\\d{2}")||months==null)continue;for(java.util.Iterator<String> mi=months.keys();mi.hasNext();){String m=mi.next();if(m.matches("0[1-9]|1[0-2]"))options.add(y+"-"+m);}}for(String option:options)availableMonths.put(option);}}catch(Exception ignored){}
        return new JSONObject().put("month",month).put("records",records).put("total",total.optBoolean("available")?total.get("value"):JSONObject.NULL).put("totalSource",totalSource).put("availableMonths",availableMonths).put("dailyTotal",raw.opt("total")).put("scale",0).put("unit","kWh");
    }
}
