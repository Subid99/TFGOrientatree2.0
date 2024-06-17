package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.tfg.marllor.orientatree.R;
import com.smov.gabriel.orientatree.adapters.ParticipantAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.model.TemplateType;

import java.util.ArrayList;
import java.util.Collections;

public class ParticipantsListActivity extends AppCompatActivity {
    private Activity activity;
    private Template template;

    private RecyclerView participantsList_recyclerView;
    private ParticipantAdapter participantAdapter;
    private ArrayList<Participation> participations;
    private ConstraintLayout emptyState_layout;
    private TextView emptyStateMessage_textView;
    private TextView participantsListparticipants_textView;

    // needed to pass it to the adapter so that cards can be clicked and head to a new activity
    private ParticipantsListActivity participantsListActivity;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("Hola",this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_participants_list);

        // initializing Firebase services
        db = FirebaseFirestore.getInstance();

        // binding interface elements
        participantsList_recyclerView = findViewById(R.id.participantsList_recyclerView);
        emptyState_layout = findViewById(R.id.peacockHead_emptyState);
        emptyStateMessage_textView = findViewById(R.id.emptyStateMessage_textView);
        participantsListparticipants_textView = findViewById(R.id.participantsListparticipants_textView);

        participantsListActivity = (ParticipantsListActivity) this;

        // setting the AppBar
                // get the activity
        Intent intent = getIntent();
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
                            if(template.getType() == TemplateType.DEPORTIVA) {
                                participantsListparticipants_textView.setText("Clasificación: ");
                            } else {
                                participantsListparticipants_textView.setText("Participantes: (" + participations.size() + ")");
                            }
                            participantAdapter = new ParticipantAdapter( ParticipantsListActivity.this,
                                    participations, template, activity);
                            participantsList_recyclerView.setAdapter(participantAdapter);
                            participantsList_recyclerView.setLayoutManager(new LinearLayoutManager(ParticipantsListActivity.this));
                        }
                    });
        }
    }

    @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case android.R.id.home:
                    this.finish();
                    return true;
            }
            return super.onOptionsItemSelected(item);
        }
}