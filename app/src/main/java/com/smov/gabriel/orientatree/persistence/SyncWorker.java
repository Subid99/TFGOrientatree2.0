package com.smov.gabriel.orientatree.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.persistence.entities.OfflineBeaconReach;
import com.smov.gabriel.orientatree.persistence.entities.OfflineLocation;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SyncWorker extends Worker {
    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Obtener datos de entrada
        String activityId = getInputData().getString("activity_id");
        String userId = getInputData().getString("user_id");

        // Sincronizar datos
        try {
            AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();

            // Sincronizar ubicaciones
            syncLocations(db, firestore, activityId, userId);

            // Sincronizar balizas alcanzadas
            syncBeaconReaches(db, firestore, activityId, userId);

            // Sincronizar estado de finalización
            syncCompletionState(activityId, userId);

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error sincronizando datos", e);
            return Result.retry();
        }
    }

    private void syncLocations(AppDatabase db, FirebaseFirestore firestore,
                               String activityId, String userId) {
        List<OfflineLocation> locations =
                db.locationDao().getLocationsForActivity(activityId, userId);

        for (OfflineLocation loc : locations) {
            // Crear objeto de ubicación para Firestore
            com.smov.gabriel.orientatree.model.Location location =
                    new com.smov.gabriel.orientatree.model.Location();
            location.setTime(new Date(loc.timestamp));
            location.setLocation(new GeoPoint(loc.latitude, loc.longitude));

            // Subir a Firestore
            firestore.collection("activities").document(activityId)
                    .collection("participations").document(userId)
                    .collection("locations").document(UUID.randomUUID().toString())
                    .set(location)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            db.locationDao().deleteLocationId(loc.id);
                        }
                    });

            // Actualizar última ubicación
            firestore.collection("activities").document(activityId)
                    .collection("participations").document(userId)
                    .update("lastLocation", new GeoPoint(loc.latitude, loc.longitude));
        }
    }

    private void syncBeaconReaches(AppDatabase db, FirebaseFirestore firestore,
                                   String activityId, String userId) {
        List<OfflineBeaconReach> reaches =
                db.beaconReachDao().getBeaconReachesForActivity(activityId, userId);

        for (OfflineBeaconReach reach : reaches) {
            BeaconReached beaconReached = new BeaconReached(
                    new Date(reach.reachTime),
                    reach.beaconId,
                    reach.answered
            );

            firestore.collection("activities").document(activityId)
                    .collection("participations").document(userId)
                    .collection("beaconReaches").document(reach.beaconId)
                    .set(beaconReached)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            db.beaconReachDao().deleteBeaconReachId(reach.id);
                        }
                    });
        }
    }

    private void syncCompletionState(String activityId, String userId) {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences("OfflineState", Context.MODE_PRIVATE);

        if (prefs.contains("activityId") && prefs.getString("activityId", "").equals(activityId)) {
            long finishTime = prefs.getLong("finishTime", 0);
            boolean completed = prefs.getBoolean("completed", false);

            FirebaseFirestore.getInstance()
                    .collection("activities").document(activityId)
                    .collection("participations").document(userId)
                    .update(
                            "state", ParticipationState.FINISHED,
                            "finishTime", new Date(finishTime),
                            "completed", completed
                    )
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            prefs.edit().clear().apply();
                        }
                    });
        }
    }
}