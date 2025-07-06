package com.smov.gabriel.orientatree.persistence.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.room.Entity;
import com.smov.gabriel.orientatree.model.TemplateColor;
import com.smov.gabriel.orientatree.model.TemplateType;
import com.smov.gabriel.orientatree.persistence.Converters;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "templates")
@TypeConverters(Converters.class)
public class OfflineTemplate {
    @PrimaryKey
    @NonNull
    public String templateId;

    public String name;

    @ColumnInfo(name = "type")
    public TemplateType type;

    @ColumnInfo(name = "color")
    public TemplateColor color;

    public String location;
    public String description;
    public String norms;

    @ColumnInfo(name = "map_id")
    public String mapId;

    @ColumnInfo(name = "start_lat")
    public double startLat;

    @ColumnInfo(name = "start_lng")
    public double startLng;

    @ColumnInfo(name = "end_lat")
    public double endLat;

    @ColumnInfo(name = "end_lng")
    public double endLng;

    public String password;

    @ColumnInfo(name = "beacons")
    @TypeConverters(Converters.class)
    public List<String> beacons;
}
