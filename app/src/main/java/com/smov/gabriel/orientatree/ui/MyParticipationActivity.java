package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.gson.Gson;
import com.tfg.marllor.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.model.Template;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MyParticipationActivity extends AppCompatActivity {

    // UI elements
    private Toolbar toolbar;
    private TextView myParticipationStart_textView, myParticipationFinish_textView,
            myParticipationTotal_textView, myParticipationBeacons_textView,
            myParticipationCompleted_textView;
    private MaterialButton myParticipationBeacons_button, myParticipationTrack_button,
            myParticipationInscription_button, myParticipationDelete_button;
    private CircularProgressIndicator myParticipation_progressIndicator;

    // model objects
    private Participation participation;
    private Activity activity;
    private ArrayList<BeaconReached> reaches;
    private Template template;

    // useful IDs
    private String userID;
    private String activityID;

    // Firebase services
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    // to format the way hours are displayed
    private static String pattern_hour = "HH:mm:ss";
    private static DateFormat df_hour = new SimpleDateFormat(pattern_hour);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("Hola",this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_participation);

        // get the intent
        Intent intent = getIntent();
        String json = getIntent().getStringExtra("participation");
        participation = new Gson().fromJson(json, Participation.class);
        activity = (Activity) intent.getSerializableExtra("activity");
        template = (Template) intent.getSerializableExtra("template"); 

        // binding UI elements
        toolbar = findViewById(R.id.myParticipation_toolbar);
        myParticipationStart_textView = findViewById(R.id.myParticipationStart_textView);
        myParticipationFinish_textView = findViewById(R.id.myParticipationFinish_textView);
        myParticipationTotal_textView = findViewById(R.id.myParticipationTotal_textView);
        myParticipationBeacons_textView = findViewById(R.id.myParticipationBeacons_textView);
        myParticipationTrack_button = findViewById(R.id.myParticipationTrack_button);
        myParticipationBeacons_button = findViewById(R.id.myParticipationBeacons_button);
        myParticipationDelete_button = findViewById(R.id.myParticipationDelete_button);
        myParticipationInscription_button = findViewById(R.id.myParticipationInscription_button);
        myParticipation_progressIndicator = findViewById(R.id.myParticipation_progressIndicator);
        myParticipationCompleted_textView = findViewById(R.id.myParticipationCompleted_textView);

        // initialize Firebase services
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        // setting useful IDs
        userID = mAuth.getCurrentUser().getUid();
        activityID = activity.getId();

        // setting the AppBar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // setting the info
        // check that we received properly the participation and the activity
        if(participation != null && activityID != null
                && template != null) {
            // check if it has already started
            if(participation.getState() != ParticipationState.NOT_YET) {
                Date start_time = participation.getStartTime();
                Date finish_time = participation.getFinishTime();
                if(start_time != null) {
                    myParticipationStart_textView.setText(df_hour.format(start_time));
                } else {
                    myParticipationStart_textView.setText("Nada que mostrar");
                }
                if(finish_time != null) {
                    myParticipationFinish_textView.setText(df_hour.format(finish_time));
                } else {
                    myParticipationFinish_textView.setText("Nada que mostrar");
                }
                if((start_time != null && finish_time != null) 
                        && start_time.before(finish_time)) {
                    long diff_millis = Math.abs(finish_time.getTime() - start_time.getTime());
                    myParticipationTotal_textView.setText(formatMillis(diff_millis));
                } else {
                    myParticipationTotal_textView.setText("Nada que mostrar");
                }
                if(participation.isCompleted()) {
                    myParticipationCompleted_textView.setText("Sí");
                } else {
                    myParticipationCompleted_textView.setText("No");
                }
                // get the reaches
                reaches = new ArrayList<>();
                db.collection("activities").document(activityID)
                        .collection("participations").document(userID)
                        .collection("beaconReaches")
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                                for(QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                    BeaconReached reach = documentSnapshot.toObject(BeaconReached.class);
                                    reaches.add(reach);
                                }
                                // set the number of beacons reached and the total number of beacons
                                if(template.getBeacons() != null) {
                                    int num_reaches = reaches.size();
                                    int number_of_beacons = template.getBeacons().size();
                                    myParticipationBeacons_textView.setText(num_reaches + "/" + number_of_beacons);
                                } else {
                                    Toast.makeText(MyParticipationActivity.this, "No se pudo recuperar la información de las balizas alcanzadas", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull @NotNull Exception e) {
                                Toast.makeText(MyParticipationActivity.this, "No se pudo recuperar la información de las balizas", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                // if the participation has not yet started
                // all fields empty
                myParticipationStart_textView.setText("Nada que mostrar");
                myParticipationFinish_textView.setText("Nada que mostrar");
                myParticipationTotal_textView.setText("Nada que mostrar");
                myParticipationBeacons_textView.setText("Nada que mostrar");
                // check if we should enable the button to cancel the inscription
                if(inscriptionCancelable()) {
                    myParticipationInscription_button.setEnabled(true);
                }
            }
            // check if we should allow the user to see the track
            if(participation.getState() == ParticipationState.FINISHED
                    || participation.getFinishTime() != null
                    || (activity.getFinishTime().before(new Date(System.currentTimeMillis()))
                            && participation.getStartTime() != null)) {
                // if the participation is finished or if there is a finish time or if the activity has finished
                // we enable the track button
                myParticipationTrack_button.setEnabled(true);
            } else {
                myParticipationTrack_button.setEnabled(false);
            }
        } else {
            // if we couldn't receive right the participation
            Toast.makeText(this, "Ocurrió un error al leer la información. Salga e inténtelo de nuevo", Toast.LENGTH_SHORT).show();
        }

        // beacons listener
        myParticipationBeacons_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUIReaches();
            }
        });

        // track listener
        myParticipationTrack_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(activity != null && participation != null) {
                    if(mapDownloaded()) {
                        // if we already have the map downloaded
                        updateUITrackMap();
                    } else {
                        // if we don't have the map downloaded
                        final ProgressDialog pd = new ProgressDialog(MyParticipationActivity.this);
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
                                            File mypath = new File(directory, activity.getTemplate() + ".png");
                                            FileOutputStream fos = null;
                                            try {
                                                fos = new FileOutputStream(mypath);
                                                // Use the compress method on the BitMap object to write image to the OutputStream
                                                bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                Toast.makeText(MyParticipationActivity.this, "Algo salió mal al descargar el mapa", Toast.LENGTH_SHORT).show();
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
                                            Toast.makeText(MyParticipationActivity.this, "Algo salió mal al descargar el mapa", Toast.LENGTH_SHORT).show();
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

        // here the second action should be performed by a cloud functions
        myParticipationInscription_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check that it is possible to cancel the inscription
                if(inscriptionCancelable()) {
                    new MaterialAlertDialogBuilder(MyParticipationActivity.this)
                            .setTitle("Eliminar mi inscripción")
                            .setTitle("¿Estás seguro/a de que quieres desinscribirte" +
                                    " de esta actividad?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Sí", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    myParticipation_progressIndicator.setVisibility(View.VISIBLE);
                                    ArrayList<String> participants = activity.getParticipants();
                                    participants.remove(userID);
                                    // update the list with the participants in the activity
                                    db.collection("activities").document(activityID)
                                            .update("participants", participants)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    // updated the list, now
                                                    // remove the participation document
                                                    db.collection("activities").document(activityID)
                                                            .collection("participations").document(userID)
                                                            .delete()
                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void unused) {
                                                                    myParticipation_progressIndicator.setVisibility(View.GONE);
                                                                    updateUIHome();
                                                                }
                                                            })
                                                            .addOnFailureListener(new OnFailureListener() {
                                                                @Override
                                                                public void onFailure(@NonNull @NotNull Exception e) {
                                                                    // we couldn't remove the participation object
                                                                    // at this point we updated the participants list but did not remove the participation object
                                                                    // on the eyes of the user there would be no difference, the only problem is that the information
                                                                    // still occupies space in the database
                                                                    myParticipation_progressIndicator.setVisibility(View.GONE);
                                                                    updateUIHome();
                                                                }
                                                            });
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull @NotNull Exception e) {
                                                    // we couldn't update the activity
                                                    myParticipation_progressIndicator.setVisibility(View.GONE);
                                                    Toast.makeText(MyParticipationActivity.this, "Algo falló al eliminar su subscripción, vuelva a intentarlo", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            })
                            .show();
                } else {
                    // if not cancelable any more
                    myParticipationInscription_button.setEnabled(false);
                    Toast.makeText(MyParticipationActivity.this, "No se pudo completar la acción." +
                            " La actividad está en curso o ya has comenzado tu participación", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void updateUITrackMap() {
        Intent intent = new Intent(MyParticipationActivity.this, TrackActivity.class);
        intent.putExtra("template", template);
        intent.putExtra("activity", activity);
        ArrayList<String> participante = new ArrayList<>();
        participante.add(participation.getParticipant());
        intent.putExtra("participantes",participante);
        ArrayList<String> nombre = new ArrayList<>();
        intent.putExtra("nombres", nombre);
        startActivity(intent);
    }

    private void updateUIReaches() {
        if(template != null && activity != null && userID != null
            && (userID.equals(participation.getParticipant()))) {
            Intent intent = new Intent(MyParticipationActivity.this, ReachesActivity.class);
            intent.putExtra("activity", activity);
            intent.putExtra("template", template);
            intent.putExtra("participantID", userID);
            startActivity(intent);
        }
    }
    
    private void updateUIHome() {
        Intent intent = new Intent(MyParticipationActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private String formatMillis (long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        String time = hours % 24 + "h " + minutes % 60 + "m " + seconds % 60 + "s";
        return time;
    }

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
        File mypath = new File(directory, template.getTemplate_id() + ".png");
        if (mypath.exists()) {
            res = true;
        }
        return res;
    }

    // allows us to know whether is possible to cancel a subscription in an activity or not
    // given that the participation must have not been yet started and that the
    // activity must be in the future
    private boolean inscriptionCancelable() {
        boolean res = false;
        // first we check that neither the activity nor the participation are null
        if(activity == null || participation == null) {
            // if any of the are null, we return false
            return res;
        } else {
            if(participation.getState() == ParticipationState.NOT_YET) {
                res = true;
               /*Date current_time = new Date(System.currentTimeMillis());
               if(current_time.before(activity.getStartTime())) {
                   // if the participation has NOT_YET started
                   // and current time is before the activity starts
                   // it should be possible to cancel the inscription
                   res = true;
               }*/
            }
        }
        return res;
    }
}