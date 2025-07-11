package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;

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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Date;

public class NowActivity2 extends AppCompatActivity {

    // UI elements
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

    // declaring Firebase services
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    // some model objects required
    private Activity activity;
    private Template template;
    private User user;
    private Participation participation;

    // some useful IDs
    private String userID;
    private String organizerID;

    // here we represent whether the current user is the organizer of the activity or not
    private boolean isOrganizer = false;

    // formatters
    // to format the way hours are displayed
    private static String pattern_hour = "HH:mm";
    private static DateFormat df_hour = new SimpleDateFormat(pattern_hour);
    // to format the way dates are displayed
    private static String pattern_day = "dd/MM/yyyy";
    private static DateFormat df_date = new SimpleDateFormat(pattern_day);

    // location permissions
    // constant that represents query for fine location permission
    private static final int FINE_LOCATION_ACCESS_REQUEST_CODE = 1001;
    // this is true or false depending on whether we have location permissions or not
    private boolean havePermissions = false;

    // needed to check that the user is at the start spot
    private FusedLocationProviderClient fusedLocationClient;

    // intent to the location service that runs in foreground while the activity is on
    private Intent locationServiceIntent;

    // threshold precision in meters to consider that the user is at the start spot
    private static final float LOCATION_PRECISION = 1000f;

    /* here we store the information of the time of te activity so that we know if it was in the past
     * or it is taking place now, or if it is in the future */
    private ActivityTime activityTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("Hola",this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_now);

        // get the activity from the intent
        Intent intent = getIntent();
        activity = (Activity) intent.getSerializableExtra("activity");
        // get if the activity is in the past, present or future
        activityTime = getActivityTime();

        // binding UI elements
        toolbar = findViewById(R.id.now_toolbar);
        nowCredentials_button = findViewById(R.id.nowCredentials_button); // only visible to organizer
        nowParticipant_extendedFab = findViewById(R.id.nowParticipant_extendedFab); // only visible to participant
        nowSeeParticipants_extendedFab = findViewById(R.id.nowSeeParticipants_extendedFab); // only visible to organizer
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

        // set the toolbar
        toolbar = findViewById(R.id.now_toolbar);
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
            default:
                break;
        }

        // initializing Firebase services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        // location services initialization
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // set intent to location foreground service
        locationServiceIntent = new Intent(this, LocationService.class);

        // get the current user's ID
        userID = mAuth.getCurrentUser().getUid();

        // check that the activity is not null
        if (activity != null) {
            organizerID = activity.getPlanner_id();
            if (organizerID.equals(userID)
                    && !activity.getParticipants().contains(userID)) {
                // if the current user is the organizer
                isOrganizer = true;
            } else if (!organizerID.equals(userID)
                    && activity.getParticipants().contains(userID)) {
                // if the current user is a participant...
                isOrganizer = false;
            } else {
                Toast.makeText(this, "Algo salió mal al " +
                        "obtener el organizador de la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                return;
            }
            // if the activity is not null, set the UI, otherwise tell the user and do nothing
            nowTitle_textView.setText(activity.getTitle());
            if (activity.isScore()) {
                nowMode_textView.append("score");
            } else {
                nowMode_textView.append("orientación clásica");
            }
            String timeString = "";
            // append start and finish hours
            timeString = timeString + df_hour.format(activity.getStartTime()) + " - " +
                    df_hour.format(activity.getFinishTime());
            // append date
            timeString = timeString + " (" + df_date.format(activity.getStartTime()) + ")";
            nowTime_textView.setText(timeString);
            // get and set the activity image
            StorageReference ref = storageReference.child("templateImages/" + activity.getTemplate() + ".jpg");
            Glide.with(this)
                    .load(ref)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                    .skipMemoryCache(true) // prevent caching
                    .into(now_imageView);
            // get the template of the activity
            db.collection("templates").document(activity.getTemplate())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            template = documentSnapshot.toObject(Template.class);
                            if (template != null) {
                                // get the data from the template
                                nowType_textView.setText(template.getType().toString());
                                if (template.getType() == TemplateType.EDUCATIVA &&
                                        template.getColor() != null) {
                                    nowType_textView.append(" " + template.getColor());
                                }
                                nowDescription_textView.setText(template.getDescription());
                                nowTemplate_textView.append(template.getName());
                                nowLocation_textView.append(template.getLocation());
                                nowNorms_textView.setText(template.getNorms());
                                // now that we have all the data from both the activity and the template, perform specific
                                // actions depending on whether the user is the organizer or a participant
                                if (isOrganizer) {
                                    // if organizer:
                                    // 1) disable options that are in any case only for participants
                                    nowParticipant_extendedFab.setEnabled(false);
                                    nowParticipant_extendedFab.setVisibility(View.GONE);
                                    nowState_textView.setVisibility(View.GONE);
                                    // 2) enable options that are in any case enabled for organizer
                                    // always enable the button to see the credentials
                                    // always enable see map button
                                    nowCredentials_button.setEnabled(true);
                                    nowCredentials_button.setVisibility(View.VISIBLE);
                                    Log.v("NowAc","Estoy en lo de habilitar el mapita");
                                    nowMap_button.setEnabled(true);
                                    nowMap_button.setVisibility(View.VISIBLE);
                                    // 2.1) check if we need to change the text of the see participants FAB
                                    switch (activityTime) {
                                        case PAST:
                                            // if the activity is past
                                            if (template.getType() == TemplateType.DEPORTIVA) {
                                                // if it was DEPORTIVA, show as classification button
                                                nowSeeParticipants_extendedFab.setText("Clasificación");
                                                nowSeeParticipants_extendedFab.setIcon(getResources().getDrawable(R.drawable.ic_trophy));
                                            }
                                            break;
                                        default:
                                            break;
                                    }
                                    // always enable the see participants FAB
                                    nowSeeParticipants_extendedFab.setEnabled(true);
                                    nowSeeParticipants_extendedFab.setVisibility(View.VISIBLE);
                                } else {
                                    // if participant:
                                    // 1) disable organizer options
                                    nowCredentials_button.setEnabled(false);
                                    nowCredentials_button.setVisibility(View.GONE);
                                    // enable or disable FABS depending on the time
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
                                            nowSeeParticipants_extendedFab.setEnabled(false);
                                            nowSeeParticipants_extendedFab.setVisibility(View.GONE);
                                            break;
                                        case FUTURE:
                                            nowSeeParticipants_extendedFab.setEnabled(false);
                                            nowSeeParticipants_extendedFab.setVisibility(View.GONE);
                                            nowParticipant_extendedFab.setEnabled(false);
                                            nowParticipant_extendedFab.setVisibility(View.GONE);
                                            nowState_textView.setVisibility(View.GONE);
                                            break;
                                        default:
                                            break;
                                    }
                                    // 2) set listener to the participations collection to know which participant options
                                    // should be enabled
                                    db.collection("activities").document(activity.getId())
                                            .collection("participations").document(userID)
                                            .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                                @Override
                                                public void onEvent(@Nullable DocumentSnapshot snapshot,
                                                                    @Nullable FirebaseFirestoreException e) {
                                                    if (e != null) {
                                                        /*Toast.makeText(NowActivity2.this, "Algo salió mal al obtener la participación. " +
                                                                "Sal y vuelve a intentarlo.", Toast.LENGTH_SHORT).show();*/
                                                        return;
                                                    }
                                                    if (snapshot != null && snapshot.exists()) {
                                                        participation = snapshot.toObject(Participation.class);
                                                        //only fot testing
                                                        /*if(mapDownloaded()) {
                                                            deleteMap();
                                                        }*/
                                                        if (activityTime == ActivityTime.ONGOING) {
                                                            if (mapDownloaded()) {
                                                                // if map already downloaded
                                                                enableRightParticipantOptions();
                                                            } else {
                                                                // if map not yet downloaded
                                                                // we only enable the option of downloading the map
                                                                nowDownloadMap_extendedFab.setEnabled(true);
                                                                nowDownloadMap_extendedFab.setVisibility(View.VISIBLE);
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
                                                        } else {
                                                            enableRightParticipantOptions();
                                                        }
                                                    } else {
                                                        /*Toast.makeText(NowActivity2.this, "Algo salió mal al obtener la participación. " +
                                                                "Sal y vuelve a intentarlo.", Toast.LENGTH_SHORT).show();*/
                                                    }
                                                }
                                            });
                                    // in order to track location we have to check if we have at least one of the following permissions...
                                    // so if the user is a participant we make this checking
                                    if (ActivityCompat.checkSelfPermission(NowActivity2.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                            && ActivityCompat.checkSelfPermission(NowActivity2.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        // if we don't...
                                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
                                    } else {
                                        // if we do...
                                        havePermissions = true;
                                    }
                                }
                                // get the organizer for we need his/her name and surname
                                db.collection("users").document(organizerID)
                                        .get()
                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                            @Override
                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                user = documentSnapshot.toObject(User.class);
                                                nowOrganizer_textView.append(user.getName() + " " + user.getSurname());
                                            }
                                        });
                            } else {
                                // if the template is null
                                Toast.makeText(NowActivity2.this, "Algo salió mal al cargar la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull @NotNull Exception e) {
                            // if we couldn't read the template
                            Toast.makeText(NowActivity2.this, "Algo salió mal al cargar la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    });
        } else {
            // if the activity is null
            Toast.makeText(this, "Algo salió mal al cargar la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
            return;
        }

        // participant FAB listener
        nowParticipant_extendedFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (havePermissions) {
                    // if the user has given the permissions required...
                    // check that the activity has not finished yet (it could have, in the meantime while
                    // the user was reading the rules, for example)
                    Date current_time = new Date(System.currentTimeMillis());
                    if (current_time.before(activity.getFinishTime())) {
                        // if the activity has not yet finished
                        new MaterialAlertDialogBuilder(NowActivity2.this)
                                .setMessage("¿Deseas comenzar/retomar la actividad? Solo deberías " +
                                        "hacerlo si el/la organizador/a ya te ha dado la salida")
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Comenzar", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        now_progressIndicator.setVisibility(View.VISIBLE);
                                        // 1) get location
                                        try {
                                            fusedLocationClient.getLastLocation()
                                                    .addOnSuccessListener(NowActivity2.this, new OnSuccessListener<Location>() {
                                                        @Override
                                                        public void onSuccess(Location location) {
                                                            if (location != null) {
                                                                // 2) check that we are close to the start spot or that we already started the activity
                                                                // (in such case, we don't have to check that we are at the start spot)
                                                                float diff = getDistance(location.getLatitude(), template.getStart_lat(),
                                                                        location.getLongitude(), template.getStart_lng());
                                                                if ((diff <= LOCATION_PRECISION
                                                                        && participation.getState() == ParticipationState.NOT_YET)
                                                                        || participation.getState() == ParticipationState.NOW) {
                                                                    // 3) if we are near enough, or if we had already started, continue to charge the map
                                                                    StorageReference reference = storageReference.child("maps/" + activity.getTemplate() + ".png");
                                                                    if (!LocationService.executing) {
                                                                        // now we have to do different things depending on whether the participation
                                                                        // is at NOT_YET or at NOW
                                                                        switch (participation.getState()) {
                                                                            case NOT_YET:
                                                                                // get current time
                                                                                long millis = System.currentTimeMillis();
                                                                                Date current_time = new Date(millis);
                                                                                // update the start time
                                                                                db.collection("activities").document(activity.getId())
                                                                                        .collection("participations").document(userID)
                                                                                        .update("state", ParticipationState.NOW,
                                                                                                "startTime", current_time)
                                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                            @Override
                                                                                            public void onSuccess(Void unused) {
                                                                                                now_progressIndicator.setVisibility(View.GONE);
                                                                                                // hide the button
                                                                                                nowParticipant_extendedFab.setEnabled(false);
                                                                                                nowParticipant_extendedFab.setVisibility(View.GONE);
                                                                                                // start service
                                                                                                locationServiceIntent.putExtra("activity", activity);
                                                                                                locationServiceIntent.putExtra("template", template);
                                                                                                startService(locationServiceIntent);
                                                                                                // enable see map button (just in case that the user wants
                                                                                                // to go back and forth between this and the map activity)
                                                                                                nowMap_button.setEnabled(true);
                                                                                                nowMap_button.setVisibility(View.VISIBLE);
                                                                                                // update UI
                                                                                                updateUIMap();
                                                                                            }
                                                                                        })
                                                                                        .addOnFailureListener(new OnFailureListener() {
                                                                                            @Override
                                                                                            public void onFailure(@NonNull @NotNull Exception e) {
                                                                                                now_progressIndicator.setVisibility(View.GONE);
                                                                                                showSnackBar("Error al comenzar la actividad. Inténtalo de nuevo.");
                                                                                            }
                                                                                        });
                                                                                break;
                                                                            case NOW:
                                                                                now_progressIndicator.setVisibility(View.GONE);
                                                                                // hide button
                                                                                nowParticipant_extendedFab.setEnabled(false);
                                                                                nowParticipant_extendedFab.setVisibility(View.GONE);
                                                                                // start service
                                                                                locationServiceIntent.putExtra("activity", activity);
                                                                                locationServiceIntent.putExtra("template", template);
                                                                                startService(locationServiceIntent);
                                                                                // update UI
                                                                                updateUIMap();
                                                                                break;
                                                                            default:
                                                                                now_progressIndicator.setVisibility(View.GONE);
                                                                                Toast.makeText(NowActivity2.this, "Parece que la actividad ya ha terminado", Toast.LENGTH_SHORT).show();
                                                                                break;
                                                                        }
                                                                    } else {
                                                                        now_progressIndicator.setVisibility(View.GONE);
                                                                        Toast.makeText(NowActivity2.this, "No se pudo iniciar la actividad... ya ha un servicio ejecutándose", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                } else {
                                                                    // too far from the start spot
                                                                    now_progressIndicator.setVisibility(View.GONE);
                                                                    int diff_meters = (int) diff;
                                                                    showSnackBar("Estás demasiado lejos de la salida (" + diff_meters + "). Acércate" +
                                                                            " a ella y vuelve a intentarlo");
                                                                }
                                                            } else {
                                                                now_progressIndicator.setVisibility(View.GONE);
                                                                Toast.makeText(NowActivity2.this, "Hubo algún problema al obtener la ubicación. Vuelve a intentarlo.", Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });
                                        } catch (SecurityException e) {
                                            showSnackBar("Parece que hay algún problema con los permisos de ubicación");
                                        }
                                    }
                                })
                                .show();
                    } else {
                        // if the activity has already finished
                        nowParticipant_extendedFab.setEnabled(false);
                        nowParticipant_extendedFab.setVisibility(View.GONE);
                        showSnackBar("Esta actividad ya ha terminado");
                    }
                } else {
                    // if we don't have the permissions we ask for them
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
                }
            }
        });

        // organizer FAB listener
        nowSeeParticipants_extendedFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (template != null && activity != null) {
                    if(mapDownloaded()) {
                        // if the map is already downloaded
                        updateUIParticipants();
                    } else {
                        // if the map is not yet downloaded
                        final ProgressDialog pd = new ProgressDialog(NowActivity2.this);
                        pd.setTitle("Cargando el mapa...");
                        pd.show();
                        StorageReference reference = storageReference.child("maps/" + activity.getTemplate() + ".png");
                        try {
                            // try to read the map image from Firebase into a file
                            File localFile = File.createTempFile("images", "png");
                            reference.getFile(localFile)
                                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            // we downloaded the map successfully
                                            // read the downloaded file into a bitmap
                                            Bitmap bmp = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                            // save the bitmap to a file
                                            ContextWrapper cw = new ContextWrapper(getApplicationContext());
                                            // path to /data/data/yourapp/app_data/imageDir
                                            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                                            // Create imageDir
                                            //File mypath = new File(directory, activity.getId() + ".png");
                                            File mypath = new File(directory, activity.getTemplate() + ".png");
                                            FileOutputStream fos = null;
                                            try {
                                                fos = new FileOutputStream(mypath);
                                                // Use the compress method on the BitMap object to write image to the OutputStream
                                                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                                updateUIParticipants();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                                            } finally {
                                                try {
                                                    fos.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            pd.dismiss();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            pd.dismiss();
                                            showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                                        }
                                    })
                                    .addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onProgress(@NonNull @NotNull FileDownloadTask.TaskSnapshot snapshot) {
                                            double progressPercent = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                                            if (progressPercent <= 90) {
                                                pd.setMessage("Progreso: " + (int) progressPercent + "%");
                                            } else {
                                                pd.setMessage("Descargado. Espera unos instantes mientras el mapa se guarda en el dispositivo");
                                            }
                                        }
                                    });
                        } catch (IOException e) {
                            pd.dismiss();
                            showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                        }
                    }
                } else {
                    Toast.makeText(NowActivity2.this, "No se pudo completar la acción. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // download map listener
        nowDownloadMap_extendedFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog pd = new ProgressDialog(NowActivity2.this);
                pd.setTitle("Cargando el mapa...");
                pd.show();
                StorageReference reference = storageReference.child("maps/" + activity.getTemplate() + ".png");
                try {
                    // try to read the map image from Firebase into a file
                    File localFile = File.createTempFile("images", "png");
                    reference.getFile(localFile)
                            .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                    // we downloaded the map successfully
                                    // read the downloaded file into a bitmap
                                    Bitmap bmp = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                    // save the bitmap to a file
                                    ContextWrapper cw = new ContextWrapper(getApplicationContext());
                                    // path to /data/data/yourapp/app_data/imageDir
                                    File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                                    // Create imageDir
                                    //File mypath = new File(directory, activity.getId() + ".png");
                                    File mypath = new File(directory, activity.getTemplate() + ".png");
                                    FileOutputStream fos = null;
                                    try {
                                        fos = new FileOutputStream(mypath);
                                        // Use the compress method on the BitMap object to write image to the OutputStream
                                        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                        showSnackBar("Mapa descargado con éxito, ya puedes comenzar la actividad");
                                        // disable the download map option and enable the others
                                        nowDownloadMap_extendedFab.setVisibility(View.GONE);
                                        nowDownloadMap_extendedFab.setEnabled(false);
                                        enableRightParticipantOptions();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                                    } finally {
                                        try {
                                            fos.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    pd.dismiss();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    pd.dismiss();
                                    showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                                }
                            })
                            .addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(@NonNull @NotNull FileDownloadTask.TaskSnapshot snapshot) {
                                    double progressPercent = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                                    if (progressPercent <= 90) {
                                        pd.setMessage("Progreso: " + (int) progressPercent + "%");
                                    } else {
                                        pd.setMessage("Descargado. Espera unos instantes mientras el mapa se guarda en el dispositivo");
                                    }
                                }
                            });
                } catch (IOException e) {
                    pd.dismiss();
                    showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                }
            }
        });

        // see map button listener
        nowMap_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(activity.getPlanner_id().equals(userID)) {
                    // if current user is the organizer
                    if(mapDownloaded()) {
                        // if the map is already downloaded
                        updateUIOrganizerMap();
                    } else {
                        // if the map is not yet downloaded
                        final ProgressDialog pd = new ProgressDialog(NowActivity2.this);
                        pd.setTitle("Cargando el mapa...");
                        pd.show();
                        StorageReference reference = storageReference.child("maps/" + activity.getTemplate() + ".png");
                        try {
                            // try to read the map image from Firebase into a file
                            File localFile = File.createTempFile("images", "png");
                            reference.getFile(localFile)
                                    .addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                            // we downloaded the map successfully
                                            // read the downloaded file into a bitmap
                                            Bitmap bmp = BitmapFactory.decodeFile(localFile.getAbsolutePath());
                                            // save the bitmap to a file
                                            ContextWrapper cw = new ContextWrapper(getApplicationContext());
                                            // path to /data/data/yourapp/app_data/imageDir
                                            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                                            // Create imageDir
                                            //File mypath = new File(directory, activity.getId() + ".png");
                                            File mypath = new File(directory, activity.getTemplate() + ".png");
                                            FileOutputStream fos = null;
                                            try {
                                                fos = new FileOutputStream(mypath);
                                                // Use the compress method on the BitMap object to write image to the OutputStream
                                                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                                updateUIOrganizerMap();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                                            } finally {
                                                try {
                                                    fos.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            pd.dismiss();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            pd.dismiss();
                                            showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                                        }
                                    })
                                    .addOnProgressListener(new OnProgressListener<FileDownloadTask.TaskSnapshot>() {
                                        @Override
                                        public void onProgress(@NonNull @NotNull FileDownloadTask.TaskSnapshot snapshot) {
                                            double progressPercent = (100.00 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                                            if (progressPercent <= 90) {
                                                pd.setMessage("Progreso: " + (int) progressPercent + "%");
                                            } else {
                                                pd.setMessage("Descargado. Espera unos instantes mientras el mapa se guarda en el dispositivo");
                                            }
                                        }
                                    });
                        } catch (IOException e) {
                            pd.dismiss();
                            showSnackBar("Algo salió mal al cargar el mapa. Sal y vuelve a intentarlo.");
                        }
                    }
                } else if(activity.getParticipants().contains(userID)) {
                    // if current user is a participant
                    switch (participation.getState()) {
                        case NOT_YET:
                            break;
                        case NOW:
                            if (!LocationService.executing) {
                                // if the service is not being executed now, show dialog to alert
                                new MaterialAlertDialogBuilder(NowActivity2.this)
                                        .setTitle("Aviso sobre el mapa")
                                        .setMessage("Esta acción te mostrará el mapa de la actividad, pero " +
                                                "el servicio que rastrea tu ubicación no está activo, por lo que" +
                                                " no se registrará tu paso por las balizas. Si lo que quieres es retomar " +
                                                "la actividad, cancela esta acción y pulsa el botón de Continuar")
                                        .setNegativeButton("Cancelar", null)
                                        .setPositiveButton("Ver mapa", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                if (mapDownloaded()) {
                                                    updateUIMap();
                                                } else {
                                                    // if for some reason the map is not downloaded then
                                                    // show the download button instead and warn the user
                                                    nowParticipant_extendedFab.setVisibility(View.GONE);
                                                    nowParticipant_extendedFab.setEnabled(false);
                                                    nowDownloadMap_extendedFab.setEnabled(true);
                                                    nowDownloadMap_extendedFab.setVisibility(View.VISIBLE);
                                                    showSnackBar("El mapa no está descargado. Descárgalo y vuelve a intentarlo");
                                                    nowMap_button.setVisibility(View.GONE);
                                                    nowMap_button.setEnabled(false);
                                                }
                                            }
                                        })
                                        .show();
                            } else {
                                // if the service is being executed now
                                if (mapDownloaded()) {
                                    updateUIMap();
                                } else {
                                    // if for some reason the map is not downloaded then
                                    // show the download button instead and warn the user
                                    nowParticipant_extendedFab.setVisibility(View.GONE);
                                    nowParticipant_extendedFab.setEnabled(false);
                                    nowDownloadMap_extendedFab.setEnabled(true);
                                    nowDownloadMap_extendedFab.setVisibility(View.VISIBLE);
                                    showSnackBar("El mapa no está descargado. Descárgalo y vuelve a intentarlo");
                                    nowMap_button.setVisibility(View.GONE);
                                    nowMap_button.setEnabled(false);
                                }
                            }
                            break;
                        case FINISHED:
                            if (mapDownloaded()) {
                                updateUIMap();
                            } else {
                                // if for some reason the map is not downloaded then
                                // show the download button instead and warn the user
                                nowParticipant_extendedFab.setVisibility(View.GONE);
                                nowParticipant_extendedFab.setEnabled(false);
                                nowDownloadMap_extendedFab.setEnabled(true);
                                nowDownloadMap_extendedFab.setVisibility(View.VISIBLE);
                                showSnackBar("El mapa no está descargado. Descárgalo y vuelve a intentarlo");
                                nowMap_button.setVisibility(View.GONE);
                                nowMap_button.setEnabled(false);
                            }
                            break;
                    }
                }
            }
        });

        // credentials button listener
        nowCredentials_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(NowActivity2.this)
                        .setTitle("Claves de acceso a la actividad")
                        .setMessage("Identificador: " + activity.getVisible_id() +
                                "\nContraseña: " + activity.getKey())
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void updateUIOrganizerMap() {
        Intent intent = new Intent(NowActivity2.this, OrganizerMapActivity.class);
        intent.putExtra("activity", activity);
        intent.putExtra("template", template);
        startActivity(intent);
    }

    private void updateUIParticipants() {
        Intent intent = new Intent(NowActivity2.this, VigilanciaActividadActivity.class);
        if(activity.getFinishTime().toInstant().isBefore(new Date().toInstant())){
            intent = new Intent(NowActivity2.this, ReviewActivity.class);
        }
        intent.putExtra("activity", activity);
        intent.putExtra("template", template);

        startActivity(intent);
    }

    private void updateUIMap() {
        Intent intent = new Intent(NowActivity2.this, MapParticipantActivity.class);
        intent.putExtra("template", template);
        intent.putExtra("activity", activity);
        startActivity(intent);
    }

    private void updateUIHome() {
        Intent intent = new Intent(NowActivity2.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateUIMyParticipation() {
        Intent intent = new Intent(NowActivity2.this, MyParticipationActivity.class);
        intent.putExtra("participation", participation);
        intent.putExtra("activity", activity);
        intent.putExtra("template", template);
        startActivity(intent);
    }

    private void showSnackBar(String message) {
        if (now_coordinatorLayout != null) {
            Snackbar.make(now_coordinatorLayout, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // Do nothing, just dismiss
                        }
                    })
                    .setDuration(8000)
                    .show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case FINE_LOCATION_ACCESS_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // user gave us the permission...
                    havePermissions = true;
                    Toast.makeText(this, "Ahora ya puedes usar toda la funcionalidad", Toast.LENGTH_SHORT).show();
                } else {
                    showSnackBar("Es necesario dar permiso para poder participar en la actividad");
                }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (activity != null && userID != null) {
            if (!activity.getPlanner_id().equals(userID)) {
                getMenuInflater().inflate(R.menu.now_overflow_menu, menu);
                // check if we have to enable the abandon activity option
                Date current_time = new Date(System.currentTimeMillis());
                if(!current_time.after(activity.getStartTime())
                        || !current_time.before(activity.getFinishTime())) {
                    menu.getItem(1).setEnabled(false);
                    menu.getItem(1).setVisible(false);
                } else {
                    menu.getItem(1).setEnabled(true);
                    menu.getItem(1).setVisible(true);
                }
            } else {
                getMenuInflater().inflate(R.menu.now_overflow_organizer_menu, menu);
            }
        } else {
            Toast.makeText(this, "Se produjo un error al carga las opciones del menú", Toast.LENGTH_SHORT).show();
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (activity != null && userID != null) {
            if (!activity.getPlanner_id().equals(userID)
                    && participation != null) {
                switch (item.getItemId()) {
                    case R.id.participation_activity:
                        updateUIMyParticipation();
                        break;
                    case R.id.quit_activity:
                        abandonActivity();
                        break;
                    default:
                        break;
                }
            } else if (activity.getPlanner_id().equals(userID)) {
                switch (item.getItemId()) {
                    case R.id.organizer_remove_activity:
                        removeActivity();
                        break;
                    default:
                        break;
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void abandonActivity() {
        new MaterialAlertDialogBuilder(NowActivity2.this)
                .setTitle("Abandonar actividad")
                .setMessage("¿Estás seguro/a de que quieres abandonar esta actividad en curso?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Date current_time = new Date(System.currentTimeMillis());
                        if(current_time.after(activity.getStartTime())
                                && current_time.before(activity.getFinishTime())
                                && participation != null) {
                            switch (participation.getState()) {
                                case NOT_YET:
                                    new MaterialAlertDialogBuilder(NowActivity2.this)
                                            .setTitle("La acción no se puede realizar")
                                            .setMessage("Tu participación aún no ha comenzado. Si no quieres " +
                                                    "tomar parte de la actividad, puedes desinscribirte en " +
                                                    "Mi Participación")
                                            .setPositiveButton("OK", null)
                                            .show();
                                    break;
                                case NOW:
                                    now_progressIndicator.setVisibility(View.VISIBLE);
                                    db.collection("activities").document(activity.getId())
                                            .collection("participations").document(userID)
                                            .update("state", ParticipationState.FINISHED,
                                                    "finishTime", current_time,
                                                    "completed", false)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    now_progressIndicator.setVisibility(View.GONE);
                                                    // once updated the document of the participation, check
                                                    // if we also need to finish the service
                                                    if(LocationService.executing) {
                                                        stopService(new Intent(NowActivity2.this, LocationService.class));
                                                    }
                                                    showSnackBar("Has abandonado la actividad.");
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull @NotNull Exception e) {
                                                    now_progressIndicator.setVisibility(View.GONE);
                                                    Toast.makeText(NowActivity2.this, "Algo salió mal al terminar la actividad. Vuelve a intentarlo.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                    break;
                                case FINISHED:
                                    Toast.makeText(NowActivity2.this, "La acción no se pudo completar" +
                                            " porque ya has terminado tu participación", Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        } else {
                            // the activity is not on going any more
                            Toast.makeText(NowActivity2.this, "La acción no se pudo completar. ", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private void removeActivity() {
        new MaterialAlertDialogBuilder(NowActivity2.this)
                .setTitle("Eliminar actividad")
                .setMessage("Se borrará la actividad y todos sus datos. Los participantes tampoco " +
                        "podrán acceder a ella. ¿Estás seguro/a de que quieres continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        now_progressIndicator.setVisibility(View.VISIBLE);
                        if (activity.getId() != null) {
                            db.collection("activities").document(activity.getId())
                                    .delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            now_progressIndicator.setVisibility(View.GONE);
                                            updateUIHome();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull @NotNull Exception e) {
                                            now_progressIndicator.setVisibility(View.GONE);
                                            Toast.makeText(NowActivity2.this, "Se produjo un error al eliminar la actividad", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(NowActivity2.this, "No se pudo eliminar la actividad. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private ActivityTime getActivityTime() {
        ActivityTime activityTime = ActivityTime.FUTURE;
        if (activity != null) {
            Date current_date = new Date(System.currentTimeMillis());
            if (activity.getStartTime().after(current_date)) {
                activityTime = ActivityTime.FUTURE;
            } else if (activity.getFinishTime().before(current_date)) {
                activityTime = ActivityTime.PAST;
            } else {
                activityTime = ActivityTime.ONGOING;
            }
        }
        return activityTime;
    }

    // reckons the distance between two points in meters
    private float getDistance(double lat1, double lat2, double lng1, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double p = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(p), Math.sqrt(1 - p));
        float dist = (float) (earthRadius * c);
        return dist;
    }

    private void enableRightParticipantOptions() {
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
                // allow to click the see map button
                nowMap_button.setEnabled(true);
                nowMap_button.setVisibility(View.VISIBLE);
                if (!LocationService.executing) {
                    if (activityTime == ActivityTime.ONGOING) {
                        nowParticipant_extendedFab.setEnabled(true);
                        nowParticipant_extendedFab.setVisibility(View.VISIBLE);
                        nowParticipant_extendedFab.setText("Continuar");
                    }
                }
                break;
            case FINISHED:
                nowState_textView.setText("Estado: terminada");
                // allow to click the see map button
                nowMap_button.setEnabled(true);
                nowMap_button.setVisibility(View.VISIBLE);
                nowParticipant_extendedFab.setEnabled(false);
                nowParticipant_extendedFab.setVisibility(View.GONE);
                break;
            default:
                break;
        }
    }

    public boolean mapDownloaded() {
        boolean res = false;
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        //File mypath = new File(directory, activity.getId() + ".png");
        File mypath = new File(directory, activity.getTemplate() + ".png");
        if (mypath.exists()) {
            res = true;
        }
        return res;
    }

    // only for testing
    /*private void deleteMap() {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        File mypath = new File(directory, activity.getId() + ".png");
        if (mypath.exists()) {
            mypath.delete();
        }
    }*/
}