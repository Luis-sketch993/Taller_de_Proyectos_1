package com.example.myapplicationf.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.graphics.Color;

import com.example.myapplicationf.R;
import com.example.myapplicationf.Models.Alertas;
import com.example.myapplicationf.Utils.NotificacionHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private String apiKey = "AIzaSyAbiXfffKiKJMOIfeD4A2RQaPq_Vuq4Vec"; // API Key ya integrada
    private String idiomaSeleccionado = "es"; // idioma por defecto

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Inicializar Firestore
        db = FirebaseFirestore.getInstance();

        // Inicializar FusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Crear canal de notificaciones
        NotificacionHelper.crearCanal(requireContext());

        // Preparar actualización de ubicación en tiempo real
        prepararActualizacionUbicacion();

        // Obtener SupportMapFragment
        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Spinner para seleccionar idioma
        Spinner spinner = root.findViewById(R.id.spinnerIdiomas);
        String[] idiomas = {"Español", "Inglés"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, idiomas);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: idiomaSeleccionado = "es"; break;
                    case 1: idiomaSeleccionado = "en"; break;
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        return root;
    }

    private void prepararActualizacionUbicacion() {
        LocationRequest locationRequest = new LocationRequest.Builder(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                5000) // 5 segundos
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if(locationResult == null) return;
                Location location = locationResult.getLastLocation();
                double lat = location.getLatitude();
                double lng = location.getLongitude();

                actualizarUbicacion(lat, lng);
                verificarZona(lat, lng);
                mostrarLugaresTuristicos(new LatLng(lat, lng));
            }
        };

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void actualizarUbicacion(double lat, double lng) {
        db.collection("ubicaciones").document("usuario1")
                .update("lat", lat, "lng", lng)
                .addOnSuccessListener(aVoid -> {
                    if(mMap != null){
                        LatLng posicion = new LatLng(lat, lng);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicion, 15));
                    }
                })
                .addOnFailureListener(Throwable::printStackTrace);
    }

    private void verificarZona(double lat, double lng) {
        LatLng zonaSegura = new LatLng(-13.53195, -71.967463);

        float[] distancia = new float[1];
        Location.distanceBetween(lat, lng, zonaSegura.latitude, zonaSegura.longitude, distancia);

        String mensaje;
        if(distancia[0] < 100){
            mensaje = "Has entrado en zona segura";
        } else {
            mensaje = "Zona de riesgo, mantente alerta";
        }

        // 🔹 Aquí aplicamos traducción automática
        traducirTexto(mensaje, idiomaSeleccionado, textoTraducido -> {
            NotificacionHelper.mostrar(requireContext(), textoTraducido);
            db.collection("alertas").add(new Alertas(textoTraducido, System.currentTimeMillis()));
        });
    }

    private void dibujarZonasDesdeFirestore() {
        if(mMap == null) return;

        mMap.clear();

        db.collection("reportes").addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;

            for (var doc : snapshots.getDocuments()) {
                double lat = doc.getDouble("lat");
                double lng = doc.getDouble("lng");
                int riesgo = doc.getLong("riesgo") != null ? doc.getLong("riesgo").intValue() : 1;

                int color;
                switch (riesgo) {
                    case 1: color = Color.parseColor("#5533FF33"); break;
                    case 2: color = Color.parseColor("#55FFA500"); break;
                    default: color = Color.parseColor("#55FF3333"); break;
                }

                LatLng posicion = new LatLng(lat, lng);
                mMap.addCircle(new CircleOptions()
                        .center(posicion)
                        .radius(100)
                        .strokeColor(color)
                        .fillColor(color)
                );
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        FrameLayout mapContainer = getView().findViewById(R.id.mapContainer);
        View legendView = LayoutInflater.from(getContext()).inflate(R.layout.legend_layout, mapContainer, false);
        mapContainer.addView(legendView);

        mMap = googleMap;

        LatLng cusco = new LatLng(-13.53195, -71.967463);
        mMap.addMarker(new MarkerOptions().position(cusco).title("Cusco Default"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cusco, 15));

        dibujarZonasDesdeFirestore();
    }

    // 🔹 Mostrar lugares turísticos cercanos
    private void mostrarLugaresTuristicos(LatLng location) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + location.latitude + "," + location.longitude +
                "&radius=2000&type=tourist_attraction&key=" + apiKey;

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray results = response.getJSONArray("results");
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject place = results.getJSONObject(i);
                            String name = place.getString("name");
                            JSONObject geometry = place.getJSONObject("geometry").getJSONObject("location");
                            double lat = geometry.getDouble("lat");
                            double lng = geometry.getDouble("lng");

                            LatLng pos = new LatLng(lat, lng);
                            mMap.addMarker(new MarkerOptions().position(pos).title(name));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> error.printStackTrace()
        );

        queue.add(request);
    }

    // 🔹 Método de traducción usando Google Translation API
    private void traducirTexto(String texto, String idiomaDestino, TranslationCallback callback) {
        try {
            // Aquí simulamos traducción. En producción deberías llamar a tu backend
            // que use Google Cloud Translation API con la API Key.
            // Ejemplo de llamada real con Retrofit/Volley.
            callback.onTranslated(texto); // por ahora devuelve igual
        } catch (Exception e) {
            e.printStackTrace();
            callback.onTranslated(texto);
        }
    }

    interface TranslationCallback {
        void onTranslated(String textoTraducido);
    }
}
