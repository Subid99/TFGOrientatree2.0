package com.smov.gabriel.orientatree.persistence;


import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

import com.smov.gabriel.orientatree.persistence.daos.ActivityDao;
import com.smov.gabriel.orientatree.persistence.daos.BeaconDao;
import com.smov.gabriel.orientatree.persistence.daos.BeaconReachDao;
import com.smov.gabriel.orientatree.persistence.daos.LocationDao;
import com.smov.gabriel.orientatree.persistence.daos.MapDao;
import com.smov.gabriel.orientatree.persistence.daos.ParticipationDao;
import com.smov.gabriel.orientatree.persistence.daos.TemplateDao;
import com.smov.gabriel.orientatree.persistence.daos.UserDao;
import com.smov.gabriel.orientatree.persistence.entities.OfflineActivity;
import com.smov.gabriel.orientatree.persistence.entities.OfflineBeacon;
import com.smov.gabriel.orientatree.persistence.entities.OfflineBeaconReach;
import com.smov.gabriel.orientatree.persistence.entities.OfflineLocation;
import com.smov.gabriel.orientatree.persistence.entities.OfflineMap;
import com.smov.gabriel.orientatree.persistence.entities.OfflineParticipation;
import com.smov.gabriel.orientatree.persistence.entities.OfflineTemplate;
import com.smov.gabriel.orientatree.persistence.entities.OfflineUser;
import com.smov.gabriel.orientatree.persistence.Converters;

@Database(
        entities = {OfflineLocation.class, OfflineBeaconReach.class, OfflineActivity.class, OfflineBeacon.class, OfflineParticipation.class, OfflineTemplate.class, OfflineUser.class, OfflineMap.class},
        version = 4,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LocationDao locationDao();
    public abstract BeaconReachDao beaconReachDao();
    public abstract ActivityDao activityDao();
    public abstract BeaconDao beaconDao();
    public abstract ParticipationDao participationDao();
    public abstract TemplateDao templateDao();
    public abstract UserDao userDao();
    public abstract MapDao mapDao();


    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "orientatree_offline.db")
                            .fallbackToDestructiveMigration() // Solo para desarrollo
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}