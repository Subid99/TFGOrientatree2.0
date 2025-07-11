package com.smov.gabriel.orientatree.ui.fragments;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.tfg.marllor.orientatree.R;
import com.smov.gabriel.orientatree.model.BeaconReached;
import com.smov.gabriel.orientatree.ui.ChallengeActivity;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ChallengeQuizFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChallengeQuizFragment extends Fragment {

    private ChallengeActivity ca;

    private Button challengeQuiz_button;
    private CircularProgressIndicator challengeQuiz_progressIndicator;
    private RadioButton quiz_radioButton_0, quiz_radioButton_1, quiz_radioButton_2, quiz_radioButton_3;
    private RadioGroup quiz_radioGroup;

    private BeaconReached beaconReached;

    private int radioButton_selected = 0;
    private boolean givenAnswerIsRight = false;

    // here we store the possible_answers
    private ArrayList<String> possible_answers;

    // flag to know if we successfully got the possible_answers or not
    private boolean possible_answers_set = false;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ChallengeQuizFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChallengeQuizFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChallengeQuizFragment newInstance(String param1, String param2) {
        ChallengeQuizFragment fragment = new ChallengeQuizFragment();
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
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_challenge_quiz, container, false);

        ca = (ChallengeActivity) getActivity();

        // bind interface elements
        challengeQuiz_button = view.findViewById(R.id.challengeQuiz_button);
        challengeQuiz_progressIndicator = view.findViewById(R.id.challengeQuiz_progressIndicator);
        quiz_radioButton_0 = view.findViewById(R.id.quiz_radio_button_0);
        quiz_radioButton_1 = view.findViewById(R.id.quiz_radio_button_1);
        quiz_radioButton_2 = view.findViewById(R.id.quiz_radio_button_2);
        quiz_radioButton_3 = view.findViewById(R.id.quiz_radio_button_3);
        quiz_radioGroup = view.findViewById(R.id.quiz_radioGroup);

        // set the text for the different options
        if(ca.beacon != null) {
            possible_answers = ca.beacon.getPossible_answers();
            if(possible_answers != null) {
                if(possible_answers.size() == 4) {
                    quiz_radioButton_0.setText(possible_answers.get(0));
                    quiz_radioButton_1.setText(possible_answers.get(1));
                    quiz_radioButton_2.setText(possible_answers.get(2));
                    quiz_radioButton_3.setText(possible_answers.get(3));
                    possible_answers_set = true;
                }
            }
        }

        // notify the user in case that the different options couldn't be set
        if(!possible_answers_set) {
            Toast.makeText(ca, "Algo salió mal al cargar las posibles respuestas", Toast.LENGTH_SHORT).show();
        }

        // get the reach to check if already answered
        ca.db.collection("activities").document(ca.activityID)
                .collection("participations").document(ca.userID)
                .collection("beaconReaches").document(ca.beacon.getBeacon_id())
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        beaconReached = documentSnapshot.toObject(BeaconReached.class);
                        if(beaconReached.isAnswered()) {
                            // if the reach has already been answered, get what the user answered
                            // and show some feedback, but without enabling any actions
                            radioButton_selected = beaconReached.getQuiz_answer();
                            if(beaconReached.isAnswer_right()) {
                                showPositiveFeedBack();
                            } else {
                                showNegativeFeedBack();
                            }
                        } else {
                            if(!ca.organizer) {
                                Date current_time = new Date(System.currentTimeMillis());
                                if(current_time.before(ca.activity.getFinishTime())) {
                                    // if the reach has not been answered yet, and we are not the organizer
                                    // and the activity didn't finish yet then enable the radio buttons
                                    quiz_radioButton_0.setClickable(true);
                                    quiz_radioButton_1.setClickable(true);
                                    quiz_radioButton_2.setClickable(true);
                                    quiz_radioButton_3.setClickable(true);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        Toast.makeText(ca, "Algo salió mal, vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                    }
                });

        // radio group listener
        quiz_radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // when we check a radio button, the answer button is enabled
                challengeQuiz_button.setEnabled(true);
                switch (checkedId){
                    case R.id.quiz_radio_button_0:
                        radioButton_selected = 0;
                        break;
                    case R.id.quiz_radio_button_1:
                        radioButton_selected = 1;
                        break;
                    case R.id.quiz_radio_button_2:
                        radioButton_selected = 2;
                        break;
                    case R.id.quiz_radio_button_3:
                        radioButton_selected = 3;
                         break;
                    default:
                        break;
                }
            }
        });

        challengeQuiz_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(getContext())
                        .setTitle("Envío de respuesta")
                        .setMessage("¿Desea enviar su respuesta?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Enviar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                challengeQuiz_progressIndicator.setVisibility(View.VISIBLE);
                                // check if the given answer is right
                                if(radioButton_selected == ca.beacon.getQuiz_right_answer()) {
                                    givenAnswerIsRight = true;
                                } else {
                                    givenAnswerIsRight = false;
                                }
                                updateBeaconReach();
                            }
                        })
                        .show();
            }
        });

        return view;
    }

    private void updateBeaconReach() {
        challengeQuiz_progressIndicator.setVisibility(View.GONE);
        challengeQuiz_button.setEnabled(false);
        quiz_radioGroup.setEnabled(false);
        quiz_radioButton_0.setClickable(false);
        quiz_radioButton_1.setClickable(false);
        quiz_radioButton_2.setClickable(false);
        quiz_radioButton_3.setClickable(false);
        if(givenAnswerIsRight) {
            showPositiveFeedBack();
        } else {
            showNegativeFeedBack();
        }
        ca.db.collection("activities").document(ca.activityID)
                .collection("participations").document(ca.userID)
                .collection("beaconReaches").document(ca.beacon.getBeacon_id())
                .update("answer_right", givenAnswerIsRight,
                        "quiz_answer", radioButton_selected,
                        "answered", true)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        challengeQuiz_progressIndicator.setVisibility(View.GONE);
                        Toast.makeText(ca, "Algo salió mal, vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showNegativeFeedBack() {
        switch (radioButton_selected){
            case 0:
                quiz_radioButton_0.setTextColor(getResources().getColor(R.color.error_red));
                quiz_radioButton_0.append("\n INCORRECTO");
                break;
            case 1:
                quiz_radioButton_1.setTextColor(getResources().getColor(R.color.error_red));
                quiz_radioButton_1.append("\n INCORRECTO");
                break;
            case 2:
                quiz_radioButton_2.setTextColor(getResources().getColor(R.color.error_red));
                quiz_radioButton_2.append("\n INCORRECTO");
                break;
            case 3:
                quiz_radioButton_3.setTextColor(getResources().getColor(R.color.error_red));
                quiz_radioButton_3.append("\n INCORRECTO");
                break;
        }
        switch (ca.beacon.getQuiz_right_answer()) {
            case 0:
                quiz_radioButton_0.setTextColor(getResources().getColor(R.color.secondary_color_variant));
                quiz_radioButton_0.append("\n La respuesta correcta era esta");
                break;
            case 1:
                quiz_radioButton_1.setTextColor(getResources().getColor(R.color.secondary_color_variant));
                quiz_radioButton_1.append("\n La respuesta correcta era esta");
                break;
            case 2:
                quiz_radioButton_2.setTextColor(getResources().getColor(R.color.secondary_color_variant));
                quiz_radioButton_2.append("\n La respuesta correcta era esta");
                break;
            case 3:
                quiz_radioButton_3.setTextColor(getResources().getColor(R.color.secondary_color_variant));
                quiz_radioButton_3.append("\n La respuesta correcta era esta");
                break;
            default:
                break;
        }
    }

    private void showPositiveFeedBack() {
        switch (radioButton_selected) {
            case 0:
                quiz_radioButton_0.setTextColor(getResources().getColor(R.color.primary_color));
                quiz_radioButton_0.append("\n  CORRECTO");
                break;
            case 1:
                quiz_radioButton_1.setTextColor(getResources().getColor(R.color.primary_color));
                quiz_radioButton_1.append("\n  CORRECTO");
                break;
            case 2:
                quiz_radioButton_2.setTextColor(getResources().getColor(R.color.primary_color));
                quiz_radioButton_2.append("\n  CORRECTO");
                break;
            case 3:
                quiz_radioButton_3.setTextColor(getResources().getColor(R.color.primary_color));
                quiz_radioButton_3.append("\n  CORRECTO");
                break;
            default:
                break;
        }
    }
}