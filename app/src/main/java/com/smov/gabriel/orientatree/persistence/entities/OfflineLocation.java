package com.smov.gabriel.orientatree.persistence.entities;

// OfflineLocation.java

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "offline_locations")
public class OfflineLocation {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String activityId;
    public String userId;
    public double latitude;
    public double longitude;
    public long timestamp;
}
