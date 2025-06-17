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
import android.widget.CompoundButton;
import android.widget.TextView;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.tfg.marllor.orientatree.R;
import com.tfg.marllor.orientatree.databinding.ActivityTrackBinding;
import com.smov.gabriel.orientatree.model.Activity;
import com.smov.gabriel.orientatree.model.Location;
import com.smov.gabriel.orientatree.model.Map;
import com.smov.gabriel.orientatree.model.Template;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

public class TrackActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityTrackBinding binding;

    // UI elements
    private Toolbar toolbar;
    private Slider track_slider;
    private TextView trackHour_textView;
    private SwitchMaterial trackCompleto_switch;

    // useful model objects
    private Template template;
    private Activity activity;
    private Map templateMap;
    //private Participation participation;
    private Duration duracionMax;
    // useful IDs
    private String userID;
    private String activityID;
    private ArrayList<String> usuarios;
    // to format the way hours are displayed
    private static String pattern_hour = "HH:mm:ss";
    private static DateFormat df_hour = new SimpleDateFormat(pattern_hour);

    // arraylist with the locations
    private ArrayList<Location> locations;
    private ArrayList<ArrayList> localizaciones;
    // objects needed to show the track
    private PolylineOptions polylineOptions;
    private ArrayList<Polyline> polylinesPartial;

    private ArrayList<Polyline> polylinesComplete;
    private Polyline polyline1; // (partial track)
    private Polyline polyline2; // (complete track)

    // max number of points that are shown at the same time in the track
    private static final int RANGE = 100;

    // Firebase services
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("Hola", this.getClass().getSimpleName());
        super.onCreate(savedInstanceState);

        binding = ActivityTrackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.trackMap);
        mapFragment.getMapAsync(this);

        // getting the intent
        Intent intent = getIntent();
        template = (Template) intent.getSerializableExtra("template");
        activity = (Activity) intent.getSerializableExtra("activity");
        usuarios = intent.getExtras().getStringArrayList("participantes");
        for (int i = 0; usuarios.size() > i; i++) {
            Log.v("participantes", usuarios.get(i));
        }
        // get the important IDs
        if (activity != null /*&& participation != null*/) {
            activityID = activity.getId();
            userID = usuarios.get(0);
        }
        duracionMax =Duration.ZERO;
        // binding UI elements
        toolbar = findViewById(R.id.track_toolbar);
        track_slider = findViewById(R.id.track_slider);
        trackHour_textView = findViewById(R.id.trackHour_textView);
        trackCompleto_switch = findViewById(R.id.trackComplete_switch);

        // set the toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // initializing Firebase services
        db = FirebaseFirestore.getInstance();

        track_slider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull @NotNull Slider slider, float value, boolean fromUser) {
                Log.v("slider", "value: " + value);
                Duration pctTiempo = duracionMax.dividedBy(100).multipliedBy((int)value);
                for(int i = 0; i < polylinesPartial.size();i++){
                if (polylinesPartial.get(i) != null && localizaciones.get(i) != null) {
                    ArrayList<LatLng> points = new ArrayList<>();
                    int index = (int) value;
                    Location firstLocation = (Location) localizaciones.get(i).get(0);
                    Instant tiempoMax = firstLocation.getTime().toInstant().plus(pctTiempo);
                    Date max = Date.from(tiempoMax);
                    for (int j = 0; j < localizaciones.get(i).size(); j++) {
                            if (localizaciones.get(i).get(j) != null) {
                                Location locations = (Location) localizaciones.get(i).get(j);
                                if(locations.getTime().before(max)){
                                    LatLng p = new LatLng(locations.getLocation().getLatitude(),
                                            locations.getLocation().getLongitude());
                                    points.add(p);
                                }

                            }

                    }
                    polylinesPartial.get(i).setPoints(points);
                    long totalSeconds = pctTiempo.getSeconds();
                    long hours = totalSeconds / 3600;
                    long minutes = (totalSeconds % 3600) / 60;
                    long seconds = totalSeconds % 60;
                    String formattedDuration = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                    trackHour_textView.setText(formattedDuration);
                }
            }
            }
        });

        trackCompleto_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    for(Polyline track : polylinesComplete){
                        track.setVisible(true);
                    }
                } else {
                    for(Polyline track : polylinesComplete){
                        track.setVisible(false);
                    }
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
        // our map...
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
                            File mypath = new File(directory, activity.getTemplate() + ".png");
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

                            // get the locations, create the polyline, enable the slider
                            if (!usuarios.isEmpty() && activityID != null) {
                                //initialize arrayList containing the locations
                                localizaciones = new ArrayList<>();
                                polylinesComplete = new ArrayList<>();
                                polylinesPartial = new ArrayList<>();
                                track_slider.setValueFrom(0);
                                track_slider.setValueTo(100);
                                track_slider.setEnabled(true);
                                trackHour_textView.setText("00:00:00");
                                Log.v("localizaciones", "tamaño usuarios " +  String.valueOf(usuarios.size()));
                                for (int i = 0; i < usuarios.size(); i++) {
                                    ArrayList<Location> localizacion = new ArrayList<>();
                                    db.collection("activities").document(activityID)
                                            .collection("participations").document(usuarios.get(i))
                                            .collection("locations")
                                            .orderBy("time", Query.Direction.ASCENDING)
                                            .get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                                                    for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {
                                                        Location location = documentSnapshot.toObject(Location.class);
                                                        localizacion.add(location);
                                                    }
                                                    Polyline polylineParcial;
                                                    Polyline polyLineCompleto;
                                                    if (localizacion != null && localizacion.size() >= 1) {
                                                        // setting partial track
                                                        // set slider parameters

                                                        if (localizacion.get(0) != null) {

                                                            polylineOptions = new PolylineOptions()
                                                                    .add(new LatLng(localizacion.get(0).getLocation().getLatitude(),
                                                                            localizacion.get(0).getLocation().getLongitude()));
                                                            polylineParcial = mMap.addPolyline(polylineOptions); // draw point at the start (partial track)
                                                            polylineParcial.setWidth(15);

                                                                 // enable slider

                                                            // setting complete track
                                                            polyLineCompleto = mMap.addPolyline(polylineOptions); // draw point at the start (complete track)
                                                            polyLineCompleto.setVisible(false);
                                                            polyLineCompleto.setColor(R.color.primary_color);
                                                            polyLineCompleto.setWidth(10);
                                                            ArrayList<LatLng> points = new ArrayList<>();
                                                            for (Location location : localizacion) {
                                                                LatLng p = new LatLng(location.getLocation().getLatitude(),
                                                                        location.getLocation().getLongitude());
                                                                points.add(p);
                                                            }
                                                            polyLineCompleto.setPoints(points);
                                                            trackCompleto_switch.setEnabled(true);
                                                            localizaciones.add(localizacion);
                                                            polylinesComplete.add(polyLineCompleto);
                                                            polylinesPartial.add(polylineParcial);
                                                            if(calculaDuracion(localizacion).compareTo(duracionMax)>0){
                                                                duracionMax=calculaDuracion(localizacion);
                                                                Log.v("duracion", String.valueOf(duracionMax.abs().get(ChronoUnit.SECONDS))+ " s");
                                                            }
                                                        }
                                                    } else {
                                                        Toast.makeText(TrackActivity.this, "No se han encontrado datos que mostrar", Toast.LENGTH_SHORT).show();
                                                    }
                                                    Log.v("localizaciones", "tamaño localizaciones" +String.valueOf(localizacion.size()));

                                                    Log.v("localizaciones", "tamaño array localizaciones" + String.valueOf(localizaciones.size()));

                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull @NotNull Exception e) {
                                                    Toast.makeText(TrackActivity.this, "Algo salió mal al descargar los datos de la participación. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                                Log.v("localizaciones", "tamaño array localizaciones final" + String.valueOf(localizaciones.size()));

                            } else {
                                Toast.makeText(TrackActivity.this, "Algo salió mal al obtener los datos de la participación. Sal y vuelve a intentarlo", Toast.LENGTH_SHORT).show();
                            }
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
    public Duration calculaDuracion(ArrayList<Location> localizacion){
        Duration duracion = Duration.ZERO;
        localizacion.sort(Comparator.comparing(Location::getTime));
        duracion = Duration.between(localizacion.get(0).getTime().toInstant(), localizacion.get(localizacion.size()-1).getTime().toInstant());
        return duracion;

    }


}
