package com.smov.gabriel.orientatree.persistence.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.smov.gabriel.orientatree.persistence.entities.OfflineBeacon;

import java.util.List;

// BeaconDao.java
@Dao
public interface BeaconDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBeacon(OfflineBeacon beacon);

    @Update
    void updateBeacon(OfflineBeacon beacon);

    @Delete
    void deleteBeacon(OfflineBeacon beacon);

    @Query("SELECT * FROM beacons WHERE beacon_id = :beaconId")
    OfflineBeacon getBeaconById(String beaconId);

    @Query("SELECT * FROM beacons WHERE template_id = :templateId")
    List<OfflineBeacon> getBeaconsByTemplate(String templateId);

    @Query("SELECT * FROM beacons WHERE template_id = :templateId ORDER BY number ASC")
    List<OfflineBeacon> getBeaconsByTemplateSorted(String templateId);

    @Query("DELETE FROM beacons WHERE beacon_id = :beaconId")
    void deleteBeaconById(String beaconId);

    @Query("DELETE FROM beacons WHERE template_id = :templateId")
    void deleteBeaconsByTemplate(String templateId);

    @Query("SELECT COUNT(*) FROM beacons WHERE template_id = :templateId")
    int getBeaconCountForTemplate(String templateId);
}
