package com.smov.gabriel.orientatree.persistence.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.firebase.firestore.GeoPoint;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.persistence.Converters;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity(tableName = "participations")
@TypeConverters(Converters.class)
public class OfflineParticipation {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "participant")
    public String participant;

    @TypeConverters(Converters.class)
    public ParticipationState state;

    @ColumnInfo(name = "start_time")
    @TypeConverters(Converters.class)
    public Date startTime;

    @ColumnInfo(name = "finish_time")
    @TypeConverters(Converters.class)
    public Date finishTime;

    public boolean completed;

    @TypeConverters(Converters.class)
    public List<BeaconReached> reaches;

    @ColumnInfo(name = "activity_id")
    public String activityId;

    @ColumnInfo(name = "last_location")
    @TypeConverters(Converters.class)
    public GeoPoint lastLocation;
}
