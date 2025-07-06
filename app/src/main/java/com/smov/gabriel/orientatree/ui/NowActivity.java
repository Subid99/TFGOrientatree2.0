package com.smov.gabriel.orientatree.ui;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.smov.gabriel.orientatree.persistence.AppDatabase;
import com.smov.gabriel.orientatree.persistence.Converters;
import com.smov.gabriel.orientatree.persistence.daos.ParticipationUpdateWorker;
import com.smov.gabriel.orientatree.persistence.entities.OfflineParticipation;
import com.smov.gabriel.orientatree.persistence.entities.OfflineTemplate;
import com.tfg.marllor.orientatree.R;
import com.smov.gabriel.orientatree.helpers.ActivityTime;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.TemplateType;
import com.smov.gabriel.orientatree.model.User;
import com.smov.gabriel.orientatree.services.LocationService;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class NowActivity extends AppCompatActivity {

    // Constants
    private static final int FINE_LOCATION_ACCESS_REQUEST_CODE = 1001;
    private static final float LOCATION_PRECISION = 1000f;
    private static final String pattern_hour = "HH:mm";
    private static final String pattern_day = "dd/MM/yyyy";
    private static final DateFormat df_hour = new SimpleDateFormat(pattern_hour);
    private static final DateFormat df_date = new SimpleDateFormat(pattern_day);

    // UI Elements
    private TextView nowType_textView, nowTitle_textView, nowTime_textView, nowOrganizer_textView,
            nowTemplate_textView, nowDescription_textView, nowNorms_textView,
            nowLocation_textView, nowMode_textView, nowState_textView;
    private ExtendedFloatingActionButton nowParticipant_extendedFab, nowSeeParticipants_extendedFab,
            nowDownloadMap_extendedFab;
    private MaterialButton nowCredentials_button, nowMap_button;
    private Toolbar toolbar;
    private ImageView now_imageView;
    private CoordinatorLayout now_coordinatorLayout;
    private CircularProgressIndicator now_progressIndicator;

    // Firebase Services
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    // Model Objects
    private Activity activity;
    private Template template;
    private User user;
    private Participation participation;

    // IDs and Flags
    private String userID;
    private String organizerID;
    private boolean isOrganizer = false;
    private boolean havePermissions = false;

    // Location Services
    private FusedLocationProviderClient fusedLocationClient;
    private Intent locationServiceIntent;
    private ActivityTime activityTime;
    private AppDatabase localDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("Hola", this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now);

        initializeActivityData();
        bindUIElements();
        initializeFirebaseServices();
        setupToolbar();
        setupLocationServices();
        setupUI();
        setupListeners();
    }

    private void initializeActivityData() {
        Intent intent = getIntent();
        activity = (Activity) intent.getSerializableExtra("activity");
        activityTime = getActivityTime();
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void bindUIElements() {
        toolbar = findViewById(R.id.now_toolbar);
        nowCredentials_button = findViewById(R.id.nowCredentials_button);
        nowParticipant_extendedFab = findViewById(R.id.nowParticipant_extendedFab);
        nowSeeParticipants_extendedFab = findViewById(R.id.nowSeeParticipants_extendedFab);
        nowType_textView = findViewById(R.id.nowType_textView);
        nowTitle_textView = findViewById(R.id.nowTitle_textView);
        nowTime_textView = findViewById(R.id.nowTime_textView);
        nowOrganizer_textView = findViewById(R.id.nowOrganizer_textView);
        nowTemplate_textView = findViewById(R.id.nowTemplate_textView);
        nowDescription_textView = findViewById(R.id.nowDescription_textView);
        nowNorms_textView = findViewById(R.id.nowNorms_textView);
        nowLocation_textView = findViewById(R.id.nowLocation_textView);
        now_imageView = findViewById(R.id.now_imageView);
        now_coordinatorLayout = findViewById(R.id.now_coordinatorLayout);
        nowState_textView = findViewById(R.id.nowState_textView);
        nowMode_textView = findViewById(R.id.nowMode_textView);
        now_progressIndicator = findViewById(R.id.now_progressIndicator);
        nowDownloadMap_extendedFab = findViewById(R.id.nowDownloadMap_extendedFab);
        nowMap_button = findViewById(R.id.nowMap_button);
    }

    private void initializeFirebaseServices() {
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        localDb = AppDatabase.getDatabase(this);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        switch (activityTime) {
            case PAST:
                getSupportActionBar().setTitle("Actividad terminada");
                break;
            case ONGOING:
                getSupportActionBar().setTitle("Actividad en curso");
                break;
            case FUTURE:
                getSupportActionBar().setTitle("Actividad prevista");
                break;
        }
    }

    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationServiceIntent = new Intent(this, LocationService.class);
    }

    private void setupUI() {
        if (activity == null) {
            Toast.makeText(this, "Algo salió mal al cargar la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
            return;
        }

        organizerID = activity.getPlanner_id();
        isOrganizer = organizerID.equals(userID) && !activity.getParticipants().contains(userID);

        nowTitle_textView.setText(activity.getTitle());
        nowMode_textView.append(activity.isScore() ? "score" : "orientación clásica");

        String timeString = df_hour.format(activity.getStartTime()) + " - " +
                df_hour.format(activity.getFinishTime()) +
                " (" + df_date.format(activity.getStartTime()) + ")";
        nowTime_textView.setText(timeString);

        loadActivityImage(activity.getTemplate());
        loadTemplateData();
        checkLocationPermissions();
    }

    private void loadActivityImage(String templateId) {
        File localFile = new File(this.getFilesDir(), "template_" + templateId + ".jpg");

        if (localFile.exists()) {
            Log.v("ImageDownload", "ExisteImage"+localFile.getPath());
            Glide.with(this)
                    .load(localFile)
                    .into(now_imageView);
        } else {
            // Si no existe localmente, cargar desde Firebase Storage (si hay conexión)
            if (isOnline()) {
                StorageReference ref = storageReference.child("templateImages/" + activity.getTemplate() + ".jpg");
                Glide.with(this)
                        .load(ref)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(now_imageView);
            } else {
                // Mostrar placeholder si no hay conexión ni imagen local
                Glide.with(this)
                        .load(R.drawable.ic_peacock)
                        .into(now_imageView);
            }
        }
    }

    private void loadTemplateData() {
        // Primero intentar cargar desde la base de datos local
        new Thread(() -> {
            OfflineTemplate localTemplate = localDb.templateDao().getTemplateById(activity.getTemplate());

            if (localTemplate != null) {
                // Convertir la entidad a modelo y actualizar UI
                Template template = convertToTemplate(localTemplate);
                updateTemplateUI(template);
            } else {
                // Si no está localmente, intentar desde Firestore (si hay conexión)
                if (isOnline()) {
                    loadTemplateFromFirestore();
                } else {
                    showErrorAndReturn("No se encontraron datos locales y no hay conexión disponible");
                }
            }
        }).start();
    }

    private void loadTemplateFromFirestore() {
        db.collection("templates").document(activity.getTemplate())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Template template = documentSnapshot.toObject(Template.class);
                        if (template == null) {
                            showErrorAndReturn("Algo salió mal al cargar la actividad. Sal y vuelve a intentarlo");
                            return;
                        }

                        // Guardar localmente para próximas veces
                        saveTemplateLocally(template);

                        // Actualizar UI
                        updateTemplateUI(template);
                    } else {
                        showErrorAndReturn("La plantilla no existe en la base de datos");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TemplateLoad", "Error al cargar plantilla", e);
                    showErrorAndReturn("Algo salió mal al cargar la actividad. Sal y vuelve a intentarlo");
                });
    }

    private void updateTemplateUI(Template template) {
        runOnUiThread(() -> {
            this.template = template;

            nowType_textView.setText(template.getType().toString());
            if (template.getType() == TemplateType.EDUCATIVA && template.getColor() != null) {
                nowType_textView.append(" " + template.getColor());
            }
            nowDescription_textView.setText(template.getDescription());
            nowTemplate_textView.append(template.getName());
            nowLocation_textView.append(template.getLocation());
            nowNorms_textView.setText(template.getNorms());

            setupRoleSpecificUI();
            loadOrganizerData();
        });
    }

    private void saveTemplateLocally(Template template) {
        new Thread(() -> {
            try {
                localDb.templateDao().insertTemplate(Converters.toEntity(template));
                Log.d("TemplateSave", "Plantilla guardada localmente: " + template.getTemplate_id());
            } catch (Exception e) {
                Log.e("TemplateSave", "Error al guardar plantilla localmente", e);
            }
        }).start();
    }

    private void setupRoleSpecificUI() {
        if (isOrganizer) {
            setupOrganizerUI();
        } else {
            setupParticipantUI();
        }
    }

    private void setupOrganizerUI() {
        nowParticipant_extendedFab.setEnabled(false);
        nowParticipant_extendedFab.setVisibility(View.GONE);
        nowState_textView.setVisibility(View.GONE);

        nowCredentials_button.setEnabled(true);
        nowCredentials_button.setVisibility(View.VISIBLE);
        nowMap_button.setEnabled(true);
        nowMap_button.setVisibility(View.VISIBLE);

        if (activityTime == ActivityTime.PAST && template.getType() == TemplateType.DEPORTIVA) {
            nowSeeParticipants_extendedFab.setText("Clasificación");
            nowSeeParticipants_extendedFab.setIcon(getResources().getDrawable(R.drawable.ic_trophy));
        }

        nowSeeParticipants_extendedFab.setEnabled(true);
        nowSeeParticipants_extendedFab.setVisibility(View.VISIBLE);
    }

    private void setupParticipantUI() {
        nowCredentials_button.setEnabled(false);
        nowCredentials_button.setVisibility(View.GONE);

        switch (activityTime) {
            case PAST:
                if (template.getType() == TemplateType.DEPORTIVA) {
                    nowSeeParticipants_extendedFab.setText("Clasificación");
                    nowSeeParticipants_extendedFab.setIcon(getResources().getDrawable(R.drawable.ic_trophy));
                    nowSeeParticipants_extendedFab.setEnabled(true);
                    nowSeeParticipants_extendedFab.setVisibility(View.VISIBLE);
                }
                nowParticipant_extendedFab.setEnabled(false);
                nowParticipant_extendedFab.setVisibility(View.GONE);
                nowState_textView.setVisibility(View.GONE);
                break;
            case ONGOING:
            case FUTURE:
                nowSeeParticipants_extendedFab.setEnabled(false);
                nowSeeParticipants_extendedFab.setVisibility(View.GONE);
                if (activityTime == ActivityTime.FUTURE) {
                    nowParticipant_extendedFab.setEnabled(false);
                    nowParticipant_extendedFab.setVisibility(View.GONE);
                    nowState_textView.setVisibility(View.GONE);
                }
                break;
        }

        setupParticipationListener();
    }

    private void setupParticipationListener() {
        // Verificar si hay conexión a internet
        if (isOnline()) {
            setupFirestoreParticipationListener();
        } else {
            setupLocalParticipationObserver();
        }
    }

    private void setupFirestoreParticipationListener() {
        db.collection("activities").document(activity.getId())
                .collection("participations").document(userID)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        Log.e("ParticipationListener", "Error en listener", e);
                        checkLocalParticipation(); // Fallback a datos locales
                        return;
                    }

                    if (snapshot == null || !snapshot.exists()) {
                        checkLocalParticipation(); // Fallback a datos locales
                        return;
                    }

                    participation = snapshot.toObject(Participation.class);
                    saveParticipationLocally(participation);
                    updateParticipationUI();
                });
    }

    private void setupLocalParticipationObserver() {
        new Thread(() -> {
            OfflineParticipation localParticipation = localDb.participationDao()
                    .getParticipation(userID, activity.getId());

            runOnUiThread(() -> {
                if (localParticipation != null) {
                    participation = toModel(localParticipation);
                    updateParticipationUI();
                } else {
                    // Mostrar estado por defecto o mensaje de no participación
                    nowDownloadMap_extendedFab.setEnabled(false);
                    nowDownloadMap_extendedFab.setVisibility(View.GONE);
                    Toast.makeText(this, "Modo offline - Sin datos de participación local",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void checkLocalParticipation() {
        new Thread(() -> {
            OfflineParticipation localParticipation = localDb.participationDao()
                    .getParticipation(userID, activity.getId());

            runOnUiThread(() -> {
                if (localParticipation != null) {
                    participation = toModel(localParticipation);
                    updateParticipationUI();
                }
            });
        }).start();
    }

    private void updateParticipationUI() {
        if (activityTime == ActivityTime.ONGOING) {
            if (mapDownloaded()) {
                enableRightParticipantOptions();
            } else {
                nowDownloadMap_extendedFab.setEnabled(true);
                nowDownloadMap_extendedFab.setVisibility(View.VISIBLE);
                updateParticipationStateUI();
            }
        } else {
            enableRightParticipantOptions();
        }
    }

    private void saveParticipationLocally(Participation participation) {
        new Thread(() -> {
            OfflineParticipation entity = toEntity(participation);
            localDb.participationDao().insertParticipation(entity);
        }).start();
    }

    private void updateParticipationStateUI() {
        if (participation == null) return;

        switch (participation.getState()) {
            case NOT_YET:
                nowState_textView.setText("Estado: no comenzada");
                break;
            case NOW:
                nowState_textView.setText("Estado: aún no terminada");
                break;
            case FINISHED:
                nowState_textView.setText("Estado: terminada");
                break;
        }
    }

    private void loadOrganizerData() {
        db.collection("users").document(organizerID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        nowOrganizer_textView.append(user.getName() + " " + user.getSurname());
                    }
                });
    }

    private void checkLocationPermissions() {
        if (!isOrganizer &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
        } else {
            havePermissions = true;
        }
    }

    private void setupListeners() {
        nowParticipant_extendedFab.setOnClickListener(v -> handleParticipantFabClick());
        nowSeeParticipants_extendedFab.setOnClickListener(v -> handleSeeParticipantsClick());
        nowDownloadMap_extendedFab.setOnClickListener(v -> handleDownloadMapClick());
        nowMap_button.setOnClickListener(v -> handleMapButtonClick());
        nowCredentials_button.setOnClickListener(v -> showCredentialsDialog());
    }

    private void handleParticipantFabClick() {
        if (!havePermissions) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
            return;
        }

        Date current_time = new Date();
        if (current_time.after(activity.getFinishTime())) {
            nowParticipant_extendedFab.setEnabled(false);
            nowParticipant_extendedFab.setVisibility(View.GONE);
            showSnackBar("Esta actividad ya ha terminado");
            return;
        }

        new MaterialAlertDialogBuilder(this)
                .setMessage("¿Deseas comenzar/retomar la actividad? Solo deberías " +
                        "hacerlo si el/la organizador/a ya te ha dado la salida")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Comenzar", (dialog, which) -> startActivityWithLocationCheck())
                .show();
    }

    private void startActivityWithLocationCheck() {
        now_progressIndicator.setVisibility(View.VISIBLE);
        Log.v("Continuar","Entrando");
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        Log.v("Continuar","En getLastLocation");
                        if (location == null) {
                            now_progressIndicator.setVisibility(View.GONE);

                            Toast.makeText(this, "Hubo algún problema al obtener la ubicación. Vuelve a intentarlo.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        float diff = getDistance(location.getLatitude(), template.getStart_lat(),
                                location.getLongitude(), template.getStart_lng());

                        if ((diff <= LOCATION_PRECISION && participation.getState() == ParticipationState.NOT_YET)
                                || participation.getState() == ParticipationState.NOW) {
                            handleValidLocation();
                        } else {
                            now_progressIndicator.setVisibility(View.GONE);
                            showSnackBar("Estás demasiado lejos de la salida (" + (int) diff + "). Acércate a ella y vuelve a intentarlo");
                        }
                    });
        } catch (SecurityException e) {
            now_progressIndicator.setVisibility(View.GONE);
            showSnackBar("Parece que hay algún problema con los permisos de ubicación");
        }
    }

    private void handleValidLocation() {
        Log.v("Continuar","En handleValidLocation");
        StorageReference reference = storageReference.child("maps/" + activity.getTemplate() + ".png");
        if (LocationService.executing) {
            now_progressIndicator.setVisibility(View.GONE);
            Log.v("Continuar","En locationServiceExecuting");
            Toast.makeText(this, "No se pudo iniciar la actividad... ya hay un servicio ejecutándose", Toast.LENGTH_SHORT).show();
            return;
        }

        switch (participation.getState()) {
            case NOT_YET:
                updateParticipationStartTime();
                Log.v("Continuar","En notyet");
                break;
            case NOW:
                Log.v("Continuar","En Now");
                startLocationService();
                updateUIMap();
                break;
            default:
                Log.v("Continuar","En default");
                now_progressIndicator.setVisibility(View.GONE);
                Toast.makeText(this, "Parece que la actividad ya ha terminado", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void updateParticipationStartTime() {
        Date current_time = new Date();

        // Crear los datos que necesitamos pasar al worker
        Data inputData = new Data.Builder()
                .putString("activityId", activity.getId())
                .putString("userId", userID)
                .putLong("currentTime", current_time.getTime())
                .build();

        // Construir la solicitud de trabajo con restricción de red
        OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(ParticipationUpdateWorker.class)
                .setInputData(inputData)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .build();

        // Programar el trabajo
        WorkManager.getInstance(this).enqueue(uploadRequest);

        // Mostrar progreso y deshabilitar botones inmediatamente (feedback UX)
        now_progressIndicator.setVisibility(View.GONE);
        nowParticipant_extendedFab.setEnabled(false);
        nowParticipant_extendedFab.setVisibility(View.GONE);
        startLocationService();
        nowMap_button.setEnabled(true);
        nowMap_button.setVisibility(View.VISIBLE);
        updateUIMap();
        new Thread(() -> {
            localDb.participationDao().updateParticipationState(participation.getParticipant(), activity.getId(), ParticipationState.NOW);
            localDb.participationDao().updateParticipationStartTime(participation.getParticipant(), activity.getId(), current_time);
        }).start();
    }

    private void startLocationService() {
        locationServiceIntent.putExtra("activity", activity);
        locationServiceIntent.putExtra("template", template);
        Log.v("Continuar","En startlocation service");
        startService(locationServiceIntent);
        Log.v("Continuar","tras startlocation service");
    }

    private void handleSeeParticipantsClick() {
        if (template == null || activity == null) {
            Toast.makeText(this, "No se pudo completar la acción. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mapDownloaded()) {
            updateUIParticipants();
        } else {
            downloadMapAndUpdateParticipants();
        }
    }

    private void downloadMapAndUpdateParticipants() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Cargando el mapa...");
        pd.show();

        StorageReference reference = storageReference.child("maps/" + activity.getTemplate() + ".png");
        try {
            File localFile = File.createTempFile("images", "png");
            reference.getFile(localFile)
                    .addOnSuccessListener(taskSnapshot -> handleMapDownloadSuccess(localFile, pd, this::updateUIParticipants))
                    .addOnFailureListener(e -> {
                        pd.dismiss();
                        showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                    })
                    .addOnProgressListener(snapshot -> updateProgressDialog(pd, snapshot));
        } catch (IOException e) {
            pd.dismiss();
            showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
        }
    }

    private void handleDownloadMapClick() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Cargando el mapa...");
        pd.show();

        StorageReference reference = storageReference.child("maps/" + activity.getTemplate() + ".png");
        try {
            File localFile = File.createTempFile("images", "png");
            reference.getFile(localFile)
                    .addOnSuccessListener(taskSnapshot -> handleMapDownloadSuccess(localFile, pd, () -> {
                        showSnackBar("Mapa descargado con éxito, ya puedes comenzar la actividad");
                        nowDownloadMap_extendedFab.setVisibility(View.GONE);
                        nowDownloadMap_extendedFab.setEnabled(false);
                        enableRightParticipantOptions();
                    }))
                    .addOnFailureListener(e -> {
                        pd.dismiss();
                        showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                    })
                    .addOnProgressListener(snapshot -> updateProgressDialog(pd, snapshot));
        } catch (IOException e) {
            pd.dismiss();
            showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
        }
    }

    private void handleMapDownloadSuccess(File localFile, ProgressDialog pd, Runnable onSuccess) {
        Bitmap bmp = BitmapFactory.decodeFile(localFile.getAbsolutePath());
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, activity.getTemplate() + ".png");

        try (FileOutputStream fos = new FileOutputStream(mypath)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            onSuccess.run();
        } catch (Exception e) {
            e.printStackTrace();
            showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
        } finally {
            pd.dismiss();
        }
    }

    private void updateProgressDialog(ProgressDialog pd, FileDownloadTask.TaskSnapshot snapshot) {
        double progressPercent = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
        if (progressPercent <= 90) {
            pd.setMessage("Progreso: " + (int) progressPercent + "%");
        } else {
            pd.setMessage("Descargado. Espera unos instantes mientras el mapa se guarda en el dispositivo");
        }
    }

    private void handleMapButtonClick() {
        if (activity.getPlanner_id().equals(userID)) {
            handleOrganizerMapButton();
        } else if (activity.getParticipants().contains(userID)) {
            handleParticipantMapButton();
        }
    }

    private void handleOrganizerMapButton() {
        if (mapDownloaded()) {
            updateUIOrganizerMap();
        } else {
            downloadMapAndUpdateOrganizerMap();
        }
    }

    private void downloadMapAndUpdateOrganizerMap() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setTitle("Cargando el mapa...");
        pd.show();

        StorageReference reference = storageReference.child("maps/" + activity.getTemplate() + ".png");
        try {
            File localFile = File.createTempFile("images", "png");
            reference.getFile(localFile)
                    .addOnSuccessListener(taskSnapshot -> handleMapDownloadSuccess(localFile, pd, this::updateUIOrganizerMap))
                    .addOnFailureListener(e -> {
                        pd.dismiss();
                        showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                    })
                    .addOnProgressListener(snapshot -> updateProgressDialog(pd, snapshot));
        } catch (IOException e) {
            pd.dismiss();
            showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
        }
    }

    private void handleParticipantMapButton() {
        if (participation == null) return;

        switch (participation.getState()) {
            case NOT_YET:
                break;
            case NOW:
                if (!LocationService.executing) {
                    showMapWarningDialog();
                } else if (mapDownloaded()) {
                    updateUIMap();
                } else {
                    handleMissingMap();
                }
                break;
            case FINISHED:
                if (mapDownloaded()) {
                    updateUIMap();
                } else {
                    handleMissingMap();
                }
                break;
        }
    }

    private void showMapWarningDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Aviso sobre el mapa")
                .setMessage("Esta acción te mostrará el mapa de la actividad, pero " +
                        "el servicio que rastrea tu ubicación no está activo, por lo que" +
                        " no se registrará tu paso por las balizas. Si lo que quieres es retomar " +
                        "la actividad, cancela esta acción y pulsa el botón de Continuar")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Ver mapa", (dialog, which) -> {
                    if (mapDownloaded()) {
                        updateUIMap();
                    } else {
                        handleMissingMap();
                    }
                })
                .show();
    }

    private void handleMissingMap() {
        nowParticipant_extendedFab.setVisibility(View.GONE);
        nowParticipant_extendedFab.setEnabled(false);
        nowDownloadMap_extendedFab.setEnabled(true);
        nowDownloadMap_extendedFab.setVisibility(View.VISIBLE);
        showSnackBar("El mapa no está descargado. Descárgalo y vuelve a intentarlo");
        nowMap_button.setVisibility(View.GONE);
        nowMap_button.setEnabled(false);
    }

    private void showCredentialsDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Claves de acceso a la actividad")
                .setMessage("Identificador: " + activity.getVisible_id() +
                        "\nContraseña: " + activity.getKey())
                .setPositiveButton("OK", null)
                .show();
    }

    // Navigation Methods
    private void updateUIOrganizerMap() {
        Intent intent = new Intent(this, OrganizerMapActivity.class);
        intent.putExtra("activity", activity);
        intent.putExtra("template", template);
        startActivity(intent);
    }

    private void updateUIParticipants() {
        Intent intent = activity.getFinishTime().toInstant().isBefore(new Date().toInstant()) ?
                new Intent(this, ReviewActivity.class) :
                new Intent(this, VigilanciaActividadActivity.class);
        intent.putExtra("activity", activity);
        intent.putExtra("template", template);
        startActivity(intent);
    }

    private void updateUIMap() {
        Intent intent = new Intent(this, MapParticipantActivity.class);
        intent.putExtra("template", template);
        intent.putExtra("activity", activity);
        startActivity(intent);
    }

    private void updateUIHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateUIMyParticipation() {
        Intent intent = new Intent(this, MyParticipationActivity.class);
        Gson gson = new Gson();
        intent.putExtra("participation", gson.toJson(participation));
        intent.putExtra("activity", activity);
        intent.putExtra("template", template);
        startActivity(intent);
    }

    // Helper Methods
    private void showSnackBar(String message) {
        if (now_coordinatorLayout != null) {
            Snackbar.make(now_coordinatorLayout, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", v -> {})
                    .setDuration(8000)
                    .show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void showErrorAndReturn(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                havePermissions = true;
                Toast.makeText(this, "Ahora ya puedes usar toda la funcionalidad", Toast.LENGTH_SHORT).show();
            } else {
                showSnackBar("Es necesario dar permiso para poder participar en la actividad");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (activity == null || userID == null) {
            Toast.makeText(this, "Se produjo un error al carga las opciones del menú", Toast.LENGTH_SHORT).show();
            return super.onCreateOptionsMenu(menu);
        }

        if (!activity.getPlanner_id().equals(userID)) {
            getMenuInflater().inflate(R.menu.now_overflow_menu, menu);
            Date current_time = new Date();
            boolean isActivityOngoing = current_time.after(activity.getStartTime()) && current_time.before(activity.getFinishTime());
            menu.getItem(1).setEnabled(isActivityOngoing);
            menu.getItem(1).setVisible(isActivityOngoing);
        } else {
            getMenuInflater().inflate(R.menu.now_overflow_organizer_menu, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (activity == null || userID == null) return super.onOptionsItemSelected(item);

        if (!activity.getPlanner_id().equals(userID) && participation != null) {
            switch (item.getItemId()) {
                case R.id.participation_activity:
                    updateUIMyParticipation();
                    break;
                case R.id.quit_activity:
                    abandonActivity();
                    break;
            }
        } else if (activity.getPlanner_id().equals(userID)) {
            if (item.getItemId() == R.id.organizer_remove_activity) {
                removeActivity();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void abandonActivity() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Abandonar actividad")
                .setMessage("¿Estás seguro/a de que quieres abandonar esta actividad en curso?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sí", (dialog, which) -> {
                    Date current_time = new Date();
                    if (!(current_time.after(activity.getStartTime()) || !current_time.before(activity.getFinishTime()) || participation == null)) {
                        Toast.makeText(this, "La acción no se pudo completar. ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    switch (participation.getState()) {
                        case NOT_YET:
                            showNotStartedDialog();
                            break;
                        case NOW:
                            finishActivityParticipation(current_time);
                            break;
                        case FINISHED:
                            Toast.makeText(this, "La acción no se pudo completar porque ya has terminado tu participación", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showNotStartedDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("La acción no se puede realizar")
                .setMessage("Tu participación aún no ha comenzado. Si no quieres " +
                        "tomar parte de la actividad, puedes desinscribirte en " +
                        "Mi Participación")
                .setPositiveButton("OK", null)
                .show();
    }

    private void finishActivityParticipation(Date current_time) {
        now_progressIndicator.setVisibility(View.VISIBLE);
        db.collection("activities").document(activity.getId())
                .collection("participations").document(userID)
                .update("state", ParticipationState.FINISHED,
                        "finishTime", current_time,
                        "completed", false)
                .addOnSuccessListener(unused -> {
                    now_progressIndicator.setVisibility(View.GONE);
                    if (LocationService.executing) {
                        stopService(new Intent(this, LocationService.class));
                    }
                    showSnackBar("Has abandonado la actividad.");
                })
                .addOnFailureListener(e -> {
                    now_progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, "Algo salió mal al terminar la actividad. Vuelve a intentarlo.", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeActivity() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Eliminar actividad")
                .setMessage("Se borrará la actividad y todos sus datos. Los participantes tampoco " +
                        "podrán acceder a ella. ¿Estás seguro/a de que quieres continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sí", (dialog, which) -> {
                    now_progressIndicator.setVisibility(View.VISIBLE);
                    if (activity.getId() == null) {
                        Toast.makeText(this, "No se pudo eliminar la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("activities").document(activity.getId())
                            .delete()
                            .addOnSuccessListener(unused -> {
                                now_progressIndicator.setVisibility(View.GONE);
                                updateUIHome();
                            })
                            .addOnFailureListener(e -> {
                                now_progressIndicator.setVisibility(View.GONE);
                                Toast.makeText(this, "Se produjo un error al eliminar la actividad", Toast.LENGTH_SHORT).show();
                            });
                })
                .show();
    }

    private ActivityTime getActivityTime() {
        if (activity == null) return ActivityTime.FUTURE;

        Date current_date = new Date();
        if (activity.getStartTime().after(current_date)) {
            return ActivityTime.FUTURE;
        } else if (activity.getFinishTime().before(current_date)) {
            return ActivityTime.PAST;
        } else {
            return ActivityTime.ONGOING;
        }
    }

    private float getDistance(double lat1, double lat2, double lng1, double lng2) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (earthRadius * c);
    }

    private void enableRightParticipantOptions() {
        if (participation == null) return;

        switch (participation.getState()) {
            case NOT_YET:
                nowState_textView.setText("Estado: no comenzada");
                if (activityTime == ActivityTime.ONGOING) {
                    nowParticipant_extendedFab.setEnabled(true);
                    nowParticipant_extendedFab.setVisibility(View.VISIBLE);
                    nowParticipant_extendedFab.setText("Comenzar");
                }
                break;
            case NOW:
                nowState_textView.setText("Estado: aún no terminada");
                nowMap_button.setEnabled(true);
                nowMap_button.setVisibility(View.VISIBLE);
                if (!LocationService.executing && activityTime == ActivityTime.ONGOING) {
                    nowParticipant_extendedFab.setEnabled(true);
                    nowParticipant_extendedFab.setVisibility(View.VISIBLE);
                    nowParticipant_extendedFab.setText("Continuar");
                }
                break;
            case FINISHED:
                nowState_textView.setText("Estado: terminada");
                nowMap_button.setEnabled(true);
                nowMap_button.setVisibility(View.VISIBLE);
                nowParticipant_extendedFab.setEnabled(false);
                nowParticipant_extendedFab.setVisibility(View.GONE);
                break;
        }
    }

    public boolean mapDownloaded() {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, activity.getTemplate() + ".png");
        return mypath.exists();
    }
    public Template convertToTemplate(OfflineTemplate entity){
        Template template = new Template();
        template.setTemplate_id(entity.templateId);
        template.setName(entity.name);
        template.setType(entity.type);
        template.setColor(entity.color);
        template.setLocation(entity.location);
        template.setDescription(entity.description);
        template.setNorms(entity.norms);
        template.setMap_id(entity.mapId);
        template.setStart_lat(entity.startLat);
        template.setStart_lng(entity.startLng);
        template.setEnd_lat(entity.endLat);
        template.setEnd_lng(entity.endLng);
        template.setBeacons(new ArrayList<>(entity.beacons));
        template.setPassword(entity.password);
        return template;
    }

    public static Participation toModel(OfflineParticipation entity) {
        Participation participation = new Participation();
        participation.setParticipant(entity.participant);
        participation.setState(entity.state);
        participation.setStartTime(entity.startTime);
        participation.setFinishTime(entity.finishTime);
        participation.setCompleted(entity.completed);
        participation.setReaches(new ArrayList<>(entity.reaches));
        participation.setLastLocation(entity.lastLocation);
        return participation;
    }
    public static OfflineParticipation toEntity(Participation participation) {
        OfflineParticipation entity = new OfflineParticipation();
        entity.participant=participation.getParticipant();
        entity.state=participation.getState();
        entity.startTime=participation.getStartTime();
        entity.finishTime=participation.getFinishTime();
        entity.completed=participation.isCompleted();
        entity.reaches=participation.getReaches();
        entity.lastLocation=participation.getLastLocation();
        return entity;
    }
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}