package com.smov.gabriel.orientatree.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import com.smov.gabriel.orientatree.ui.fragments.CardsFragment;
import com.smov.gabriel.orientatree.ui.fragments.MapaFragment;
import com.smov.gabriel.orientatree.ui.fragments.ListaFragment;
import com.google.android.material.tabs.TabLayout;
import com.smov.gabriel.orientatree.adapters.ViewPagerAdapter;
import com.tfg.marllor.orientatree.databinding.VigilanciaActividadActivityBinding;


public class VigilanciaActividadActivity extends AppCompatActivity {

    private VigilanciaActividadActivityBinding binding;
    private MapaFragment MapaFragment;
    private CardsFragment CardsFragment;
    private ListaFragment ListaFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MapaFragment = new MapaFragment();
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