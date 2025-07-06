package com.smov.gabriel.orientatree.persistence.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.smov.gabriel.orientatree.persistence.entities.OfflineBeaconReach;
import java.util.List;

@Dao
public interface BeaconReachDao {
    @Insert
    void insert(OfflineBeaconReach reach);

    @Query("SELECT * FROM offline_beacon_reaches WHERE activityId = :activityId AND userId = :userId")
    List<OfflineBeaconReach> getBeaconReachesForActivity(String activityId, String userId);

    @Query("DELETE FROM offline_beacon_reaches WHERE activityId = :activityId AND userId = :userId")
    void deleteBeaconReaches(String activityId, String userId);

    @Query("DELETE FROM offline_locations WHERE id = :id_offline")
    void deleteBeaconReachId(int id_offline);
}
