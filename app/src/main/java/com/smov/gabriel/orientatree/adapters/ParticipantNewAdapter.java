package com.smov.gabriel.orientatree.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;


import com.smov.gabriel.orientatree.model.Participation;
import com.tfg.marllor.orientatree.R;

import java.util.ArrayList;

public class ParticipantNewAdapter extends RecyclerView.Adapter<ParticipantNewAdapter.MyViewHolder> {
    private ArrayList<Participation> emplist;

    public ParticipantNewAdapter(ArrayList<Participation> emplist) {
        this.emplist = emplist;
    }

    // This method creates a new ViewHolder object for each item in the RecyclerView
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Inflate the layout for each item and return a new ViewHolder object
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.lista_participantes, parent, false);
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
        holder.nombre.setText(currentParticipation.getParticipant());
        holder.Balizas.setText(currentParticipation.obtenerResultados());
        holder.estadoParticipacion.setText(currentParticipation.getState().toString());
    }

    // This class defines the ViewHolder object for each item in the RecyclerView
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView nombre;
        private TextView estadoParticipacion;
        private TextView Balizas;

        public MyViewHolder(View itemView) {
            super(itemView);
            nombre = itemView.findViewById(R.id.Nombre);
            estadoParticipacion = itemView.findViewById(R.id.EstadoParticipacion);
            Balizas = itemView.findViewById(R.id.Balizas);
        }
    }
}
