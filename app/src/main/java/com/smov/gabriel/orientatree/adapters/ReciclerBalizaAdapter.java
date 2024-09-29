package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;


import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Beacon;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.TemplateType;
import com.smov.gabriel.orientatree.ui.ChallengeActivity;
import com.smov.gabriel.orientatree.ui.fragments.PopUpBalizas;
import com.tfg.marllor.orientatree.R;

import java.util.ArrayList;

public class ReciclerBalizaAdapter extends RecyclerView.Adapter<ReciclerBalizaAdapter.MyViewHolder> {
    private Context context;
    private android.app.Activity reachesActivity;
    private int position;
    private String templateID;
    private Activity activity;
    private ArrayList<BeaconReached> reaches;
    private Template template;
    private String participantID;
    public ReciclerBalizaAdapter(ArrayList<BeaconReached> reaches,
                                 Activity activity,
                                 Template template,
                                 String participantID) {
        this.reaches = reaches;
        this.templateID = template.getTemplate_id();
        this.activity = activity;
        this.template = template;
        this.participantID = participantID;


    }

    // This method creates a new ViewHolder object for each item in the RecyclerView
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the layout for each item and return a new ViewHolder object
        context = parent.getContext();
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.items_baliza, parent, false);
        return new MyViewHolder(itemView);
    }

    // This method returns the total
    // number of items in the data set
    @Override
    public int getItemCount() {
        if(reaches != null){
        return reaches.size();
        }
        return 0;
    }

    // This method binds the data to the ViewHolder object
    // for each item in the RecyclerView
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        BeaconReached currentBeacon = reaches.get(position);
        String beaconID=currentBeacon.getBeacon_id();
        holder.db.collection("templates").document(templateID)
                .collection("beacons").document(beaconID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        Beacon beacon = documentSnapshot.toObject(Beacon.class);
                        holder.nombre.setText(beacon.getName());
                                                                    }
                });
        if (!currentBeacon.isAnswered()){
            holder.estadoBaliza.setText("Sin Responder");
            holder.estadoBaliza.setTextColor(Color.GRAY);
        }
        else if(currentBeacon.isAnswer_right()) {
            holder.estadoBaliza.setText("Acierto");
            holder.estadoBaliza.setTextColor(Color.parseColor("#008000"));
        } else if (!currentBeacon.isAnswer_right()) {
            holder.estadoBaliza.setText("Fallo");
            holder.estadoBaliza.setTextColor(Color.RED);
        }

        holder.NumeroBaliza.setText(String.valueOf(position+1));
        holder.FilaBaliza.setOnClickListener(new View.OnClickListener() {

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
        context.startActivity(intent);
    }
    // This class defines the ViewHolder object for each item in the RecyclerView
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView nombre;
        private TextView estadoBaliza;
        private TextView NumeroBaliza;
        private TableRow FilaBaliza;
        FirebaseFirestore db;

        public MyViewHolder(View itemView) {
            super(itemView);
            db = FirebaseFirestore.getInstance();
            nombre = itemView.findViewById(R.id.Nombre);
            estadoBaliza = itemView.findViewById(R.id.EstadoBaliza);
            NumeroBaliza = itemView.findViewById(R.id.NumeroBaliza);
            FilaBaliza = itemView.findViewById(R.id.FilaBaliza);
        }
    }


    }
