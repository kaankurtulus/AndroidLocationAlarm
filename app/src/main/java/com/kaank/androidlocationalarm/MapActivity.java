package com.kaank.androidlocationalarm;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.pm.ActivityInfoCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.PlacesOptions;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.kaank.androidlocationalarm.models.PlaceInfo;
import com.kaank.androidlocationalarm.models.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.safety.locationlistenerhelper.core.LocationTracker;

/**
 * Created by k_kur on 27.10.2018.
 */

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback,GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnMarkerClickListener {

    private static final String TAG = "MapActivity";
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int NEW_REMINDER_REQUEST_CODE = 330;
    //whole world
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40,-168),new LatLng(71,136));

    //--
    private AutoCompleteTextView searchText;
    private FloatingActionButton btnLoc;
    private FloatingActionButton buttonSave;

    private boolean mPermissionsGranted = false;
    private GoogleMap gMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceAutocompleteAdapter autocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private GeofencingClient mGeofencingClient;
    // The entry points to the Places API.
    private GeoDataClient mGeoDataClient;
    private PlaceDetectionClient mPlaceDetectionClient;

    private PlaceInfo mPlace;
    private Marker marker;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        searchText = findViewById(R.id.etSearch);
        btnLoc = findViewById(R.id.currentLocation);
        buttonSave = findViewById(R.id.newReminder);




        getLocPermission();
    }
/******************************************/
    private void init(){
        Log.d(TAG, "init: initiliazing");


        searchText.setOnItemClickListener(autoCompleteClickListener);

        mGeoDataClient = Places.getGeoDataClient(this);
        mGeofencingClient = LocationServices.getGeofencingClient(this);
        autocompleteAdapter = new PlaceAutocompleteAdapter(this,mGeoDataClient,LAT_LNG_BOUNDS,null);


        searchText.setAdapter(autocompleteAdapter);

        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH||actionId == EditorInfo.IME_ACTION_DONE
                        ||keyEvent.getAction() == KeyEvent.ACTION_DOWN||keyEvent.getAction()==KeyEvent.KEYCODE_ENTER){

                    //call method for searching
                    geoLocate();

                }
                return false;
            }
        });

        btnLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: clicked gps icon");
                getDeviceLocation();
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setButtonOnClickListener(view);
            }
        });
        hideSoftKeyboard();
        showReminders();
    }

    private void showReminders(){
        gMap.clear();
        for (Reminder reminder : Utils.getAll(this)) {
            Utils.showReminderInMap(this,gMap,reminder);

        }
    }

    private void setButtonOnClickListener(View view){
        Intent intent = new Intent(this,NewReminderActivity.class);
        intent.putExtra("EXTRA_LAT_LNG",gMap.getCameraPosition().target);
        intent.putExtra("EXTRA_ZOOM",gMap.getCameraPosition().zoom);
        startActivityForResult(intent,NEW_REMINDER_REQUEST_CODE);


    }

    private void geoLocate(){
        hideSoftKeyboard();
        Log.d(TAG, "geoLocate: geolocating");
        String searchString = searchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = new ArrayList<>();
        try {

            list = geocoder.getFromLocationName(searchString,1);
        }catch (IOException e){
            Log.d(TAG, "geoLocate: IOException"+e.getMessage());
        }

        if(list.size()>0){
            Address address = list.get(0);
            Log.d(TAG, "geoLocate: address"+address.toString());

            moveCamera(new LatLng(address.getLatitude(),address.getLongitude()),DEFAULT_ZOOM,address.getAddressLine(0));
        }
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting device location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mPermissionsGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Log.d(TAG, "onComplete: found location!!");
                            Location currentLoc = (Location) task.getResult();

                            moveCamera(new LatLng(currentLoc.getLatitude(), currentLoc.getLongitude()), DEFAULT_ZOOM,"My Location");

                        } else {
                            Log.d(TAG, "onComplete: current location not found");
                            Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.d(TAG, "getDeviceLocation: SecurityException" + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom,String title) {
        Log.d(TAG, "MoveCamera: moving camera to" + latLng);
        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));

        //create and drop marker on map
        if(title!="My Location"){
            if(marker != null)
                marker.remove();
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);

            marker = gMap.addMarker(options);
        }

        hideSoftKeyboard();
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapActivity.this);
    }

    private void getLocPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mPermissionsGranted = true;
                initMap();

            } else {
                ActivityCompat.requestPermissions(this, permissions, 123);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, 123);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mPermissionsGranted = false;
                            return;
                        }
                    }
                    mPermissionsGranted = true;
                    //if permissions granted initialize map
                    initMap();

                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        gMap = googleMap;

        if (mPermissionsGranted) {
            getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            gMap.setMyLocationEnabled(true); //blue dot on map
            gMap.getUiSettings().setMyLocationButtonEnabled(false); //Google's find my location button
            gMap.getUiSettings().setMapToolbarEnabled(false); //Set default directions button false
            gMap.setOnMarkerClickListener(this);

            init();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClick: "+marker.getTitle());
        Reminder reminder = Utils.get(this,marker.getTag().toString());

        if (reminder != null) {
            showReminderRemoveAlert(reminder);
        }
        return false;
    }

    private void showReminderRemoveAlert(final Reminder reminder) {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle("Remove reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        //Utils.removeReminder(reminder);
                        removeReminder(reminder);
                        //Snackbar.make(this, R.string.reminder_removed_success, Snackbar.LENGTH_LONG).show()
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();


    }

    void removeReminder(Reminder reminder){
        ArrayList<String> mGeofenceList = new ArrayList<>();
        mGeofenceList.add(reminder.id);
        List<Reminder> reminders = Utils.getAll(this);
        for(int i = 0; i<reminders.size();i++){
            if(reminders.get(i).id.equals(reminder.id)){
                reminders.remove(i);
                //break;
            }
        }
        Utils.saveAll(getBaseContext(),reminders);
        showReminders();
        mGeofencingClient.removeGeofences(mGeofenceList);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void hideSoftKeyboard(){
        //hide keyboard
        Log.d(TAG, "hideSoftKeyboard: hide keyboard");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEW_REMINDER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            showReminders();

            Reminder reminder = Utils.getLast(this);
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(reminder.latLng,DEFAULT_ZOOM));


            View container = findViewById(android.R.id.content);
            if (container != null) {
                Snackbar.make(container, R.string.reminder_added_success, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    //-----------------------------------------------------

    private AdapterView.OnItemClickListener autoCompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();

            //Log.d(TAG, "onItemClick: getItem id:"+autocompleteAdapter.getItem(i).toString());
            final AutocompletePrediction item = autocompleteAdapter.getItem(i);

            final String placeID = item.getPlaceId();
            Task<PlaceBufferResponse> placeResult = mGeoDataClient.getPlaceById(placeID);
            placeResult.addOnCompleteListener(mUpdatePlaceDetailsCallback);

        }
    };

    private OnCompleteListener<PlaceBufferResponse> mUpdatePlaceDetailsCallback = new OnCompleteListener<PlaceBufferResponse>() {
        @Override
        public void onComplete(@NonNull Task<PlaceBufferResponse> task) {
            if(task.isSuccessful()){
                Log.d(TAG, "onComplete: Task success");
                PlaceBufferResponse places = task.getResult();
                final Place myPlace = places.get(0);
                try{
                   /* mPlace = new PlaceInfo(myPlace.getName().toString(),myPlace.getAddress().toString(),
                            myPlace.getPhoneNumber().toString(),myPlace.getId(),myPlace.getWebsiteUri(),
                            myPlace.getLatLng(),myPlace.getRating(),myPlace.getAttributions().toString());*/
                    mPlace = new PlaceInfo();
                    mPlace.setName(myPlace.getName().toString());
                    mPlace.setAddress(myPlace.getAddress().toString());
                    //mPlace.setAttributions(myPlace.getAttributions().toString());
                    mPlace.setId(myPlace.getId());
                    mPlace.setLatLng(myPlace.getLatLng());
                    mPlace.setRating(myPlace.getRating());
                    mPlace.setPhoneNumber(myPlace.getPhoneNumber().toString());
                    mPlace.setWebURL(myPlace.getWebsiteUri());
                    Log.d(TAG, "onComplete: place:"+mPlace.toString());
                }catch (NullPointerException e){
                    Log.e(TAG,e.getMessage());
                }
                moveCamera(mPlace.getLatLng(),DEFAULT_ZOOM,mPlace.getName());
//                Log.d(TAG,"place details: "+myPlace.getAttributions());
//                Log.d(TAG,"place details: "+myPlace.getViewport());
//                Log.d(TAG,"place details: "+myPlace.getPhoneNumber());
//                Log.d(TAG,"place details: "+myPlace.getWebsiteUri());
//                Log.d(TAG,"place details: "+myPlace.getId());
//                Log.d(TAG,"place details: "+myPlace.getAddress());
//                Log.d(TAG,"place details: "+myPlace.getLatLng());
                places.release();

            }else{
                Log.e(TAG,"Place not Found");
            }
        }
    };


}
