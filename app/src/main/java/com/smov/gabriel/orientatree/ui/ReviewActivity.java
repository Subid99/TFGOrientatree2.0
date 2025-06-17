package com.smov.gabriel.orientatree.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.recyclerview.selection.SelectionTracker;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.adapters.ReviewCardsAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.Template;
import com.tfg.marllor.orientatree.R;
import com.tfg.marllor.orientatree.databinding.ReviewActivityBinding;

import java.util.ArrayList;
import java.util.Collections;


public class ReviewActivity extends AppCompatActivity {
    private Template template;
    private FirebaseFirestore db;
    private Activity activity;
    private ReviewActivityBinding binding;


    private ArrayList<Participation> participations;

    private SelectionTracker<String> selectionTracker;
    private RecyclerView recyclerView;
    private Button boton;
    private ReviewActivity reviewActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("Hola",this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        db = FirebaseFirestore.getInstance();
        reviewActivity = this;

        activity = (Activity) intent.getSerializableExtra("activity");
        template = (Template) intent.getSerializableExtra("template");
        binding = ReviewActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        recyclerView = reviewActivity.findViewById(R.id.recycleView);
        boton = reviewActivity.findViewById(R.id.button);
        boton.setVisibility(View.GONE);
        if(activity != null && template != null) {
            db.collection("activities").document(activity.getId())
                    .collection("participations")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                return;
                            }
                            participations = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : value) {
                                Participation participation = doc.toObject(Participation.class);
                                participations.add(participation);
                            }
                            // sort the participants
                            Collections.sort(participations, new Participation());
                            // show or hide the empty state with its message

                            ReviewCardsAdapter itemAdapter = new ReviewCardsAdapter(participations,template,activity,reviewActivity);
                            RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(reviewActivity, 3);
                            recyclerView.setLayoutManager(mLayoutManager);
                            recyclerView.setAdapter(itemAdapter);
                            boton.setOnClickListener(new View.OnClickListener(){
                                @Override
                                public void onClick(View v) {
                                    Toast.makeText(reviewActivity, itemAdapter.getSelectedItems().get(0), Toast.LENGTH_SHORT).show();
                                    for(int i=0;itemAdapter.getSelectedItems().size()>i;i++){
                                    Log.v("participantes",itemAdapter.getSelectedItems().get(i));
                                    }
                                    Intent intent = new Intent(ReviewActivity.this, TrackActivity.class);
                                    intent.putExtra("activity", activity);
                                    intent.putExtra("template", template);
                                    intent.putExtra("participantes", itemAdapter.getSelectedItems());
                                    startActivity(intent);
                                }
                            });
                        }
                    });
        }
        }
    public void muestraBoton(){
        boton.setVisibility(View.VISIBLE);
    }
    public void ocultaBoton(){
        boton.setVisibility(View.GONE);
    }

}