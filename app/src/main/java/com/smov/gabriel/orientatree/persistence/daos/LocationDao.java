package com.smov.gabriel.orientatree.persistence.daos;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.smov.gabriel.orientatree.persistence.entities.OfflineLocation;
import java.util.List;

@Dao
public interface LocationDao {
    @Insert
    void insert(OfflineLocation location);

    @Query("SELECT * FROM offline_locations WHERE activityId = :activityId AND userId = :userId")
    List<OfflineLocation> getLocationsForActivity(String activityId, String userId);

    @Query("DELETE FROM offline_locations WHERE activityId = :activityId AND userId = :userId")
    void deleteLocations(String activityId, String userId);

    @Query("DELETE FROM offline_locations WHERE id = :id_offline")
    void deleteLocationId(int id_offline);
}
