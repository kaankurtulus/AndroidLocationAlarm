package com.kaank.androidlocationalarm.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kaank.androidlocationalarm.R;
import com.kaank.androidlocationalarm.Reminder;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by k_kur on 11.12.2018.
 */

public class Utils {
    private static final String REMINDERS = "REMINDERS";
    private static final String PREFS_NAME = "ReminderRepository";


    private static SharedPreferences preferences;
    public static void showReminderInMap(Context context, GoogleMap map, Reminder reminder){
        if (reminder.latLng != null) {
            LatLng latLng = reminder.latLng;

            Marker marker = map.addMarker(new MarkerOptions().position(latLng));
            marker.setIcon(bitmapDescriptorFromVector(context, R.drawable.ic_twotone_location_on_48px));
            marker.setTag(reminder.id);
            if (reminder.radius != null) {
                Double radius = reminder.radius;
                map.addCircle(new CircleOptions()
                        .center(reminder.latLng)
                        .radius(radius)
                        .strokeColor(ContextCompat.getColor(context, R.color.colorAccent))
                        .fillColor(ContextCompat.getColor(context, R.color.colorReminderFill)));
            }
        }
    }

    private static BitmapDescriptor bitmapDescriptorFromVector(Context context, @DrawableRes int vectorDrawableResourceId) {
        Drawable background = ContextCompat.getDrawable(context, R.drawable.ic_twotone_location_on_48px);
        background.setBounds(0, 0, background.getIntrinsicWidth(), background.getIntrinsicHeight());
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorDrawableResourceId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth() , vectorDrawable.getIntrinsicHeight() );
        Bitmap bitmap = Bitmap.createBitmap(background.getIntrinsicWidth(), background.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        background.draw(canvas);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    public static void saveAll(Context context, List<Reminder> reminders) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        preferences
                .edit()
                .putString(REMINDERS,new Gson().toJson(reminders))
                .apply();
    }

    public static List<Reminder> getAll(Context context){
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if(preferences.contains(REMINDERS)){

            String reminderString = preferences.getString(REMINDERS,null);
            Type type = new TypeToken<ArrayList<Reminder>>(){}.getType();
            ArrayList<Reminder> reminders = new Gson().fromJson(reminderString,type);
            if(reminders!=null){
                return reminders;
            }


        }
        return new ArrayList<Reminder>();
    }

    public static Reminder getLast(Context context){
        List<Reminder> reminders = getAll(context);

        return reminders.get(reminders.size()-1);
    }

    public static Reminder get(Context context,String id){
        Iterator<Reminder> iterator = getAll(context).iterator();
        while (iterator.hasNext()){
            Reminder r = iterator.next();
            if(r.id.equals(id)){
                return r;
            }
        }
        return null;
    }






}

