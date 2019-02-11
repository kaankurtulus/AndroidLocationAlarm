package com.kaank.androidlocationalarm;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.PersistableBundle;
import android.util.Log;

/**
 * Created by k_kur on 3.12.2018.
 */

@TargetApi(21)
public class OreoJobService extends JobService {
    private JobParameters mParams;

    //Assuming it takes maximum 5 seconds for
    private String TAG = "OreoJobService";

    public boolean onStartJob(JobParameters params) {

        this.mParams = params;

        String command = params.getExtras().getString("command");

        if (command != null && command.equals("stop")) {

            this.endJob();
            return false;
        } else {

           /* if (MyApplication.getInstance().geoFencePendingIntent == null) {



                if (params.getExtras().containsKey("fromJobScheduler")) {




                    String geoFences = MyApplication.getInstance().fetchGeoFences();

                    if (geoFences != null) {


                        List<Fence> fencesList = new Gson().fromJson(geoFences, new TypeToken<List<Fence>>() {
                        }.getType());
                        MyApplication.getInstance().createGeoFences(fencesList);


                    }

                }
            } else {


                Log.e(TAG, "onStartJob: scheduling job ");
                this.scheduleJob(10000);
            }*/
            Log.e(TAG, "onStartJob: scheduling job ");
            this.scheduleJob(10000);
            return true;
        }
    }

    void endJob() {
        this.jobFinished(this.mParams, false);
    }

    void scheduleJob(long interval) {

        ComponentName serviceName = new ComponentName(this.getPackageName(), OreoJobService.class.getName());


        PersistableBundle extras = new PersistableBundle();
        extras.putString("command", "start");
        extras.putInt("fromJobScheduler", 1);
        JobInfo jobInfo = (new JobInfo.Builder(34, serviceName))
                .setExtras(extras).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(interval)
                .setOverrideDeadline(interval)
                .build();

        JobScheduler jobScheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        assert jobScheduler != null;
        jobScheduler.schedule(jobInfo);
        this.endJob();
    }

    public boolean onStopJob(JobParameters params) {

        return false;
    }
}
