package com.smov.gabriel.orientatree.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Template;
import com.smov.gabriel.orientatree.ui.fragments.CardsFragment;
import com.smov.gabriel.orientatree.ui.fragments.MapOrganizerFragment;
import com.smov.gabriel.orientatree.ui.fragments.MapaFragment;
import com.smov.gabriel.orientatree.ui.fragments.ListaFragment;
import com.google.android.material.tabs.TabLayout;
import com.smov.gabriel.orientatree.adapters.ViewPagerAdapter;
import com.tfg.marllor.orientatree.databinding.VigilanciaActividadActivityBinding;


public class VigilanciaActividadActivity extends AppCompatActivity {
    private Template template;
    private Activity activity;
    private VigilanciaActividadActivityBinding binding;
    private MapOrganizerFragment MapaFragment;
    private CardsFragment CardsFragment;
    private ListaFragment ListaFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("Hola",this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        activity = (Activity) intent.getSerializableExtra("activity");
        template = (Template) intent.getSerializableExtra("template");
        MapaFragment = new MapOrganizerFragment();
        CardsFragment = new CardsFragment();
        ListaFragment = new ListaFragment();
        binding = VigilanciaActividadActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewPagerAdapter adaptador = new ViewPagerAdapter(getSupportFragmentManager());
        adaptador.addFragment(MapaFragment, "Mapa");
        adaptador.addFragment(CardsFragment, "General");
        adaptador.addFragment(ListaFragment, "Lista");
        //adaptador.addFragment(, "pruebaCards");
        ViewPager viewPager = binding.viewpager;
        viewPager.setAdapter(adaptador);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);


    }
}