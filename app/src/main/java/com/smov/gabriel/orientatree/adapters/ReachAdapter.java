package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.ui.ChallengeActivity;
import com.tfg.marllor.orientatree.R;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Beacon;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.TemplateType;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ReachAdapter extends RecyclerView.Adapter<ReachAdapter.MyViewHolder> {

    private Context context;
    private android.app.Activity reachesActivity;
    private int position;
    private String templateID;
    private Activity activity;
    private ArrayList<BeaconReached> reaches;
    private Template template;
    private String participantID;

    public ReachAdapter(android.app.Activity reachesActivity, Context context,
                        ArrayList<BeaconReached> reaches, String templateID,
                        Activity activity, Template template,
                        String participantID) {
        this.context = context;
        this.reachesActivity = reachesActivity;
        this.reaches = reaches;
        this.templateID = templateID;
        this.activity = activity;
        this.template = template;
        this.participantID = participantID;
    }

    @NonNull
    @NotNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.row_reach, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull MyViewHolder holder, int position) {

        this.position = position;
        BeaconReached reach = reaches.get(position);

        // useful IDs
        String beaconID = reach.getBeacon_id();

        // pattern to format the our at which the beacon was reached
        String pattern = "HH:mm:ss";
        DateFormat df = new SimpleDateFormat(pattern);

        holder.reachTime_textView.setText("Alcanzada: " + df.format(reach.getReachMoment()));

        // get the beacon to set the name and the number
        holder.db.collection("templates").document(templateID)
                .collection("beacons").document(beaconID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Beacon beacon = documentSnapshot.toObject(Beacon.class);
                        holder.reachTitle_textView.setText(beacon.getName());
                        holder.reachNumber_textView.setText("Baliza número " + beacon.getNumber());
                        if (template.getType() == TemplateType.EDUCATIVA) {
                            if (!reach.isAnswered()) {
                                holder.reachState_textView.setText("Pendiente");
                            } else {
                                holder.reachState_textView.setText("Respondida");
                                if (reach.isAnswer_right()) {
                                    holder.row_reach_cardView.setCardBackgroundColor(Color.parseColor("#b9f6ca"));
                                } else {
                                    holder.row_reach_cardView.setCardBackgroundColor(Color.parseColor("#ef9a9a"));
                                }
                            }
                        } else if (template.getType() == TemplateType.DEPORTIVA) {
                            holder.reachState_textView.setText("Sin contenido");
                        }
                    }
                });

        holder.row_reach_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (template.getType() == TemplateType.EDUCATIVA /*&& !reach.isGoal()*/) {
                    // if template DEPORTIVA we don't do anything
                    // same if it is goal
                    updateUIChallengeActivity(beaconID, activity);
                }
            }
        });
    }

    private void updateUIChallengeActivity(String beaconID, Activity activity) {
        Intent intent = new Intent(context, ChallengeActivity.class);
        intent.putExtra("beaconID", beaconID);
        intent.putExtra("activity", activity);
        intent.putExtra("participantID", participantID);
        reachesActivity.startActivityForResult(intent, 1);
    }

    @Override
    public int getItemCount() {
        return reaches.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView reachState_textView, reachTitle_textView,
                reachNumber_textView, reachTime_textView;

        MaterialCardView row_reach_cardView;

        FirebaseFirestore db;

        LinearLayout row_reach_layout;

        public MyViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);

            // start Firebase services
            db = FirebaseFirestore.getInstance();

            // bind UI elements
            reachState_textView = itemView.findViewById(R.id.reachState_textView);
            reachTitle_textView = itemView.findViewById(R.id.reachTitle_textView);
            reachNumber_textView = itemView.findViewById(R.id.reachNumber_textView);
            reachTime_textView = itemView.findViewById(R.id.reachTime_textView);
            row_reach_layout = itemView.findViewById(R.id.row_reach_layout);
            row_reach_cardView = itemView.findViewById(R.id.row_reach_cardView);

        }
    }
}
