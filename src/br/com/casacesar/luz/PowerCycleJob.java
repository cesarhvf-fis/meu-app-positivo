package br.com.casacesar.luz;
import android.app.job.JobParameters;import android.app.job.JobService;import android.os.Handler;import android.os.Looper;
public final class PowerCycleJob extends JobService {
    @Override public boolean onStartJob(JobParameters params){CasaApp app=(CasaApp)getApplication();app.worker.execute(()->{boolean retry=false;try{retry=!PowerCycle.restore(app);}catch(Exception e){retry=true;}final boolean again=retry;new Handler(Looper.getMainLooper()).post(()->jobFinished(params,again));});return true;}
    @Override public boolean onStopJob(JobParameters params){return true;}
}
