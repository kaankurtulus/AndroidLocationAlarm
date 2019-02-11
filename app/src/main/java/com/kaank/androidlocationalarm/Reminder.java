package com.kaank.androidlocationalarm;

import com.google.android.gms.maps.model.LatLng;

import java.util.UUID;

/**
 * Created by k_kur on 11.12.2018.
 */

public class Reminder {
    public String id;
    public LatLng latLng;
    public Double radius;
    public String message;

    public Reminder(LatLng latLng, Double radius, String message) {
        this.id = UUID.randomUUID().toString();
        this.latLng = latLng;
        this.radius = radius;
        this.message = message;
    }

}
