package com.smov.gabriel.orientatree.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.tfg.marllor.orientatree.R;
import com.tfg.marllor.orientatree.databinding.ActivityOrganizerMapBinding;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Map;
import com.smov.gabriel.orientatree.model.Template;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class OrganizerMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityOrganizerMapBinding binding;

    private Toolbar toolbar;
    private ExtendedFloatingActionButton organizerMapParticipants_fab;

    private Map templateMap;
    private Template template;
    private Activity activity;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("Hola",this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);

        binding = ActivityOrganizerMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.organizer_map);
        mapFragment.getMapAsync(this);

        db = FirebaseFirestore.getInstance();

        //toolbar = findViewById(R.id.organizerMap_toolbar);
        organizerMapParticipants_fab = findViewById(R.id.organizerMapParticipants_fab);

        // set the toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // get the activity
        Intent intent = getIntent();
        activity = (Activity) intent.getSerializableExtra("activity");
        template = (Template) intent.getSerializableExtra("template");

        if(activity != null && template != null) {
            // if these attributes are not null, it means that we came from the now activity
            // in which case we should offer the option of watching the participants
            organizerMapParticipants_fab.setEnabled(true);
            organizerMapParticipants_fab.setVisibility(View.VISIBLE);
        } // in other case, we come from the template and we don't have to give that option

        organizerMapParticipants_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (template != null && activity != null) {
                    updateUIParticipants();
                } else {
                    Toast.makeText(OrganizerMapActivity.this, "No se pudo completar la acción. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // setting styles...
        try {
            boolean success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));
            if (!success) {
                Toast.makeText(this, "Algo salió mal al configurar el mapa", Toast.LENGTH_SHORT).show();
            }
        } catch (Resources.NotFoundException e) {
            Toast.makeText(this, "Algo salió mal al configurar el mapa", Toast.LENGTH_SHORT).show();
        }

        if (template != null) {
            CollectionReference collectionRef = db.collection("maps");
            collectionRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {

                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        // La consulta fue exitosa
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                // Procesa cada documento aquí
                                Object data = document.getData();
                                templateMap = document.toObject(Map.class);
                                Log.d("PruebaMapa", "MapID;" + templateMap.getMap_id());
                            }
                        }
                    } else {
                        // La consulta falló
                        Log.d("PruebaMapa", "Error getting documents: ", task.getException());
                    }
                }
            });
            db.collection("maps").document(template.getMap_id())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            // getting the map
                            templateMap = documentSnapshot.toObject(Map.class);
                            // where to center the map at the outset
                            LatLng center_map = new LatLng(templateMap.getCentering_point().getLatitude(),
                                    templateMap.getCentering_point().getLongitude());

                            // get the map image from a file and reduce its size
                            ContextWrapper cw = new ContextWrapper(getApplicationContext());
                            File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
                            //File mypath = new File(directory, activity.getId() + ".png");
                            File mypath = new File(directory, template.getTemplate_id() + ".png");
                            Bitmap image_bitmap = decodeFile(mypath, 540, 960);
                            BitmapDescriptor image = BitmapDescriptorFactory.fromBitmap(image_bitmap);

                            LatLngBounds overlay_bounds = new LatLngBounds(
                                    new LatLng(templateMap.getOverlay_corners().get(0).getLatitude(),
                                            templateMap.getOverlay_corners().get(0).getLongitude()),       // South west corner
                                    new LatLng(templateMap.getOverlay_corners().get(1).getLatitude(),
                                            templateMap.getOverlay_corners().get(1).getLongitude()));

                            // set image as overlay
                            GroundOverlayOptions overlayMap = new GroundOverlayOptions()
                                    .image(image)
                                    .positionFromBounds(overlay_bounds);

                            // set the overlay on the map
                            mMap.addGroundOverlay(overlayMap);

                            mMap.moveCamera(CameraUpdateFactory.newLatLng(center_map));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center_map, templateMap.getInitial_zoom()));

                            // setting maximum and minimum zoom the user can perform on the map
                            mMap.setMinZoomPreference(templateMap.getMin_zoom());
                            mMap.setMaxZoomPreference(templateMap.getMax_zoom());

                            // setting bounds for the map so that user can not navigate other places
                            LatLngBounds map_bounds = new LatLngBounds(
                                    new LatLng(templateMap.getMap_corners().get(0).getLatitude(),
                                            templateMap.getMap_corners().get(0).getLongitude()), // SW bounds
                                    new LatLng(templateMap.getMap_corners().get(1).getLatitude(),
                                            templateMap.getMap_corners().get(1).getLongitude())  // NE bounds
                            );
                            mMap.setLatLngBoundsForCameraTarget(map_bounds);
                        }
                    });
        } else {
            Toast.makeText(this, "Algo salió mal al cargar el mapa", Toast.LENGTH_SHORT).show();
        }
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    // decode image from file
    private Bitmap decodeFile(File f, int width, int height) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);

            int scale = calculateInSampleSize(o, width, height);

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Algo salió mal al cargar el mapa", Toast.LENGTH_SHORT).show();
        }
        return null;
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

    private void updateUIParticipants() {
        Intent intent = new Intent(OrganizerMapActivity.this, ParticipantsListActivity.class);
        intent.putExtra("activity", activity);
        intent.putExtra("template", template);
        startActivity(intent);
    }
}