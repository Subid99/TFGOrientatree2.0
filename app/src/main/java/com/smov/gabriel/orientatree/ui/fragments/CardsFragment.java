package com.smov.gabriel.orientatree.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.adapters.ParticipantAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.TemplateType;
import com.smov.gabriel.orientatree.ui.ParticipantsListActivity;
import com.tfg.marllor.orientatree.R;

import java.util.ArrayList;
import java.util.Collections;

public class CardsFragment extends Fragment {
    private Activity activity;
    private Template template;

    private RecyclerView participantsList_recyclerView;
    private ParticipantAdapter participantAdapter;
    private ArrayList<Participation> participations;
    private ConstraintLayout emptyState_layout;
    private TextView emptyStateMessage_textView;
    private TextView participantsListparticipants_textView;

    // needed to pass it to the adapter so that cards can be clicked and head to a new activity
    private CardsFragment cardsFragment;

    private FirebaseFirestore db;
    public CardsFragment() {
        // Constructor vacío requerido
    }
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            String param1 = getArguments().getString("param1");
            String param2 = getArguments().getString("param2");
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.activity_participants_list,
                container, false);
    }
    @Override
    public void
    onViewCreated(@NonNull View view,
                  @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        // initializing Firebase services
        db = FirebaseFirestore.getInstance();

        // binding interface elements
        participantsList_recyclerView = getView().findViewById(R.id.participantsList_recyclerView);
        emptyState_layout = getView().findViewById(R.id.peacockHead_emptyState);
        emptyStateMessage_textView = getView().findViewById(R.id.emptyStateMessage_textView);
        participantsListparticipants_textView = getView().findViewById(R.id.participantsListparticipants_textView);

        cardsFragment = (CardsFragment) this;
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
                            if(participations.isEmpty()) {
                                emptyStateMessage_textView.setText("Parece que esta actividad no tiene participantes");
                                emptyState_layout.setVisibility(View.VISIBLE);
                            } else {
                                emptyStateMessage_textView.setText("");
                                emptyState_layout.setVisibility(View.GONE);
                            }
                            if(template.getType() == TemplateType.DEPORTIVA) {
                                participantsListparticipants_textView.setText("Clasificación: ");
                            } else {
                                participantsListparticipants_textView.setText("Participantes: (" + participations.size() + ")");
                            }
                            participantAdapter = new ParticipantAdapter(getContext(),
                                    participations, template, activity);
                            participantsList_recyclerView.setAdapter(participantAdapter);
                            participantsList_recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                        }
                    });
        }
    }
}
