package com.kaank.androidlocationalarm;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.kaank.androidlocationalarm.models.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by k_kur on 10.12.2018.
 */

public class NewReminderActivity extends AppCompatActivity implements OnMapReadyCallback,GoogleApiClient.OnConnectionFailedListener,OnCompleteListener<Void> {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private final String EXTRA_LAT_LNG = "EXTRA_LAT_LNG";
    private final String EXTRA_ZOOM = "EXTRA_ZOOM";

    private GoogleMap gMap;
    private Reminder reminder = new Reminder(null, null,  null);
    private TextView instTitle,instSubtitle,radiusDesc;
    private SeekBar radius;
    private EditText message;
    private Button next;

    private GeofencingClient mGeofencingClient;
    private ArrayList<Geofence> mGeofenceList;
    private PendingIntent mGeofencePendingIntent;




    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_reminder);
        findViewById(R.id.container).setVisibility(View.GONE);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        instTitle=findViewById(R.id.instructionTitle);
        instSubtitle=findViewById(R.id.instructionSubtitle);
        radiusDesc=findViewById(R.id.radiusDescription);
        radius = findViewById(R.id.radiusBar);
        message=findViewById(R.id.message);
        next=findViewById(R.id.next);

        mGeofencePendingIntent = null;
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        mGeofenceList = new ArrayList<>();

    }

    /*@Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }*/

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        gMap=googleMap;
        gMap.getUiSettings().setMapToolbarEnabled(false);
        centerCamera();
        showConfigureLocation();

    }

    private void showConfigureLocation() {
        findViewById(R.id.container).setVisibility(View.VISIBLE);
        radius.setVisibility(View.GONE);
        radiusDesc.setVisibility(View.GONE);
        message.setVisibility(View.GONE);
        instTitle.setText(R.string.instruction_where_description);
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reminder.latLng = gMap.getCameraPosition().target;
                showConfigureRadiusMessage();
            }
        });

        radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateRadiusWithProgress(progress);
                showReminderUpdate();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void showConfigureRadiusMessage(){
        findViewById(R.id.marker).setVisibility(View.GONE);
        instSubtitle.setVisibility(View.GONE);
        radius.setVisibility(View.VISIBLE);
        radiusDesc.setVisibility(View.VISIBLE);
        message.setVisibility(View.VISIBLE);
        instTitle.setText(R.string.instruction_radius_description);


        updateRadiusWithProgress(radius.getProgress());
        gMap.animateCamera(CameraUpdateFactory.zoomTo(15f));
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard(v);

                reminder.message=message.getText().toString();
                Log.i("NewReminderr", "onClick: "+message.getText().toString());

                if(reminder.message.isEmpty()||reminder.message==null){
                    message.setError(getString(R.string.error_required));
                }else{
                    addReminder(reminder);
                }
            }
        });

        showReminderUpdate();

    }

    private void showReminderUpdate() {
        gMap.clear();
        Utils.showReminderInMap(this,gMap,reminder);
    }




    private void updateRadiusWithProgress(int progress) {
        double radius = getRadius(progress);
        reminder.radius = radius;
        radiusDesc.setText(getString(R.string.radius_description, String.valueOf(radius)));
    }
    private double getRadius(int progress) {
        return  100 + (2 * (double)progress + 1) * 100;
    }

    private void centerCamera() {
        LatLng latLng = (LatLng)getIntent().getExtras().get(EXTRA_LAT_LNG);
        Log.i("NewReminder", "centerCamera: "+latLng);
        float zoom = (float)getIntent().getExtras().get(EXTRA_ZOOM);
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
    }

    private void addReminder(Reminder reminder){
        Geofence geofence=buildGeofence(reminder);
        mGeofenceList.add(geofence);

        if(geofence!=null&& ContextCompat.checkSelfPermission(
               this,
               Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
           mGeofencingClient.addGeofences(buildGeofencingReq(geofence),getGeofencePendingIntent()).addOnCompleteListener(this);
       }
    }

    private GeofencingRequest buildGeofencingReq(Geofence geofence) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    private Geofence buildGeofence(Reminder reminder) {
        double lat =reminder.latLng.latitude;
        double longitude=reminder.latLng.longitude;
        double radius=reminder.radius;

        return new Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(
                        lat,
                        longitude,
                        (float)radius
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build();


    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        /*if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }*/
        Intent intent = new Intent(this, GeofenceTransitionsJobIntentService.class);
        //ContextCompat.startForegroundService(this, new Intent(this,GeofenceTransitionsJobIntentService.class));
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private void hideSoftKeyboard(View view){
        //hide keyboard

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    @Override
    public void onComplete(@NonNull Task<Void> task) {

        if (task.isSuccessful()) {
            List<Reminder> reminderArrayList = Utils.getAll(this);
            reminderArrayList.add(reminder);
            Utils.saveAll(this, reminderArrayList);
            Log.d(TAG, "onComplete: "+new Gson().toJson(mGeofenceList));
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel serviceChannel = new NotificationChannel(
                        "ServiceChannel",
                        "Example Service Channel",
                        NotificationManager.IMPORTANCE_DEFAULT
                );

                NotificationManager manager = getSystemService(NotificationManager.class);
                manager.createNotificationChannel(serviceChannel);
            }*/

            //Toast.makeText(this, "Geofence added", Toast.LENGTH_SHORT).show();
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                ComponentName serviceName = new ComponentName(this, OreoJobService.class.getName());
                PersistableBundle extras = new PersistableBundle();
                extras.putString("command", "start");

                JobInfo jobInfo = (new JobInfo.Builder(573, serviceName))
                        .setExtras(extras).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setMinimumLatency(1L)
                        .setOverrideDeadline(1L)
                        .build();

                JobScheduler jobScheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);

                try {
                    jobScheduler.schedule(jobInfo);
                } catch (IllegalArgumentException errorMessage) {
                    errorMessage.printStackTrace();
                }


            }*/
            setResult(Activity.RESULT_OK);
            finish();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this, task.getException());
            Log.w(TAG, errorMessage);
        }

    }


}
