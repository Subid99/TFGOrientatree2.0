package com.smov.gabriel.orientatree.persistence.daos;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.model.ParticipationState;

import java.util.Date;

public class ParticipationUpdateWorker extends Worker {
    public ParticipationUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Obtener los datos de entrada
        String activityId = getInputData().getString("activityId");
        String userId = getInputData().getString("userId");
        long timeMillis = getInputData().getLong("currentTime", 0);
        Date currentTime = new Date(timeMillis);

        try {
            // Realizar la actualización en Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Task<Void> updateTask = db.collection("activities").document(activityId)
                    .collection("participations").document(userId)
                    .update("state", ParticipationState.NOW, "startTime", currentTime);

            // Bloquear hasta que se complete la tarea (esto se ejecuta en un hilo de fondo)
            Tasks.await(updateTask);

            return Result.success();
        } catch (Exception e) {
            return Result.retry(); // Reintentar si falla
        }
    }
}