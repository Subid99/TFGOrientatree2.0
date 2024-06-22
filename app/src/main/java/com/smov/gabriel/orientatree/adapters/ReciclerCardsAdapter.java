package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;


import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.ParticipationState;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.User;
import com.smov.gabriel.orientatree.ui.fragments.PopUpBalizas;
import com.tfg.marllor.orientatree.R;

import java.util.ArrayList;
import java.util.Collections;

public class ReciclerCardsAdapter extends RecyclerView.Adapter<ReciclerCardsAdapter.MyViewHolder> {
    private ArrayList<Participation> emplist;
    private Context context;
    private Template template;
    private Activity activity;
    public ReciclerCardsAdapter(ArrayList<Participation> emplist, Template template, Activity activity ) {
        this.emplist = emplist;
        this.template = template;
        this.activity = activity;
    }

    // This method creates a new ViewHolder object for each item in the RecyclerView
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        context = parent.getContext();
        // Inflate the layout for each item and return a new ViewHolder object
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.items_card, parent, false);


        return new MyViewHolder(itemView);
    }

    // This method returns the total
    // number of items in the data set
    @Override
    public int getItemCount() {
        return emplist.size();
    }

    // This method binds the data to the ViewHolder object
    // for each item in the RecyclerView
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Participation currentParticipation = emplist.get(position);
        String userID = currentParticipation.getParticipant();

        holder.db.collection("users").document(userID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        User user = documentSnapshot.toObject(User.class);
                        if(user != null) {
                            holder.nombre.setText(user.getName());
                            if(user.isHasPhoto()) {
                                StorageReference ref = holder.storageReference.child("profileImages/" + user.getId());
                                Glide.with(context)
                                        .load(ref)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE ) // prevent caching
                                        .skipMemoryCache(true) // prevent caching
                                        .into(holder.participantImageView);
                            }
                        }
                    }
                });
        holder.db.collection("activities").document(activity.getId())
                .collection("participations").document(userID)
                .collection("beaconReaches")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return;
                        }
                        ArrayList<BeaconReached> beaconsReached = new ArrayList<BeaconReached>();
                        for (DocumentSnapshot documentSnapshot : value) {
                            // here we have a list with the reaches achieved
                            BeaconReached beaconReached = documentSnapshot.toObject(BeaconReached.class);
                            beaconsReached.add(beaconReached);
                        }
                        Collections.sort(beaconsReached, new BeaconReached());
                        Collections.reverse(beaconsReached);
                        currentParticipation.setReaches(beaconsReached);
                        holder.Balizas.setText(currentParticipation.getResults());
                    }
                });

        holder.Tarjeta.setOnClickListener(new View.OnClickListener() {
                                              @Override
                                              public void onClick(View v) {

                                                  PopUpBalizas popUpBalizas = new PopUpBalizas(currentParticipation,template,activity);
                                                  popUpBalizas.showPopupWindow(v);
                                              }
                                          }
        );


        ParticipationState estadoParticipacion = currentParticipation.getState();
        if(estadoParticipacion==ParticipationState.FINISHED) {
            holder.estadoParticipacion.setText("Finalizado");
            holder.estadoParticipacion.setTextColor(Color.BLUE);
        }
        else if(estadoParticipacion==ParticipationState.NOW){
            holder.estadoParticipacion.setText("En curso");
            holder.estadoParticipacion.setTextColor(Color.GREEN);
        }
        else if(estadoParticipacion==ParticipationState.NOT_YET) {
            holder.estadoParticipacion.setText("Sin Empezar");
            holder.estadoParticipacion.setTextColor(Color.RED);
        }
    }

    // This class defines the ViewHolder object for each item in the RecyclerView
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        FirebaseFirestore db;
        FirebaseStorage storage;
        StorageReference storageReference;

        TextView nombre;
        TextView estadoParticipacion;

        TextView Balizas;
        CardView Tarjeta;
        ImageView participantImageView;
        public MyViewHolder(View itemView) {
            super(itemView);
            db = FirebaseFirestore.getInstance();
            storage = FirebaseStorage.getInstance();
            storageReference = storage.getReference();

            participantImageView = itemView.findViewById(R.id.imageViewParticipante);
            Tarjeta = itemView.findViewById(R.id.Tarjeto);
            nombre = itemView.findViewById(R.id.Nombre);
            estadoParticipacion = itemView.findViewById(R.id.EstadoParticipacion);
            Balizas = itemView.findViewById(R.id.Balizas);
        }
    }
}
