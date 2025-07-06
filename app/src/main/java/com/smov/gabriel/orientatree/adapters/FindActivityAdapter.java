package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.model.Beacon;
import com.smov.gabriel.orientatree.model.Map;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.persistence.AppDatabase;
import com.smov.gabriel.orientatree.persistence.Converters;
import com.smov.gabriel.orientatree.persistence.OfflineMapUtils;
import com.smov.gabriel.orientatree.persistence.entities.OfflineActivity;
import com.smov.gabriel.orientatree.persistence.entities.OfflineMap;
import com.smov.gabriel.orientatree.persistence.entities.OfflineParticipation;
import com.tfg.marllor.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class FindActivityAdapter extends RecyclerView.Adapter<FindActivityAdapter.MyViewHolder> {

    private Context context;

    private ArrayList<Activity> activities;
    private FirebaseStorage firebaseStorage;
    private int position;
    private AppDatabase localDb;

    public FindActivityAdapter(Context context, ArrayList<Activity> activities) {
        this.context = context;
        this.activities = activities;
        this.localDb = AppDatabase.getDatabase(context);
    }

    @NonNull
    @Override
    public FindActivityAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_find_activity, parent, false);
        return new FindActivityAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FindActivityAdapter.MyViewHolder holder, int position) {
        this.position = position ;
        Activity activity = activities.get(position);
        firebaseStorage = FirebaseStorage.getInstance();
        // formatting date in order to display it on card
        String pattern = "dd/MM/yyyy";
        DateFormat df = new SimpleDateFormat(pattern);
        Date date = activity.getStartTime();
        String dateAsString = df.format(date);

        // display title and date
        holder.find_title_textView.setText(activity.getTitle());
        holder.find_date_textView.setText("Fecha: " + dateAsString);

        // check that the current user is not the organizer of the activity
        if(!activity.getPlanner_id().equals(holder.userID)) {
            // if he/she is not the organizer:
            // get the activity's participants
            ArrayList<String> participants = activity.getParticipants();
            if(participants != null) {
                if(participants.contains(holder.userID)) {
                    // if current user is a participant
                    holder.subscribe_button.setText("Inscrito/a");
                    holder.subscribe_button.setEnabled(false);
                } else {
                    // if current user is not a participant
                    holder.subscribe_button.setText("Inscribirme");
                    holder.subscribe_button.setEnabled(true);
                }
            }
            // show the subscribe button (only if the current user is not the organizer of the activity)
            holder.subscribe_button.setVisibility(View.VISIBLE);
            holder.findActivity_separator.setVisibility(View.VISIBLE);
        }
        // if the current user is the organizer, no button will be displayed since it just remains "gone"

        // get and set the activity picture
        StorageReference ref = holder.storageReference.child("templateImages/" + activity.getTemplate() + ".jpg");
        Glide.with(context)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // prevent caching
                .skipMemoryCache(true) // prevent caching
                .into(holder.find_row_imageView);

        holder.subscribe_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkKeyDialog(activity, holder);
            }
        });
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        // Firebase services
        FirebaseStorage storage;
        StorageReference storageReference;
        FirebaseAuth mAuth;
        FirebaseFirestore db;

        // useful IDs
        String userID;

        // UI elements
        TextView find_title_textView, find_date_textView;
        ImageView find_row_imageView;
        Button subscribe_button;
        View findActivity_separator;
        CircularProgressIndicator progressIndicator;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            // binding UI elements
            find_title_textView = itemView.findViewById(R.id.find_row_title_textView);
            find_date_textView = itemView.findViewById(R.id.find_row_date_textView);
            find_row_imageView = itemView.findViewById(R.id.find_row_imageView);
            subscribe_button = itemView.findViewById(R.id.subscribe_button);
            findActivity_separator = itemView.findViewById(R.id.finActivity_separator);
            progressIndicator = itemView.findViewById(R.id.findActivity_progressIndicator);
            FirebaseFirestoreSettings persistencia = new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(true).build();
            db = FirebaseFirestore.getInstance();
            db.setFirestoreSettings(persistencia);
            mAuth = FirebaseAuth.getInstance();
            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();

            // getting IDs
            userID = mAuth.getCurrentUser().getUid();
        }
    }

    private void checkKeyDialog(Activity activity, @NonNull FindActivityAdapter.MyViewHolder holder) {
        final EditText input = new EditText(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        new MaterialAlertDialogBuilder(context)
                .setTitle("Introduzca la clave de acceso (4 caracteres)")
                .setView(input)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String input_key = input.getText().toString().trim();
                        if(input_key.equals(activity.getKey())) {
                            processSubscription(activity, holder);
                        } else {
                            new MaterialAlertDialogBuilder(context)
                                    .setTitle("Clave incorrecta")
                                    .setMessage("La clave introducida para esa actividad es incorrecta")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
    private void saveActivityLocally(Activity activity) {
        OfflineActivity entity = new OfflineActivity();
        entity.id = activity.getId();
        entity.title = activity.getTitle();
        entity.plannerId = activity.getPlanner_id();
        entity.template = activity.getTemplate();
        entity.key = activity.getKey();
        entity.startTime = activity.getStartTime().getTime();
        entity.finishTime = activity.getFinishTime().getTime();
        entity.participants = activity.getParticipants();
        entity.imageUrl = "templateImages/" + activity.getTemplate() + ".jpg";
        entity.location_help=activity.isLocation_help();
        new Thread(() -> {
            localDb.activityDao().insertActivity(entity);
        }).start();
    }

    private void processSubscription(Activity activity, MyViewHolder holder){
        activity.addParticipant(holder.userID);
        holder.progressIndicator.setVisibility(View.VISIBLE);
        holder.db.collection("activities").document(activity.getId())
                .set(activity)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Participation participation = new Participation(holder.userID);
                        holder.db.collection("activities").document(activity.getId())
                                .collection("participations").document(holder.userID)
                                .set(participation)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        downloadAndSaveRelatedData(activity, holder);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull @NotNull Exception e) {
                                        holder.progressIndicator.setVisibility(View.GONE);
                                        Toast.makeText(context, "La inscripción no pudo completarse. Vuelve a intentarlo", Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                });
    }
    private void downloadAndSaveRelatedData(Activity activity, MyViewHolder holder) {
        // 1. Guardar la actividad localmente
        saveActivityLocally(activity);

        // 2. Descargar y guardar el template
        holder.db.collection("templates").document(activity.getTemplate())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Template template = documentSnapshot.toObject(Template.class);
                        saveTemplateLocally(template);
                        // 3. Descargar y guardar las balizas del template
                        downloadAndSaveBeacons(activity.getTemplate(), holder);
                        saveParticipationLocally(activity.getId(), holder.userID);
                        saveActivityImageLocally(template.getTemplate_id(), holder.storageReference);
                        saveMapImageLocally(activity,this.context);
                        saveTemplateImagesLocally(template,this.context);
                        downloadAndSaveMap(template.getMap_id(), holder);
                    }
                });

        // 3. Actualizar UI
        holder.progressIndicator.setVisibility(View.GONE);
        holder.subscribe_button.setText("Inscrito/a");
        holder.subscribe_button.setEnabled(false);
        Toast.makeText(context, "Inscripción completada y datos descargados", Toast.LENGTH_LONG).show();
    }
    private void downloadAndSaveBeacons(String templateId, MyViewHolder holder) {
        holder.db.collection("templates").document(templateId)
                .collection("beacons")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    new Thread(() -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Beacon beacon = document.toObject(Beacon.class);
                            saveBeaconLocally(beacon);
                        }
                    }).start();
                });
    }
    public void downloadAndSaveMap(String mapId, MyViewHolder holder) {

            // 2. Descargar datos del mapa desde Firestore
            holder.db.collection("maps").document(mapId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        new Thread(() -> {
                        if (documentSnapshot.exists()) {
                            // 3. Convertir y guardar en Room
                            Map map = documentSnapshot.toObject(Map.class);
                            saveMapLocally(map);
                            downloadMapForOfflineUse(map, mapId);
                        }
                        }).start();
                    })
                    .addOnFailureListener(e -> {
                        Log.v("Error al descargar el mapa: " , e.getMessage());
                    });
    }
    private void saveMapLocally(Map map) {
        new Thread(() -> {

                localDb.mapDao().insertMap(Converters.toEntity(map));
        }).start();
    }
    private void saveTemplateLocally(Template template) {
        new Thread(() -> {
            // Convierte y guarda el template usando TemplateDao
            localDb.templateDao().insertTemplate(Converters.toEntity(template));
        }).start();
    }

    private void saveBeaconLocally(Beacon beacon) {
        new Thread(() -> {
            // Convierte y guarda la baliza usando BeaconDao
            localDb.beaconDao().insertBeacon(Converters.toEntity(beacon));
        }).start();
    }
    private void saveParticipationLocally(String activityId, String userId) {
        new Thread(() -> {
            OfflineParticipation entity = new OfflineParticipation();
            entity.participant=userId;
            entity.state = ParticipationState.NOT_YET;
            entity.reaches = new ArrayList<>();
            entity.activityId = activityId;
            localDb.participationDao().insertParticipation(entity);
        }).start();
    }
    private void downloadMapForOfflineUse(Map map, String MapId) {
        if (map == null || MapId == null) return;

        LatLngBounds bounds = new LatLngBounds(
                new LatLng(map.getOverlay_corners().get(0).getLatitude(),
                        map.getOverlay_corners().get(0).getLongitude()),       // South west corner
                new LatLng(map.getOverlay_corners().get(1).getLatitude(),
                        map.getOverlay_corners().get(1).getLongitude()));
        OfflineMapUtils.downloadMapArea(this.context, bounds, MapId);

    }

    private void saveMapImageLocally(Activity activity, Context context) {
        if(!mapDownloaded(context,activity)){
        new Thread(() -> {
            StorageReference storageReference = firebaseStorage.getReference();
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
                                ContextWrapper cw = new ContextWrapper(context);
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
                                } finally {
                                    try {
                                        fos.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });

            } catch (IOException e) {
                Log.v("Error", e.toString());
            }
        }).start();
    }
    }

    private void saveTemplateImagesLocally(Template template, Context context) {
        if(!beaconsDownloaded(context,template)) {
            new Thread(() -> {
                StorageReference storageReference = firebaseStorage.getReference();
                ArrayList<String> beacons = template.getBeacons();
                for (int i = 0; i < beacons.size(); i++) {
                    int indexBeacons = i;
                    StorageReference reference = storageReference.child("challengeImages/" + template.getTemplate_id() + beacons.get(indexBeacons) + ".jpg");
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
                                        ContextWrapper cw = new ContextWrapper(context);
                                        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                                        // Create imageDir
                                        //File mypath = new File(directory, activity.getId() + ".png");
                                        File mypath = new File(directory, template.getTemplate_id() + beacons.get(indexBeacons) + ".jpg");
                                        FileOutputStream fos = null;
                                        try {
                                            fos = new FileOutputStream(mypath);
                                            // Use the compress method on the BitMap object to write image to the OutputStream
                                            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        } finally {
                                            try {
                                                fos.close();
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                });

                    } catch (IOException e) {
                        Log.v("Error", e.toString());
                    }
                }
            }).start();
        }
    }

    private void saveActivityImageLocally(String templateId, StorageReference storageRef) {
        // Descargar la imagen y guardarla en almacenamiento local
        File localFile = new File(context.getFilesDir(), "template_" + templateId + ".jpg");

        storageRef.child("templateImages/" + templateId + ".jpg")
                .getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d("Download", "Imagen descargada correctamente");
                })
                .addOnFailureListener(e -> {
                    Log.e("Download", "Error al descargar imagen", e);
                });
    }
    public boolean mapDownloaded(Context context,Activity activity) {
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        //File mypath = new File(directory, activity.getId() + ".png");
        File mypath = new File(directory, activity.getTemplate() + ".png");
        return mypath.exists();
    }
    public boolean beaconsDownloaded(Context context,Template template) {
        ArrayList<String> beacons = template.getBeacons();
        ContextWrapper cw = new ContextWrapper(context);
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        for(int i = 0; i < beacons.size(); i++) {
            //File mypath = new File(directory, activity.getId() + ".png");
            File mypath = new File(directory, template.getTemplate_id()+beacons.get(i) + ".jpg");
            if (!mypath.exists()){
                Log.v("beacons","No existe");
                return false;
            }
        }
        Log.v("beacons","existen");
        return true;
    }

}
