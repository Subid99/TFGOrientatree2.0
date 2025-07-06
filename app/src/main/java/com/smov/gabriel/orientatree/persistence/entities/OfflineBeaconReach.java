package com.smov.gabriel.orientatree.persistence.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "offline_beacon_reaches")
public class OfflineBeaconReach {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String activityId;
    public String userId;
    public String beaconId;
    public long reachTime;
    public int quiz_answer;
    public String written_answer;
    public boolean answer_right;
    public boolean answered;
}