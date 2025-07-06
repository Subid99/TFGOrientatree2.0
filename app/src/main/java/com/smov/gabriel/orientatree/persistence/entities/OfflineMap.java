package com.smov.gabriel.orientatree.persistence.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.firebase.firestore.GeoPoint;
import com.smov.gabriel.orientatree.persistence.Converters;

import java.util.ArrayList;

@Entity(tableName = "maps")
public class OfflineMap {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "map_id")
    public String mapId;

    @ColumnInfo(name = "map_corners")
    @TypeConverters(Converters.class)
    public ArrayList<GeoPoint> mapCorners;

    @ColumnInfo(name = "overlay_corners")
    @TypeConverters(Converters.class)
    public ArrayList<GeoPoint> overlayCorners;

    @ColumnInfo(name = "centering_point")
    @TypeConverters(Converters.class)
    public GeoPoint centeringPoint;

    @ColumnInfo(name = "max_zoom")
    public float maxZoom;

    @ColumnInfo(name = "min_zoom")
    public float minZoom;

    @ColumnInfo(name = "initial_zoom")
    public float initialZoom;

}
