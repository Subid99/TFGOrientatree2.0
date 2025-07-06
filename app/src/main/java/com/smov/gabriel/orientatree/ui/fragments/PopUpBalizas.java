package com.smov.gabriel.orientatree.ui.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.adapters.ReciclerBalizaAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Beacon;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.TemplateType;
import com.smov.gabriel.orientatree.model.User;
import com.smov.gabriel.orientatree.ui.ReviewActivity;
import com.smov.gabriel.orientatree.ui.TrackActivity;
import com.tfg.marllor.orientatree.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

public class PopUpBalizas {
    private String Titulo = "holaCaracola";
    private Participation Participacion;
    String pattern = "HH:mm:ss";
    DateFormat df = new SimpleDateFormat(pattern);
    FirebaseFirestore db;
    Activity activity;
    Template template;
    public PopUpBalizas(Participation currentParticipation, Template template, Activity activity) {
        Participacion = currentParticipation;
        db = FirebaseFirestore.getInstance();
        Titulo = currentParticipation.getParticipant();
        this.template= template;
        this.activity=activity;
    }

    //PopupWindow display method

    public void showPopupWindow(final View view) {

        db.collection("users").document(Participacion.getParticipant())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        User user = documentSnapshot.toObject(User.class);
                        if(user != null) {
                            Titulo= user.getName();
                            Log.v("Lista",user.getName());
                        }
                    }
                });
        //Create a View object yourself through inflater
        LayoutInflater inflater = (LayoutInflater) view.getContext().getSystemService(view.getContext().LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.pop_up_window, null);

        //Specify the length and width through constants
        int width = LinearLayout.LayoutParams.MATCH_PARENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;

        //Make Inactive Items Outside Of PopupWindow
        boolean focusable = true;

        //Create a window with our parameters
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
        //Set the location of the window on the screen
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        //Initialize the elements of our window, install the handler

        TextView test2 = popupView.findViewById(R.id.TituloPopup);
        TextView Inicio = popupView.findViewById(R.id.Inicio);
        TextView Fin = popupView.findViewById(R.id.Fin);
        Button Track = popupView.findViewById(R.id.VerTrack);
        db.collection("users").document(Participacion.getParticipant())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        User user = documentSnapshot.toObject(User.class);
                        if(user != null) {
                            Titulo= user.getName();
                            test2.setText(Titulo);

                            Log.v("Lista",user.getName());
                        }
                    }
                });
        if (Participacion.getStartTime() != null) {
            Inicio.setText("Inicio: " + df.format(Participacion.getStartTime()));
        }
        if (Participacion.getFinishTime() != null) {
            Fin.setText("Fin: " + df.format(Participacion.getFinishTime()));
        }
        ArrayList<BeaconReached> Balizasconseguidas = Participacion.getReaches();

        // Assign employeelist to ItemAdapter
        ReciclerBalizaAdapter itemAdapter = new ReciclerBalizaAdapter(Balizasconseguidas,activity,template,Participacion);
        // Set the LayoutManager that
        // this RecyclerView will use.
        RecyclerView recyclerView = popupView.findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        // adapter instance is set to the
        // recyclerview to inflate the items.
        recyclerView.setAdapter(itemAdapter);

        //Handler for clicking on the inactive zone of the window
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                //Close the window when clicked
                popupWindow.dismiss();
                return true;
            }
        });
        Track.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<String> participante = new ArrayList<>();
                participante.add(Participacion.getParticipant());
                ArrayList<String> nombre = new ArrayList<>();
                nombre.add(test2.getText().toString());
                Intent intent = new Intent(view.getContext(), TrackActivity.class);
                intent.putExtra("activity", activity);
                intent.putExtra("template", template);
                intent.putExtra("participantes",participante);
                intent.putExtra("nombres",nombre);
                view.getContext().startActivity(intent);
            }

        });
    }

}