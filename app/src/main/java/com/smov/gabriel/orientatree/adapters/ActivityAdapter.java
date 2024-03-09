package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.content.Intent;
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
import com.smov.gabriel.orientatree.ui.NowActivity;
import com.tfg.marllor.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Template;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.MyViewHolder> {

    private Context context;
    private android.app.Activity homeActivity;

    private ArrayList<Activity> activities;
    private int position;

    public ActivityAdapter(android.app.Activity homeActivity, Context context, ArrayList<Activity> activities) {
        this.homeActivity = homeActivity;
        this.context = context;
        this.activities = activities;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_activity, parent, false);
        return new MyViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        this.position = position;
        Activity activity = activities.get(position);

        String planner_id = activity.getPlanner_id();
        String user_id = holder.mAuth.getCurrentUser().getUid();

        // formatting date in order to display it on card
        String pattern = "dd/MM/yyyy";
        DateFormat df = new SimpleDateFormat(pattern);
        Date date = activity.getStartTime();
        String dateAsString = df.format(date);

        holder.title_textView.setText(activity.getTitle());
        holder.date_textView.setText("Fecha: " + dateAsString);

        if (activity.getPlanner_id().equals(user_id)) {
            holder.role_textView.setText("Organizador/a");
        } else if (activity.getParticipants() != null) {
            if (activity.getParticipants().contains(user_id)) {
                holder.role_textView.setText("Participante");
            }
        } else {
            holder.role_textView.setText("");
        }

        holder.row_activity_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUINowActivity(activity);
            }
        });

        holder.db.collection("templates").document(activity.getTemplate())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Template template = documentSnapshot.toObject(Template.class);
                        holder.template_textView.setText(template.getName());
                    }
                });

        // get and set the activity picture
        StorageReference ref = holder.storageReference.child("templateImages/" + activity.getTemplate() + ".jpg");
        Glide.with(context)
                .load(ref)
                .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                .skipMemoryCache(true) // prevent caching
                .into(holder.rowImage_imageView);
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
