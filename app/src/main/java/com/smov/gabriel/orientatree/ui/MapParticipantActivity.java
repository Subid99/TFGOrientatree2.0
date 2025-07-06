package com.smov.gabriel.orientatree.ui;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.model.Beacon;
import com.smov.gabriel.orientatree.persistence.AppDatabase;
import com.smov.gabriel.orientatree.persistence.entities.OfflineBeacon;
import com.smov.gabriel.orientatree.persistence.entities.OfflineBeaconReach;
import com.smov.gabriel.orientatree.persistence.entities.OfflineParticipation;
import com.smov.gabriel.orientatree.persistence.entities.OfflineTemplate;
import com.tfg.marllor.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.Map;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.TemplateType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapParticipantActivity extends AppCompatActivity implements OnMapReadyCallback {

    // Constants
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1110;
    private static final String pattern_hour = "HH:mm";
    private static final DateFormat df_hour = new SimpleDateFormat(pattern_hour);

    // UI Components
    private TextView reachesMap_textView, map_timer_textView, mapBeaconsBadge_textView;
    private MaterialButton mapBeacons_button;
    private FloatingActionButton map_fab, mapLocationOff_fab;
    private CircularProgressIndicator map_progressIndicator;
    private Toolbar toolbar;

    // Map Related
    private GoogleMap mMap;
    private Map templateMap;

    // Data Models
    private Participation participation;
    private Template template;
    private Activity activity;

    // Timer
    private Timer timer;
    private TimerTask timerTask;
    private Double time = 0.0;

    // Beacon Tracking
    private int num_beacons;
    private int beacons_reached;

    // Firebase Services
    private FirebaseFirestore db;
    private AppDatabase localDb;
    private FirebaseAuth mAuth;

    // User Info
    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("Hola", this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        initializeFirebaseServices();
        initializeUIComponents();
        setupMapFragment();
        setupToolbar();
        loadIntentData();
        setupListeners();
        setupParticipationListener();

    }

    // Initialization Methods
    private void initializeFirebaseServices() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        localDb = AppDatabase.getDatabase(this);
    }

    private void initializeUIComponents() {
        mapBeacons_button = findViewById(R.id.beaconsMap_button);
        reachesMap_textView = findViewById(R.id.reachesMap_textView);
        map_timer_textView = findViewById(R.id.map_timer_textView);
        map_fab = findViewById(R.id.map_fab);
        map_progressIndicator = findViewById(R.id.map_progressIndicator);
        mapLocationOff_fab = findViewById(R.id.mapLocationOff_fab);
        toolbar = findViewById(R.id.map_toolbar);
        mapBeaconsBadge_textView = findViewById(R.id.mapBeaconsBadge_textView);
    }

    private void setupMapFragment() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void loadIntentData() {
        Intent intent = getIntent();
        template = (Template) intent.getSerializableExtra("template");
        activity = (Activity) intent.getSerializableExtra("activity");
        Log.v("mapLocationHelp", String.valueOf( activity.isLocation_help()));
        Log.v("mapLocationHelp", String.valueOf( template.getBeacons().size()));
    }

    // Listener Setup Methods
    private void setupListeners() {
        mapBeacons_button.setOnClickListener(v -> updateUIReaches(activity));
        map_fab.setOnClickListener(v -> enableLocation());
        mapLocationOff_fab.setOnClickListener(v -> disableLocation());
    }

    private void setupParticipationListener() {
        if (activity == null || template == null || userID == null) return;

        Log.v("updateBeaconUi", "estoy en setupParticipationListener");
        new Thread(() -> {
            OfflineParticipation localParticipation = localDb.participationDao().getParticipation(userID,activity.getId());
            Log.v("updateBeaconUi", "estoy en setupParticipationListener con la offlineParticipation");
            if (localParticipation != null) {
                Log.v("updateBeaconUi", "la LocalParticipation no es null");
                // Convertir la entidad a modelo y actualizar UI
                participation = convertToParticipation(localParticipation);
                if (participation == null || participation.getStartTime() == null) return;
                Log.v("updateBeaconUi", "el getstarttime no es null");
                Date current_time = new Date();
                switch (participation.getState()) {
                    case NOW:
                        handleOngoingParticipation(current_time);
                        setupBeaconReachesListener();
                        break;
                    case FINISHED:
                        handleFinishedParticipation();
                        setupBeaconReachesListener();
                        break;
                }
            }
            else if (isOnline()) {
                db.collection("activities").document(activity.getId())
                        .collection("participations").document(userID)
                        .addSnapshotListener((value, error) -> {
                            if (error != null || value == null || !value.exists()) return;

                            participation = value.toObject(Participation.class);
                            if (participation == null || participation.getStartTime() == null) return;

                            Date current_time = new Date();
                            switch (participation.getState()) {
                                case NOW:
                                    handleOngoingParticipation(current_time);
                                    setupBeaconReachesListener();
                                    break;
                                case FINISHED:
                                    handleFinishedParticipation();
                                    setupBeaconReachesListener();
                                    break;
                            }
                        });
            } else {
                showErrorAndReturn("No se encontraron datos locales y no hay conexión disponible");
            }
        }).start();
        if (activity.isLocation_help()) {
            map_fab.setEnabled(true);
            map_fab.setVisibility(View.VISIBLE);
        }
    }

    private void setupBeaconReachesListener() {
        Log.v("updateBeaconUi", "estoy en setupBeaconReachesListener");
        if (activity == null || template == null || template.getBeacons() == null) return;

        num_beacons = template.getBeacons().size();
        if (isOnline()) {
            db.collection("activities").document(activity.getId())
                    .collection("participations").document(userID)
                    .collection("beaconReaches")
                    .addSnapshotListener((value, e) -> {
                        if (e != null || value == null) return;

                        beacons_reached = value.size();
                        updateBeaconUI(value);
                    });
        }
        else{
            new Thread(() -> {
                List<OfflineBeaconReach> llegadasOffline = localDb.beaconReachDao().getBeaconReachesForActivity(activity.getId(), participation.getParticipant());
                ArrayList<BeaconReached> llegadas = new ArrayList<BeaconReached>();
                for (int i=0;llegadasOffline.size()>i;i++){
                    BeaconReached beaconReached = convertToBeacon(llegadasOffline.get(i));
                }
                beacons_reached =llegadasOffline.size();
                updateBeaconUI(null);
            }).start();
        }

    }

    // UI Update Methods
    private void updateBeaconUI(QuerySnapshot value) {
        Log.v("updateBeaconUi", String.valueOf(beacons_reached));
        Log.v("updateBeaconUi", String.valueOf(num_beacons));
        reachesMap_textView.setText(beacons_reached + "/" + num_beacons);

        if (beacons_reached > 0 && template.getType() == TemplateType.EDUCATIVA) {
            int not_answered = countUnansweredBeacons(value);
            updateBadgeVisibility(not_answered);
        } else {
            mapBeaconsBadge_textView.setVisibility(View.INVISIBLE);
        }
    }

    private int countUnansweredBeacons(QuerySnapshot value) {
        int count = 0;
        for (DocumentSnapshot document : value.getDocuments()) {
            BeaconReached beaconReached = document.toObject(BeaconReached.class);
            if (beaconReached != null && !beaconReached.isAnswered()) {
                count++;
            }
        }
        return count;
    }

    private void updateBadgeVisibility(int not_answered) {
        if (not_answered > 0) {
            mapBeaconsBadge_textView.setText(String.valueOf(not_answered));
            mapBeaconsBadge_textView.setVisibility(View.VISIBLE);
        } else {
            mapBeaconsBadge_textView.setVisibility(View.INVISIBLE);
        }
    }

    // Participation State Handlers
    private void handleOngoingParticipation(Date current_time) {
        long diff_to_now = Math.abs(participation.getStartTime().getTime() - current_time.getTime()) / 1000;
        time = (double) diff_to_now;

        if (time < 86400) {
            startTimer();
        }
    }

    private void handleFinishedParticipation() {
        stopTimer();
        if (participation.getFinishTime() != null) {
            long diff_to_finish = Math.abs(participation.getStartTime().getTime() -
                    participation.getFinishTime().getTime()) / 1000;
            double total_time = (double) diff_to_finish;
            map_timer_textView.setText(getTimerText(total_time));
        }
    }

    // Timer Methods
    private void startTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    time++;
                    map_timer_textView.setText(getTimerText());
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, 1000);
    }

    private void stopTimer() {
        if (timerTask != null) {
            timerTask.cancel();
        }
    }

    // Location Methods
    private void enableLocation() {
        if (hasLocationPermission()) {
            toggleLocationUI(true);
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            requestLocationPermission();
        }
    }

    private void disableLocation() {
        if (hasLocationPermission()) {
            toggleLocationUI(false);
            if (mMap != null) {
                mMap.setMyLocationEnabled(false);
            }
        } else {
            requestLocationPermission();
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void toggleLocationUI(boolean locationEnabled) {
        map_fab.setVisibility(locationEnabled ? View.GONE : View.VISIBLE);
        map_fab.setEnabled(!locationEnabled);
        mapLocationOff_fab.setVisibility(locationEnabled ? View.VISIBLE : View.GONE);
        mapLocationOff_fab.setEnabled(locationEnabled);
    }

    // Navigation Methods
    private void updateUIReaches(Activity activity) {
        Intent intent = new Intent(this, ReachesActivity.class);
        intent.putExtra("activity", activity);
        intent.putExtra("template", template);
        startActivity(intent);
    }

    // Map Methods
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setupMapStyle();
        loadMapData();
    }

    private void setupMapStyle() {
        try {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                showToast("Algo salió mal al configurar el mapa");
            }
        } catch (Resources.NotFoundException e) {
            showToast("Algo salió mal al configurar el mapa");
        }
    }

    private void loadMapData() {
        if (template == null) {
            showToast("Algo salió mal al cargar el mapa");
            return;
        }

        db.collection("maps").document(template.getMap_id())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    templateMap = documentSnapshot.toObject(Map.class);
                    if (templateMap != null) {
                        setupMapOverlay();
                    }
                });
    }

    private void setupMapOverlay() {
        LatLng center_map = new LatLng(templateMap.getCentering_point().getLatitude(),
                templateMap.getCentering_point().getLongitude());

        Bitmap image_bitmap = loadMapImage();
        if (image_bitmap == null) return;

        BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(image_bitmap);
        LatLngBounds overlay_bounds = createOverlayBounds();

        GroundOverlayOptions overlayMap = new GroundOverlayOptions()
                .image(image)
                .positionFromBounds(overlay_bounds);

        mMap.addGroundOverlay(overlayMap);
        centerMap(center_map);
        setMapBounds();
    }

    private Bitmap loadMapImage() {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, activity.getTemplate() + ".png");
        return decodeFile(mypath, 540, 960);
    }

    private LatLngBounds createOverlayBounds() {
        return new LatLngBounds(
                new LatLng(templateMap.getOverlay_corners().get(0).getLatitude(),
                        templateMap.getOverlay_corners().get(0).getLongitude()),
                new LatLng(templateMap.getOverlay_corners().get(1).getLatitude(),
                        templateMap.getOverlay_corners().get(1).getLongitude()));
    }

    private void centerMap(LatLng center) {
        mMap.moveCamera(CameraUpdateFactory.newLatLng(center));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, templateMap.getInitial_zoom()));
    }

    private void setMapBounds() {
        mMap.setMinZoomPreference(templateMap.getMin_zoom());
        mMap.setMaxZoomPreference(templateMap.getMax_zoom());

        LatLngBounds map_bounds = new LatLngBounds(
                new LatLng(templateMap.getMap_corners().get(0).getLatitude(),
                        templateMap.getMap_corners().get(0).getLongitude()),
                new LatLng(templateMap.getMap_corners().get(1).getLatitude(),
                        templateMap.getMap_corners().get(1).getLongitude()));
        mMap.setLatLngBoundsForCameraTarget(map_bounds);
    }

    // Image Processing Methods
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private Bitmap decodeFile(File f, int width, int height) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            int scale = calculateInSampleSize(o, width, height);

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
            showToast("Algo salió mal al cargar el mapa");
            return null;
        }
    }

    // Time Formatting Methods
    private String getTimerText() {
        return formatTime(time);
    }

    private String getTimerText(double time) {
        return formatTime(time);
    }

    private String formatTime(double time) {
        int rounded = (int) Math.round(time);
        int seconds = ((rounded % 86400) % 3600) % 60;
        int minutes = ((rounded % 86400) % 3600) / 60;
        int hours = ((rounded % 86400) / 3600);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // Permission Handling
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            showToast("Ahora ya puedes comenzar la actividad");
        }
    }

    // Menu Handling
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Helper Methods
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private boolean isOnline() {
        /*ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();*/
        return true;
    }
    private void showErrorAndReturn(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private Participation convertToParticipation(OfflineParticipation localParticipation){
        Participation participation = new Participation();
        participation.setParticipant(localParticipation.participant);
        participation.setStartTime(localParticipation.startTime);
        participation.setFinishTime(localParticipation.finishTime);
        participation.setReaches(new ArrayList<BeaconReached>(localParticipation.reaches));
        participation.setState(localParticipation.state);
        return participation;
    }
    private BeaconReached convertToBeacon(OfflineBeaconReach localBeaconReach){
        BeaconReached beaconReached = new BeaconReached();
        beaconReached.setBeacon_id(localBeaconReach.beaconId);
        beaconReached.setReachMoment(new Date(localBeaconReach.reachTime));
        return beaconReached;
    }
}