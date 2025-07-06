package com.smov.gabriel.orientatree.persistence.daos;

import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Dao;

import com.smov.gabriel.orientatree.persistence.entities.OfflineActivity;

import java.util.List;
@Dao
public interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertActivity(OfflineActivity activity);

    @Query("SELECT * FROM offline_activities WHERE id = :activityId")
    OfflineActivity getActivityById(String activityId);

    @Query("SELECT * FROM offline_activities")
    List<OfflineActivity> getAllActivities();

    @Update
    void updateActivity(OfflineActivity activity);

    @Query("DELETE FROM offline_activities WHERE id = :activityId")
    void deleteActivity(String activityId);

    @Query("SELECT * FROM offline_activities WHERE " +
            "finishTime >= :currentTime AND " +
            "(plannerId = :userId OR " +
            "participants LIKE '%' || :userId || '%') AND " +
            "startTime <= :currentTime")
    List<OfflineActivity> getCurrentActivities(long currentTime, String userId);
}
