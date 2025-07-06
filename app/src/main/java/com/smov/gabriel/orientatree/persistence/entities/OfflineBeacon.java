package com.smov.gabriel.orientatree.persistence.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.google.firebase.firestore.GeoPoint;
import com.smov.gabriel.orientatree.persistence.Converters;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "beacons")
public class OfflineBeacon {
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "beacon_id")
    public String beaconId;

    @ColumnInfo(name = "template_id")
    public String templateId;

    @ColumnInfo(name = "location")
    @TypeConverters(Converters.class)
    public GeoPoint location;

    public int number;
    public String name;
    public String text;
    public String question;

    @ColumnInfo(name = "written_right_answer")
    public String writtenRightAnswer;

    @ColumnInfo(name = "possible_answers")
    @TypeConverters(Converters.class)
    public List<String> possibleAnswers;

    @ColumnInfo(name = "quiz_right_answer")
    public int quizRightAnswer;
}
