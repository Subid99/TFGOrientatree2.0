package com.smov.gabriel.orientatree.persistence.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.google.firebase.firestore.GeoPoint;
import com.smov.gabriel.orientatree.persistence.entities.OfflineMap;

import java.util.List;

@Dao
public interface MapDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMap(OfflineMap map);

    @Update
    void updateMap(OfflineMap map);

    @Delete
    void deleteMap(OfflineMap map);

    @Query("SELECT * FROM maps WHERE map_id = :mapId")
    OfflineMap getMapById(String mapId);

    @Query("SELECT * FROM maps")
    List<OfflineMap> getAllMaps();

    @Query("DELETE FROM maps WHERE map_id = :mapId")
    void deleteMapById(String mapId);

    @Query("SELECT COUNT(*) FROM maps WHERE map_id = :mapId")
    int mapExists(String mapId);

}