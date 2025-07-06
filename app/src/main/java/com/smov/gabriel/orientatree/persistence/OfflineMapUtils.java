package com.smov.gabriel.orientatree.persistence;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.io.File;

public class OfflineMapUtils {
    private static final String OFFLINE_MAPS_DIR = "offline_maps";
    private static final String PREF_OFFLINE_MAPS = "offline_maps_prefs";

    public static void downloadMapArea(Context context, LatLngBounds bounds, String mapId) {
        // Crear directorio si no existe
        File dir = new File(context.getFilesDir(), OFFLINE_MAPS_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Guardar metadatos del área
        SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_MAPS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putFloat(mapId + "_sw_lat", (float)bounds.southwest.latitude);
        editor.putFloat(mapId + "_sw_lng", (float)bounds.southwest.longitude);
        editor.putFloat(mapId + "_ne_lat", (float)bounds.northeast.latitude);
        editor.putFloat(mapId + "_ne_lng", (float)bounds.northeast.longitude);
        editor.putLong(mapId + "_timestamp", System.currentTimeMillis());
        editor.apply();

        // Descargar los tiles del mapa (implementación simplificada)
        new DownloadMapTask(context, mapId, bounds).execute();
    }

    public static LatLngBounds getOfflineMapBounds(Context context, String mapId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_MAPS, Context.MODE_PRIVATE);

        if (!prefs.contains(mapId + "_sw_lat")) {
            return null;
        }

        double swLat = prefs.getFloat(mapId + "_sw_lat", 0);
        double swLng = prefs.getFloat(mapId + "_sw_lng", 0);
        double neLat = prefs.getFloat(mapId + "_ne_lat", 0);
        double neLng = prefs.getFloat(mapId + "_ne_lng", 0);

        return new LatLngBounds(new LatLng(swLat, swLng), new LatLng(neLat, neLng));
    }

    public static boolean hasOfflineMap(Context context, String mapId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_MAPS, Context.MODE_PRIVATE);
        return prefs.contains(mapId + "_sw_lat");
    }

    private static class DownloadMapTask extends AsyncTask<Void, Integer, Boolean> {
        private Context context;
        private String mapId;
        private LatLngBounds bounds;

        public DownloadMapTask(Context context, String mapId, LatLngBounds bounds) {
            this.context = context.getApplicationContext();
            this.mapId = mapId;
            this.bounds = bounds;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            // Implementación real de descarga de tiles
            // Esto es un ejemplo simplificado
            try {
                // Simular descarga
                for (int i = 0; i <= 100; i += 10) {
                    Thread.sleep(300);
                    publishProgress(i);
                }
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // Notificar progreso (podrías usar un BroadcastReceiver)
            Intent intent = new Intent("MAP_DOWNLOAD_PROGRESS");
            intent.putExtra("mapId", mapId);
            intent.putExtra("progress", progress[0]);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Intent intent = new Intent("MAP_DOWNLOAD_COMPLETE");
            intent.putExtra("mapId", mapId);
            intent.putExtra("success", success);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }
}
