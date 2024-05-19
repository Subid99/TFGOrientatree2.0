package com.smov.gabriel.orientatree.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.tfg.marllor.orientatree.R;

public class MapaFragment extends Fragment {

    public MapaFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Infla el layout XML del fragmento
        return inflater.inflate(R.layout.fragment_mapa, container, false);
    }
}