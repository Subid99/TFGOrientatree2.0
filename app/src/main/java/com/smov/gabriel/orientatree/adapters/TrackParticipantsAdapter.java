package com.smov.gabriel.orientatree.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Participation;
import com.smov.gabriel.orientatree.model.Template;
import com.tfg.marllor.orientatree.R;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TrackParticipantsAdapter extends RecyclerView.Adapter<TrackParticipantsAdapter.MyViewHolder> {
    private ArrayList<String> participantes;
    private Context context;
    public TrackParticipantsAdapter(Context context, ArrayList<String> participants) {
        this.context = context;
        this.participantes = participants;
    }
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.track_participants, parent, false);
        return new TrackParticipantsAdapter.MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        String nombre = participantes.get(position);
        holder.textView.setText(nombre);
        int colorResId;
        switch(position % 3) {
            case 0: colorResId = R.drawable.solid_color; break;
            case 1: colorResId = R.drawable.solid_color2; break;
            default: colorResId = R.drawable.solid_color3;
        }
        holder.imageView.setImageResource(colorResId);
    }

    @Override
    public int getItemCount() {
        return participantes != null ? participantes.size() : 0;
    }

public class MyViewHolder extends RecyclerView.ViewHolder {
    public ImageView imageView;
    public TextView textView;

    public MyViewHolder(@NonNull View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.Color);
        textView = itemView.findViewById(R.id.Nombre);
    }
}
}