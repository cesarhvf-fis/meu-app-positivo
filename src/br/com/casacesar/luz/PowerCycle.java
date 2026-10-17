package br.com.casacesar.luz;
import android.app.job.JobInfo;import android.app.job.JobScheduler;import android.content.ComponentName;import android.content.Context;import android.content.SharedPreferences;import android.os.Handler;import android.os.Looper;import org.json.JSONObject;

/** A single explicit power cycle, persisted before cutting power. Never tests real hardware automatically. */
final class PowerCycle {
    static final int JOB=32004;private static final Handler main=new Handler(Looper.getMainLooper());
    static SharedPreferences storage(Context c){return c.getSharedPreferences("power_cycle",Context.MODE_PRIVATE);}
    static JSONObject status(Context c) throws Exception {SharedPreferences p=storage(c);return new JSONObject().put("pending",p.contains("id")).put("deviceId",p.getString("id","")).put("restoreAt",p.getLong("at",0));}
    static void requireSocket(CloudBackend cloud,String id) throws Exception {DeviceExtras.requireSocket(cloud,id);}
    private static void power(CasaApp app,String id,boolean value) throws Exception {requireSocket(app.cloud,id);JSONObject control=CloudBackend.control(app.cloud.member(id),"switch_1|switch");if(control==null)throw new Exception("A tomada não oferece esse comando.");app.cloud.publish(id,new JSONObject().put(control.getString("id"),value));app.updateSnapshot();}
    static JSONObject start(CasaApp app,String id) throws Exception {
        requireSocket(app.cloud,id);if(storage(app).contains("id"))throw new Exception("Já há um religamento programado. Aguarde a conclusão.");
        if(Boolean.FALSE.equals(app.cloud.member(id).opt("online")))throw new Exception("A tomada está offline.");
        CycleFlow.start(new CycleFlow.Recovery(){public void prepare(long at)throws Exception{if(!storage(app).edit().putString("id",id).putLong("at",at).commit())throw new Exception("Não foi possível salvar o religamento. O plano anterior, se houver, foi mantido.");}public boolean schedule(){return PowerCycle.schedule(app);}public void arm(){PowerCycle.arm(app);}public void clear(){storage(app).edit().clear().commit();}},()->power(app,id,false),System::currentTimeMillis);
        return status(app);
    }
    static boolean schedule(Context c){long delay=Math.max(0,storage(c).getLong("at",0)-System.currentTimeMillis());JobInfo info=new JobInfo.Builder(JOB,new ComponentName(c,PowerCycleJob.class)).setMinimumLatency(delay).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true).setBackoffCriteria(10000,JobInfo.BACKOFF_POLICY_LINEAR).build();return ((JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).schedule(info)==JobScheduler.RESULT_SUCCESS;}
    static void arm(CasaApp app){if(!storage(app).contains("id"))return;long delay=Math.max(0,storage(app).getLong("at",0)-System.currentTimeMillis());main.postDelayed(()->app.worker.execute(()->{try{restore(app);}catch(Exception ignored){schedule(app);}}),delay);}
    static boolean restore(CasaApp app) throws Exception {
        SharedPreferences p=storage(app);String id=p.getString("id","");if(id.isEmpty())return true;
        if(p.getLong("at",0)>System.currentTimeMillis()){arm(app);return false;}
        if(CloudBackend.find(app.cloud.state().getJSONArray("devices"),id)==null)app.cloud.refresh();
        app.cloud.readDevice(id);
        power(app,id,true);p.edit().clear().commit();((JobScheduler)app.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancel(JOB);return true;
    }
}
