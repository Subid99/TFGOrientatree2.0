package com.smov.gabriel.orientatree.ui.fragments;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.tfg.marllor.orientatree.R;

public class ListaFragment extends Fragment {

    public ListaFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lista, container, false);
        TableLayout tableLayout = rootView.findViewById(R.id.tablaMain);
        for (int i = 0; i < 5; i++) {
            TableRow tableRow = new TableRow(getActivity());
            tableRow.setLayoutParams(new TableRow.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
            float textSizeSp = 16;
            TextView Numero = new TextView(getActivity());
            Numero.setGravity(Gravity.CENTER);
            Numero.setPadding(8, 8, 8, 8);
            Numero.setText(String.valueOf(i));
            Numero.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
            TextView Nombre = new TextView(getActivity());
            Nombre.setGravity(Gravity.CENTER);
            Nombre.setPadding(8, 8, 8, 8);
            Nombre.setText("Ricardito" + String.valueOf(i));
            Nombre.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
            TextView Estado = new TextView(getActivity());
            Estado.setGravity(Gravity.CENTER);
            Estado.setPadding(8, 8, 8, 8);
            Estado.setText("En Curso");
            Estado.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
            TextView NumBalizas = new TextView(getActivity());
            NumBalizas.setGravity(Gravity.CENTER);
            NumBalizas.setPadding(8, 8, 8, 8);
            NumBalizas.setText(String.valueOf(i)+ "/6");
            NumBalizas.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
            tableRow.addView(Numero);
            tableRow.addView(Nombre);
            tableRow.addView(Estado);
            tableRow.addView(NumBalizas);

            // Agregar TableRow al TableLayout
            tableLayout.addView(tableRow);
        }
        return rootView;
    }


}