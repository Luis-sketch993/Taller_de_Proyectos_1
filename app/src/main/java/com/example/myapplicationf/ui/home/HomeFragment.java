package com.example.myapplicationf.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.graphics.Color;

import com.example.myapplicationf.R;
import com.example.myapplicationf.Models.Reporte;
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

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

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

                // Actualizar ubicación en Firestore
                actualizarUbicacion(lat, lng);

                // Guardar alerta en función de la zona
                verificarZona(lat, lng);
            }
        };

        // Verificar permisos
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
        // Ejemplo: zona segura fija (puedes reemplazar con zonas de Firestore si quieres)
        LatLng zonaSegura = new LatLng(-13.53195, -71.967463);

        float[] distancia = new float[1];
        Location.distanceBetween(lat, lng, zonaSegura.latitude, zonaSegura.longitude, distancia);

        String mensaje;
        if(distancia[0] < 100){
            mensaje = "Has entrado en zona segura";
        } else {
            mensaje = "Zona de riesgo, mantente alerta";
        }

        // Notificación local
        NotificacionHelper.mostrar(requireContext(), mensaje);

        // Guardar alerta en Firestore
        db.collection("alertas").add(new Alertas(mensaje, System.currentTimeMillis()));
    }

    private void dibujarZonasDesdeFirestore() {
        if(mMap == null) return;

        mMap.clear(); // Limpiar círculos anteriores

        db.collection("reportes").addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;

            for (var doc : snapshots.getDocuments()) {
                double lat = doc.getDouble("lat");
                double lng = doc.getDouble("lng");
                int riesgo = doc.getLong("riesgo") != null ? doc.getLong("riesgo").intValue() : 1;

                int color;
                switch (riesgo) {
                    case 1: color = Color.parseColor("#5533FF33"); break; // Verde
                    case 2: color = Color.parseColor("#55FFA500"); break; // Naranja
                    default: color = Color.parseColor("#55FF3333"); break; // Rojo
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

        // Posición inicial
        LatLng cusco = new LatLng(-13.53195, -71.967463);
        mMap.addMarker(new MarkerOptions().position(cusco).title("Cusco Default"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cusco, 15));

        // Dibujar zonas dinámicamente desde Firestore
        dibujarZonasDesdeFirestore();

        // Click en mapa para reportes
        mMap.setOnMapClickListener(latLng -> {
            Reporte reporte = new Reporte(latLng.latitude, latLng.longitude, "Reportado por usuario");

            db.collection("reportes").add(reporte);

            mMap.addMarker(new MarkerOptions().position(latLng).title("Reporte enviado"));
            NotificacionHelper.mostrar(requireContext(),
                    "Has reportado una zona: " + latLng.latitude + ", " + latLng.longitude);
        });
    }
}
