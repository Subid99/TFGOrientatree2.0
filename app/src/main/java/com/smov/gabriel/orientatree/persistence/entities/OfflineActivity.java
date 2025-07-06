package com.smov.gabriel.orientatree.persistence.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import com.smov.gabriel.orientatree.persistence.Converters;
import com.google.firebase.firestore.GeoPoint;

import java.util.List;
@Entity(tableName = "offline_activities")
@TypeConverters(Converters.class)
public class OfflineActivity {
    @PrimaryKey
    @NonNull
    public String id;

    public String visible_id;
    public String key;

    public String title;

    public String template;
    public String plannerId;
    public boolean score;
    public boolean location_help;
    public GeoPoint goalLocation;
    public long startTime;
    public long finishTime;
    public String imageUrl;

    @TypeConverters
    public List<String> participants;
    @TypeConverter
    public static GeoPoint toGeoPoint(String value) {
        if (value == null || value.isEmpty()) return null;
        String[] parts = value.split(",");
        return new GeoPoint(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
    }
    @TypeConverter
    public static String fromGeoPoint(GeoPoint geoPoint) {
        return geoPoint == null ? null : geoPoint.getLatitude() + "," + geoPoint.getLongitude();
    }
}
