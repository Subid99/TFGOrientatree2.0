package com.smov.gabriel.orientatree.ui.fragments;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.smov.gabriel.orientatree.adapters.ActivityAdapter;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.persistence.AppDatabase;
import com.smov.gabriel.orientatree.persistence.entities.OfflineActivity;
import com.smov.gabriel.orientatree.ui.HomeActivity;
import com.tfg.marllor.orientatree.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link OnGoingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OnGoingFragment extends Fragment implements View.OnClickListener {

    private RecyclerView onGoing_recyclerView;
    private ActivityAdapter activityAdapter;

    private SwipeRefreshLayout onGoing_pull_layout;

    private ArrayList<Activity> first_selection;
    private ArrayList<Activity> ultimate_selection;
    private ArrayList<Activity> no_duplicates_activities; // to remove duplicates due to being both organizer and participant

    private HomeActivity homeActivity;
    private AppDatabase localDb;


    private ConstraintLayout no_activities_layout;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public OnGoingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OnGoingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OnGoingFragment newInstance(String param1, String param2) {
        OnGoingFragment fragment = new OnGoingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
            localDb = AppDatabase.getDatabase(this.getContext());
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_on_going, container, false);

        homeActivity = (HomeActivity) getActivity();
        localDb = AppDatabase.getDatabase(this.getContext());
        onGoing_pull_layout = view.findViewById(R.id.onGoing_pull_layout);

        onGoing_pull_layout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getActivities(view);
                onGoing_pull_layout.setRefreshing(false);
            }
        });

        no_activities_layout = view.findViewById(R.id.onGoing_empty_layout);

        getActivities(view);

        return view;
    }

    private void getActivities(View view) {
        // Verificar conexión a internet
        if (isOnline()) {
            getActivitiesFromFirestore(view);
        } else {
            getActivitiesFromLocalDb(view);
        }
    }

    private void getActivitiesFromFirestore(View view) {
        first_selection = new ArrayList<>();
        ultimate_selection = new ArrayList<>();
        no_duplicates_activities = new ArrayList<>();

        long millis = System.currentTimeMillis();
        Date currentDate = new Date(millis);

        homeActivity.db.collection("activities")
                .whereGreaterThanOrEqualTo("finishTime", currentDate)
                .whereEqualTo("planner_id", homeActivity.userID)
                .get()
                .addOnCompleteListener(task -> {
                    processOrganizerActivities(task, view, currentDate);
                });
    }

    private void getActivitiesFromLocalDb(View view) {
        new Thread(() -> {
            long currentMillis = System.currentTimeMillis();
            List<OfflineActivity> localActivities = localDb.activityDao().getCurrentActivities(
                    currentMillis, homeActivity.userID);

            ArrayList<Activity> activities = new ArrayList<>();
            for (OfflineActivity entity : localActivities) {
                Activity activity = convertToActivity(entity);
                activities.add(activity);
            }

            // Eliminar duplicados y ordenar
            no_duplicates_activities = removeDuplicatesAndSort(activities);

            requireActivity().runOnUiThread(() -> {
                updateActivitiesUI(view);
            });
        }).start();
    }

    private void processOrganizerActivities(@NonNull Task<QuerySnapshot> task, View view, Date currentDate) {
        first_selection = new ArrayList<>();
        ultimate_selection = new ArrayList<>();
        no_duplicates_activities = new ArrayList<>();

        // Procesar actividades como organizador
        for (QueryDocumentSnapshot document : task.getResult()) {
            Activity activity = document.toObject(Activity.class);
            first_selection.add(activity);
        }

        // Procesar actividades como participante
        homeActivity.db.collection("activities")
                .whereGreaterThanOrEqualTo("finishTime", currentDate)
                .whereArrayContains("participants", homeActivity.userID)
                .get()
                .addOnCompleteListener(participantTask -> {
                    for (QueryDocumentSnapshot document : participantTask.getResult()) {
                        Activity activity = document.toObject(Activity.class);
                        first_selection.add(activity);
                    }
                    filterAndDisplayActivities(currentDate, view);
                });
    }

    private void filterAndDisplayActivities(Date currentDate, View view) {
        // Filtrar actividades que ya han comenzado
        for (Activity activity : first_selection) {
            if (currentDate.after(activity.getStartTime())) {
                ultimate_selection.add(activity);
            }
        }

        // Eliminar duplicados y ordenar
        no_duplicates_activities = removeDuplicatesAndSort(ultimate_selection);

        // Actualizar UI
        updateActivitiesUI(view);
    }

    private ArrayList<Activity> removeDuplicatesAndSort(List<Activity> activities) {
        ArrayList<Activity> uniqueActivities = new ArrayList<>();
        for (Activity a : activities) {
            boolean isFound = false;
            for (Activity b : uniqueActivities) {
                if (b.equals(a)) {
                    isFound = true;
                    break;
                }
            }
            if (!isFound) uniqueActivities.add(a);
        }
        Collections.sort(uniqueActivities, new Activity());
        return uniqueActivities;
    }

    private void updateActivitiesUI(View view) {
        if (no_duplicates_activities.size() < 1) {
            no_activities_layout.setVisibility(View.VISIBLE);
        } else {
            no_activities_layout.setVisibility(View.GONE);
        }

        activityAdapter = new ActivityAdapter(homeActivity, getContext(), no_duplicates_activities);
        onGoing_recyclerView = view.findViewById(R.id.onGoing_recyclerView);
        onGoing_recyclerView.setAdapter(activityAdapter);
        onGoing_recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private Activity convertToActivity(OfflineActivity entity) {
        Activity activity = new Activity();
        activity.setId(entity.id);
        activity.setTitle(entity.title);
        activity.setPlanner_id(entity.plannerId);
        activity.setTemplate(entity.template);
        activity.setKey(entity.key);
        activity.setStartTime(new Date(entity.startTime));
        activity.setFinishTime(new Date(entity.finishTime));
        activity.setParticipants(new ArrayList<>(entity.participants));
        return activity;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    public void onClick(View v) {

    }

}