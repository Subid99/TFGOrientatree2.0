package com.smov.gabriel.orientatree.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.adapters.ReciclerCardsAdapter;
import com.smov.gabriel.orientatree.adapters.ReciclerListAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.participationGiver;
import com.tfg.marllor.orientatree.R;

import java.util.ArrayList;
import java.util.Collections;

public class ListaFragment extends Fragment {
    private FirebaseFirestore db;
    private Activity activity;
    private Template template;

    private ArrayList<Participation> participations;
    private TextView emptyStateMessage_textView;
    private ConstraintLayout emptyState_layout;
    private RecyclerView recyclerView;
    private ListaFragment listaFragment;
    public ListaFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lista,
                container, false);
    }
    public void
    onViewCreated(@NonNull View view,
                  @Nullable Bundle savedInstanceState)
    {

        super.onViewCreated(view, savedInstanceState);
        // getting the employeelist
        Log.v("Hola",this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);

        // initializing Firebase services
        db = FirebaseFirestore.getInstance();
        // binding interface elements
        recyclerView = view.findViewById(R.id.recycleView);
        emptyState_layout = view.findViewById(R.id.peacockHead_emptyState);
        emptyStateMessage_textView = view.findViewById(R.id.emptyStateMessage_textView);


        listaFragment = (ListaFragment) this;

        // setting the AppBar
        // get the activity
        Intent intent = getActivity().getIntent();
        activity = (Activity) intent.getSerializableExtra("activity");
        template = (Template) intent.getSerializableExtra("template");

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
                            if(participations.size() < 1) {
                                emptyStateMessage_textView.setText("Parece que esta actividad no tiene participantes");
                                emptyState_layout.setVisibility(View.VISIBLE);
                            } else {
                                emptyStateMessage_textView.setText("");
                                emptyState_layout.setVisibility(View.GONE);
                            }

                            ReciclerListAdapter itemAdapter = new ReciclerListAdapter(participations,template,activity);
                            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getContext());
                            recyclerView.setLayoutManager(mLayoutManager);
                            recyclerView.setAdapter(itemAdapter);

                        }
                    });
        }

    }

}