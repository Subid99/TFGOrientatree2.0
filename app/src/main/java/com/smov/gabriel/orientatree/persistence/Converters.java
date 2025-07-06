package com.smov.gabriel.orientatree.persistence;

import androidx.room.TypeConverter;

import com.google.common.reflect.TypeToken;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Beacon;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.Map;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.TemplateColor;
import com.smov.gabriel.orientatree.model.TemplateType;
import com.smov.gabriel.orientatree.persistence.entities.OfflineActivity;
import com.smov.gabriel.orientatree.persistence.entities.OfflineBeacon;
import com.smov.gabriel.orientatree.persistence.entities.OfflineMap;
import com.smov.gabriel.orientatree.persistence.entities.OfflineTemplate;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Converters {
    @TypeConverter
    public static List<String> fromString(String value) {
        return new Gson().fromJson(value, new TypeToken<List<String>>(){}.getType());
    }

    @TypeConverter
    public static String fromList(List<String> list) {
        return new Gson().toJson(list);
    }
    @TypeConverter
    public static TemplateType toTemplateType(String value) {
        return value == null ? null : TemplateType.fromString(value);
    }

    @TypeConverter
    public static String fromTemplateType(TemplateType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static TemplateColor toTemplateColor(String value) {
        return value == null ? null : TemplateColor.fromString(value);
    }

    @TypeConverter
    public static String fromTemplateColor(TemplateColor color) {
        return color == null ? null : color.name();
    }

    @TypeConverter
    public static String fromArrayList(ArrayList<String> list) {
        return new Gson().toJson(list);
    }
    // Converter para GeoPoint
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
    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static List<BeaconReached> stringToOfflineBeaconReachList(String data) {
        if (data == null) {
            return Collections.emptyList();
        }
        Type listType = new TypeToken<List<BeaconReached>>() {}.getType();
        return new Gson().fromJson(data, listType);
    }

    @TypeConverter
    public static String offlineBeaconReachListToString(List<BeaconReached> offlineBeaconReaches) {
        return new Gson().toJson(offlineBeaconReaches);
    }
    @TypeConverter
    public static String fromBeaconReachedList(ArrayList<BeaconReached> beaconReachedList) {
        if (beaconReachedList == null) {
            return null;
        }
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<BeaconReached>>() {}.getType();
        return gson.toJson(beaconReachedList, type);
    }
    // Converter para ArrayList<GeoPoint>
    @TypeConverter
    public static ArrayList<GeoPoint> toGeoPointList(String value) {
        if (value == null || value.isEmpty()) return new ArrayList<>();
        Type listType = new TypeToken<ArrayList<GeoPoint>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromGeoPointList(ArrayList<GeoPoint> list) {
        return new Gson().toJson(list);
    }

    @TypeConverter
    public static ArrayList<BeaconReached> toBeaconReachedList(String beaconReachedListString) {
        if (beaconReachedListString == null) {
            return null;
        }
        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<BeaconReached>>() {}.getType();
        return gson.fromJson(beaconReachedListString, type);
    }
    public static OfflineBeacon toEntity(Beacon beacon) {
        OfflineBeacon offlineBeacon = new OfflineBeacon();
        offlineBeacon.beaconId =beacon.getBeacon_id();
        offlineBeacon.templateId=beacon.getTemplate_id();
        offlineBeacon.location=beacon.getLocation();
        offlineBeacon.number=beacon.getNumber();
        offlineBeacon.name=beacon.getName();
        offlineBeacon.text=beacon.getText();
        offlineBeacon.question=beacon.getQuestion();
        offlineBeacon.writtenRightAnswer=beacon.getWritten_right_answer();
        offlineBeacon.possibleAnswers=beacon.getPossible_answers() != null ? beacon.getPossible_answers() : new ArrayList<>();
        offlineBeacon.quizRightAnswer=beacon.getQuiz_right_answer();
        return offlineBeacon;
    }

    public static OfflineTemplate toEntity(Template template) {
        OfflineTemplate offlineTemplate = new OfflineTemplate();
        offlineTemplate.templateId = template.getTemplate_id();
        offlineTemplate.name = template.getName();
        offlineTemplate.type = template.getType();
        offlineTemplate.color = template.getColor();
        offlineTemplate.location = template.getLocation();
        offlineTemplate.description = template.getDescription();
        offlineTemplate.norms = template.getNorms();
        offlineTemplate.mapId = template.getMap_id();
        offlineTemplate.startLat = template.getStart_lat();
        offlineTemplate.startLng = template.getStart_lng();
        offlineTemplate.endLat = template.getEnd_lat();
        offlineTemplate.endLng = template.getEnd_lng();
        offlineTemplate.password = template.getPassword();
        offlineTemplate.beacons = template.getBeacons() != null ? template.getBeacons() : new ArrayList<>();
        return offlineTemplate;
    }

    public static OfflineMap toEntity(Map map) {
        OfflineMap offlineMap = new OfflineMap();
        offlineMap.mapId = map.getMap_id();
        offlineMap.overlayCorners = map.getOverlay_corners();
        offlineMap.centeringPoint = map.getCentering_point();
        offlineMap.maxZoom = map.getMax_zoom();
        offlineMap.minZoom = map.getMin_zoom();
        offlineMap.initialZoom = map.getInitial_zoom();
        return offlineMap;
    }
    public Activity convertToActivity(OfflineActivity entity) {
        Activity activity = new Activity();
        activity.setId(entity.id);
        activity.setTitle(entity.title);
        activity.setPlanner_id(entity.plannerId);
        activity.setTemplate(entity.template);
        activity.setKey(entity.key);
        activity.setStartTime(new Date(entity.startTime));
        activity.setFinishTime(new Date(entity.finishTime));
        activity.setParticipants(new ArrayList<>(entity.participants));
        return activity;
    }
    public Template convertToTemplate(OfflineTemplate entity){
        Template template = new Template();
        template.setTemplate_id(entity.templateId);
        template.setName(entity.name);
        template.setType(entity.type);
        template.setColor(entity.color);
        template.setLocation(entity.location);
        template.setDescription(entity.description);
        template.setNorms(entity.norms);
        template.setMap_id(entity.mapId);
        template.setStart_lat(entity.startLat);
        template.setStart_lng(entity.startLng);
        template.setEnd_lat(entity.endLat);
        template.setEnd_lng(entity.endLng);
        template.setBeacons(new ArrayList<>(entity.beacons));
        template.setPassword(entity.password);
        return template;
    }
}
