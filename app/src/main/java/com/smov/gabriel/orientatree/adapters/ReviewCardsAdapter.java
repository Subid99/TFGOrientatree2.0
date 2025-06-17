package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.SelectionTracker;
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
import com.smov.gabriel.orientatree.ui.ReviewActivity;
import com.smov.gabriel.orientatree.ui.fragments.PopUpBalizas;
import com.tfg.marllor.orientatree.R;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

public class ReviewCardsAdapter extends RecyclerView.Adapter<ReviewCardsAdapter.MyViewHolder> {
    private ArrayList<Participation> participations;
    private Context context;
    private Template template;
    private Activity activity;
    private boolean SelectMode = false;
    private ReviewActivity reviewActivity;
    private ArrayList<String> selectedItems;

    public ReviewCardsAdapter(ArrayList<Participation> participations, Template template, Activity activity, ReviewActivity activity1) {
        this.participations = participations;
        this.template = template;
        this.activity = activity;
        this.reviewActivity = activity1;
        this.selectedItems = new ArrayList<>();
    }

    String pattern = "HH:mm:ss";
    DateFormat df = new SimpleDateFormat(pattern);

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
        return participations.size();
    }

    // This method binds the data to the ViewHolder object
    // for each item in the RecyclerView
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Participation currentParticipation = participations.get(position);
        String userID = currentParticipation.getParticipant();

        holder.db.collection("users").document(userID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            holder.nombre.setText(user.getName());
                            if (user.isHasPhoto()) {
                                StorageReference ref = holder.storageReference.child("profileImages/" + user.getId());
                                Glide.with(context)
                                        .load(ref)
                                        .diskCacheStrategy(DiskCacheStrategy.NONE) // prevent caching
                                        .skipMemoryCache(false) // prevent caching
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
                        holder.Balizas.setText(currentParticipation.obtenerResultados());
                    }
                });
        holder.Tarjeta.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Seleccion(holder,userID);

                return true;
            }
        });
        holder.Tarjeta.setOnClickListener(new View.OnClickListener() {
                                              @Override
                                              public void onClick(View v) {
                                                  if(SelectMode){
                                                      Seleccion(holder,userID);
                                                  }
                                                  else{
                                                      PopUpBalizas popUpBalizas = new PopUpBalizas(currentParticipation, template, activity);
                                                      popUpBalizas.showPopupWindow(v);
                                                  }


                                              }
                                          }
        );


        ParticipationState estadoParticipacion = currentParticipation.getState();
        if (estadoParticipacion == ParticipationState.FINISHED) {
            holder.estadoParticipacion.setText("Finalizado");
            holder.estadoParticipacion.setTextColor(Color.BLUE);
        } else if (estadoParticipacion == ParticipationState.NOW) {
            holder.estadoParticipacion.setText("En curso");
            holder.estadoParticipacion.setTextColor(Color.GREEN);
        } else if (estadoParticipacion == ParticipationState.NOT_YET) {
            holder.estadoParticipacion.setText("Sin Empezar");
            holder.estadoParticipacion.setTextColor(Color.RED);
        }
        String Tiempo = "Inicio --:--:--";
        if (currentParticipation.getStartTime() != null) {
            Tiempo = "Inicio " + df.format(currentParticipation.getStartTime());
        }
        if (currentParticipation.getFinishTime() != null) {
            Tiempo = "Fin " + df.format(currentParticipation.getFinishTime());
        }
        holder.Tiempo.setText(Tiempo);

    }

    @Override
    public long getItemId(int position) {
        return (long) position;
    }

    // This class defines the ViewHolder object for each item in the RecyclerView
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        FirebaseFirestore db;
        FirebaseStorage storage;
        StorageReference storageReference;
        SelectionTracker<String> tracker;
        TextView nombre;
        TextView estadoParticipacion;
        TextView Tiempo;
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
            Tiempo = itemView.findViewById(R.id.Tiempo);
            nombre = itemView.findViewById(R.id.Nombre);
            estadoParticipacion = itemView.findViewById(R.id.EstadoParticipacion);
            Balizas = itemView.findViewById(R.id.Balizas);
        }

        public String getNombre() {
            return nombre.getText().toString();
        }

    }
    private void Seleccion(MyViewHolder holder,String userID){
        if (SelectMode){
            if(!holder.Tarjeta.isSelected()){
                holder.Tarjeta.setSelected(true);
                holder.Tarjeta.setBackgroundColor(Color.GRAY);
                selectedItems.add(userID);
            }
            else{
                holder.Tarjeta.setSelected(false);
                holder.Tarjeta.setBackgroundColor(Color.WHITE);
                selectedItems.remove(userID);
                if (selectedItems.isEmpty()){
                    reviewActivity.ocultaBoton();
                    SelectMode = false;
                }
            }
        }
        else if(!SelectMode) {
            if (!holder.Tarjeta.isSelected()) {
                SelectMode = true;
                reviewActivity.muestraBoton();
                holder.Tarjeta.setSelected(true);
                holder.Tarjeta.setBackgroundColor(Color.GRAY);
                selectedItems.add(userID);
            } else {
                holder.Tarjeta.setSelected(false);
                holder.Tarjeta.setBackgroundColor(Color.WHITE);
                selectedItems.remove(userID);
            }
        }

    }
    public ArrayList<String> getSelectedItems() {
        return selectedItems;
    }
}


