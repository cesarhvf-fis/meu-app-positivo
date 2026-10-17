package br.com.casacesar.luz;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.atomic.AtomicInteger;

/** Network polling is scheduled, avoiding long work in the widget's BroadcastReceiver. */
public final class WidgetRefreshJob extends JobService {
    private static final int JOB = 32003;
    private final AtomicInteger generation = new AtomicInteger();
    static void schedule(Context context) {
        try {
            JobScheduler jobs = context.getSystemService(JobScheduler.class);
            jobs.schedule(new JobInfo.Builder(JOB, new ComponentName(context, WidgetRefreshJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).build());
        } catch (RuntimeException unsupportedScheduler) { }
    }
    @Override public boolean onStartJob(JobParameters parameters) {
        final int token = generation.incrementAndGet(); CasaApp app = (CasaApp) getApplication();
        app.worker.execute(() -> {
            try { app.readPreferred(); } catch (Exception e) { app.sessionFailure(e); }
            finally {
                LampWidgetProvider.renderAll(this);
                new Handler(Looper.getMainLooper()).post(() -> { if (generation.get() == token) jobFinished(parameters, false); });
            }
        });
        return true;
    }
    @Override public boolean onStopJob(JobParameters parameters) { generation.incrementAndGet(); return false; }
}
