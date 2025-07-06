package com.smov.gabriel.orientatree.persistence.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;
import androidx.room.Update;

import com.google.firebase.firestore.GeoPoint;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.persistence.Converters;
import com.smov.gabriel.orientatree.persistence.entities.OfflineParticipation;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Dao
@TypeConverters(Converters.class)
public interface ParticipationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertParticipation(OfflineParticipation participation);

    @Update
    void updateParticipation(OfflineParticipation participation);

    @Delete
    void deleteParticipation(OfflineParticipation participation);

    @Query("SELECT * FROM participations WHERE participant = :participantId")
    OfflineParticipation getParticipation(String participantId);

    @Query("SELECT * FROM participations WHERE participant = :userId AND activity_id = :activityId")
    OfflineParticipation getParticipation(String userId, String activityId);

    @Query("SELECT * FROM participations")
    List<OfflineParticipation> getAllParticipations();

    @Query("DELETE FROM participations WHERE participant = :participantId")
    void deleteParticipationById(String participantId);

    @Query("UPDATE participations SET state = :state WHERE participant = :participantId")
    void updateParticipationState(String participantId, ParticipationState state);
    @Query("UPDATE participations SET state = :state WHERE participant = :participantId and activity_id = :activityId")
    void updateParticipationState(String participantId,String activityId, ParticipationState state);

    @Query("UPDATE participations SET last_location = :location WHERE participant = :participantId")
    void updateLastLocation(String participantId, GeoPoint location);

    @Query("UPDATE participations SET reaches = :reaches WHERE participant = :participantId")
    void updateReaches(String participantId, ArrayList<BeaconReached> reaches);
    @Query("UPDATE participations SET start_time = :currentTime WHERE participant = :userID and activity_id = :activityid")
    void updateParticipationStartTime(String userID, String activityid, Date currentTime);
    @Query("UPDATE participations SET finish_time = :currentTime WHERE participant = :userID and activity_id = :activityid")
    void updateParticipationFinishTime(String userID, String activityid, Date currentTime);
}
