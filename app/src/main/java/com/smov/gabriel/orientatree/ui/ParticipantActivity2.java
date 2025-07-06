package com.smov.gabriel.orientatree.ui;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;


import com.smov.gabriel.orientatree.adapters.ViewPagerAdapter;
import com.tfg.marllor.orientatree.databinding.ActivityParticipant2Binding;

public class ParticipantActivity2 extends AppCompatActivity {

    private ActivityParticipant2Binding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityParticipant2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewPagerAdapter adaptador = new ViewPagerAdapter(getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(adaptador);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);


    }
}