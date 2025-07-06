package com.smov.gabriel.orientatree;

import android.app.Application;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class mainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Configuración inicial de WorkManager
        //configureWorkManager();

        // 2. Configuración de Firestore para persistencia offline
        configureFirestore();
    }

    private void configureWorkManager() {
        Configuration workManagerConfig = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG) // Nivel de logs
                .build();

        WorkManager.initialize(this, workManagerConfig);
    }

    private void configureFirestore() {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true) // Habilita cache offline
                .build();

        FirebaseFirestore.getInstance().setFirestoreSettings(settings);
    }
}