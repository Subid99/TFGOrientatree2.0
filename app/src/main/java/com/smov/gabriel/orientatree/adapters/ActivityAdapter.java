package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.persistence.AppDatabase;
import com.smov.gabriel.orientatree.persistence.Converters;
import com.smov.gabriel.orientatree.persistence.entities.OfflineTemplate;
import com.smov.gabriel.orientatree.ui.NowActivity;
import com.tfg.marllor.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Template;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.MyViewHolder> {

    private Context context;
    private android.app.Activity homeActivity;

    private ArrayList<Activity> activities;
    private int position;
    private AppDatabase localDb;
    public ActivityAdapter(android.app.Activity homeActivity, Context context, ArrayList<Activity> activities) {
        this.homeActivity = homeActivity;
        this.context = context;
        this.activities = activities;
        localDb = AppDatabase.getDatabase(context);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_activity, parent, false);
        localDb = AppDatabase.getDatabase(context);
        return new MyViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Activity activity = activities.get(position);
        String user_id = holder.mAuth.getCurrentUser() != null ?
                holder.mAuth.getCurrentUser().getUid() : "";

        // Formatear fecha
        String pattern = "dd/MM/yyyy";
        DateFormat df = new SimpleDateFormat(pattern);
        String dateAsString = df.format(activity.getStartTime());

        // Configurar vistas básicas
        holder.title_textView.setText(activity.getTitle());
        holder.date_textView.setText("Fecha: " + dateAsString);

        // Determinar rol del usuario
        if (activity.getPlanner_id().equals(user_id)) {
            holder.role_textView.setText("Organizador/a");
        } else if (activity.getParticipants() != null &&
                activity.getParticipants().contains(user_id)) {
            holder.role_textView.setText("Participante");
        } else {
            holder.role_textView.setText("");
        }

        // Configurar click listener
        holder.row_activity_layout.setOnClickListener(v -> {
            updateUINowActivity(activity);
        });

        // Obtener nombre del template (versión offline/online)
        loadTemplateName(holder, activity.getTemplate());

        // Cargar imagen (versión offline/online)
        loadActivityImage(holder, activity.getTemplate());
    }

    private void loadTemplateName(MyViewHolder holder, String templateId) {
        // Primero intentar desde local
        new Thread(() -> {
            OfflineTemplate Offlinetemplate = localDb.templateDao().getTemplateById(templateId);
            if (Offlinetemplate != null) {
                holder.itemView.post(() -> {
                    holder.template_textView.setText(Offlinetemplate.name);
                });
            } else {
                // Si no está localmente, intentar desde Firestore (si hay conexión)
                if (isOnline()) {
                    holder.db.collection("templates").document(templateId)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    Template template = documentSnapshot.toObject(Template.class);
                                    holder.template_textView.setText(template.getName());
                                    // Guardar localmente para próximas veces
                                    saveTemplateLocally(template);
                                }
                            });
                }
            }
        }).start();
    }

    private void loadActivityImage(MyViewHolder holder, String templateId) {
        // Primero intentar cargar imagen local
        File localFile = new File(context.getFilesDir(), "template_" + templateId + ".jpg");

        if (localFile.exists()) {
            Log.v("ImageDownload", "ExisteImage"+localFile.getPath());
            Glide.with(context)
                    .load(localFile)
                    .into(holder.rowImage_imageView);
        } else {
            // Si no existe localmente, cargar desde Firebase Storage (si hay conexión)
            if (isOnline()) {
                StorageReference ref = holder.storageReference.child("templateImages/" + templateId + ".jpg");
                Glide.with(context)
                        .load(ref)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(holder.rowImage_imageView);

                // Descargar imagen para almacenamiento local
                downloadImageForOfflineUse(ref, templateId);
            } else {
                // Mostrar placeholder si no hay conexión ni imagen local
                Glide.with(context)
                        .load(R.drawable.ic_peacock)
                        .into(holder.rowImage_imageView);
            }
        }
    }

    private void downloadImageForOfflineUse(StorageReference ref, String templateId) {
        File localFile = new File(context.getFilesDir(), "template_" + templateId + ".jpg");
        ref.getFile(localFile).addOnSuccessListener(taskSnapshot -> {
            Log.d("ImageDownload", "Imagen guardada localmente: " + localFile.getPath());
        }).addOnFailureListener(e -> {
            Log.e("ImageDownload", "Error al guardar imagen local", e);
        });
    }

    private void saveTemplateLocally(Template template) {
        new Thread(() -> {
            localDb.templateDao().insertTemplate(Converters.toEntity(template));
        }).start();
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        FirebaseStorage storage;
        StorageReference storageReference;

        FirebaseAuth mAuth;

        FirebaseFirestore db;

        LinearLayout row_activity_layout;
        TextView title_textView, date_textView, template_textView, role_textView;
        ImageView rowImage_imageView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            title_textView = itemView.findViewById(R.id.title_textView);
            date_textView = itemView.findViewById(R.id.date_textView);
            template_textView = itemView.findViewById(R.id.template_textView);
            role_textView = itemView.findViewById(R.id.role_textView);
            row_activity_layout = itemView.findViewById(R.id.row_activity_layout);
            rowImage_imageView = itemView.findViewById(R.id.rowImage_imageView);

            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();

            db = FirebaseFirestore.getInstance();

            mAuth = FirebaseAuth.getInstance();
        }
    }

    private void updateUINowActivity(Activity activity) {
        Intent intent = new Intent(context, NowActivity.class);
        intent.putExtra("activity", activity);
        homeActivity.startActivityForResult(intent, 1); // this is to allow us to come back from the activity
    }
}
