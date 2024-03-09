package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.tfg.marllor.orientatree.R;
import com.smov.gabriel.orientatree.adapters.ReachAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.Template;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class ReachesActivity extends AppCompatActivity {

    private Toolbar toolbar;

    private RecyclerView reaches_recyclerView;
    private ArrayList<BeaconReached> reaches;
    private ReachAdapter reachAdapter;
    private ConstraintLayout emptyState_layout;
    private TextView emptyStateMessage_textView;
    private ExtendedFloatingActionButton reachesTrack_fab;

    private Activity activity;
    private Template template;

    private ReachesActivity reachesActivity;

    // useful ID strings
    private String activityID;
    private String userID; // currently logged user's ID
    private String templateID;
    private String participantID; // ID received within the intent
    // (only used if we are the organizer trying to see certain participant's information)

    // Firebase services
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reaches);

        // initialize Firebase services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        reachesActivity = this;

        reaches_recyclerView = findViewById(R.id.reaches_recyclerView);
        emptyState_layout = findViewById(R.id.peacockHead_emptyState);
        emptyStateMessage_textView = findViewById(R.id.emptyStateMessage_textView);
        reachesTrack_fab = findViewById(R.id.reachesTrack_fab);

        // set the AppBar
        toolbar = findViewById(R.id.reaches_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // get from the intent the activity
        Intent intent = getIntent();
        activity = (Activity) intent.getSerializableExtra("activity");
        template = (Template) intent.getSerializableExtra("template");
        participantID = intent.getExtras().getString("participantID");

        // get current user id
        userID = mAuth.getCurrentUser().getUid();

        if(activity != null && template != null) {
            activityID = activity.getId();
            templateID = activity.getTemplate();
            String participant_searched;
            if(activity.getPlanner_id().equals(userID)) {
                // if we are the organizer
                if(participantID != null) {
                    // we should have received from the intent the participant ID
                    participant_searched = participantID;
                    if(activity.getStartTime().before(new Date(System.currentTimeMillis()))) {
                        reachesTrack_fab.setEnabled(true);
                        reachesTrack_fab.setVisibility(View.VISIBLE);
                    }
                } else {
                    // if we haven't, finish and tell the user
                    Toast.makeText(reachesActivity, "Algo salió mal al mostrar las balizas", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // if we are not the organizer, then we are a user trying to watch its own beacons
                participant_searched = userID;
            }
            // get the participant's reaches with realtime updates
            db.collection("activities").document(activityID)
                    .collection("participations").document(participant_searched)
                    .collection("beaconReaches")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                return;
                            }
                            reaches = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : value) {
                                BeaconReached reach = doc.toObject(BeaconReached.class);
                                reaches.add(reach);
                            }
                            // show or hide the empty state with its message
                            if(reaches.size() < 1) {
                                emptyStateMessage_textView.setText("No hay balizas alcanzadas");
                                emptyState_layout.setVisibility(View.VISIBLE);
                            } else {
                                emptyStateMessage_textView.setText("");
                                emptyState_layout.setVisibility(View.GONE);
                            }
                            Collections.sort(reaches, new BeaconReached());
                            reachAdapter = new ReachAdapter(reachesActivity, ReachesActivity.this, reaches,
                                    templateID, activity, template, participant_searched);
                            reaches_recyclerView.setAdapter(reachAdapter);
                            reaches_recyclerView.setLayoutManager(new LinearLayoutManager(ReachesActivity.this));
                        }
                    });
        } else {
            Toast.makeText(this, "Algo salió mal al cargar los datos", Toast.LENGTH_SHORT).show();
            finish();
        }

        reachesTrack_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(activity != null) {
                    if(mapDownloaded()) {
                        // if we already have the map downloaded
                        updateUITrackMap();
                    } else {
                        // if we don't have the map downloaded
                        final ProgressDialog pd = new ProgressDialog(ReachesActivity.this);
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
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                Toast.makeText(ReachesActivity.this, "Algo salió mal al descargar el mapa", Toast.LENGTH_SHORT).show();
                                            } finally {
                                                try {
                                                    fos.close();
                                                } catch (IOException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            pd.dismiss();
                                            if(mapDownloaded()) {
                                                updateUITrackMap();
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            pd.dismiss();
                                            Toast.makeText(ReachesActivity.this, "Algo salió mal al descargar el mapa", Toast.LENGTH_SHORT).show();
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
                        }
                    }
                }
            }
        });

    }

    private void updateUITrackMap() {
        Intent intent = new Intent(ReachesActivity.this, TrackActivity.class);
        intent.putExtra("template", template);
        intent.putExtra("activity", activity);
        intent.putExtra("participantID", participantID);
        startActivity(intent);
    }

    // allow to go back when pressing the AppBar back arrow
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
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
}