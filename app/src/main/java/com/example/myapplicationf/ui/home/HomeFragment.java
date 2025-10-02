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

import java.util.List;
import java.util.Locale;

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
        LatLng zonaSegura = new LatLng(-13.53195, -71.967463);

        float[] distancia = new float[1];
        Location.distanceBetween(lat, lng, zonaSegura.latitude, zonaSegura.longitude, distancia);

        String mensaje;
        if(distancia[0] < 100){
            mensaje = "Has entrado en zona segura";
        } else {
            mensaje = "Zona de riesgo, mantente alerta";
        }

        NotificacionHelper.mostrar(requireContext(), mensaje);

        db.collection("alertas").add(new Alertas(mensaje, System.currentTimeMillis()));
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

        // 🔹 Click en mapa para seleccionar ubicación
        mMap.setOnMapClickListener(latLng -> {
            Bundle bundle = new Bundle();
            bundle.putDouble("lat", latLng.latitude);
            bundle.putDouble("lng", latLng.longitude);

            // 🔹 Obtener nombre del lugar con Geocoder
            String nombreLugar = "Ubicación seleccionada";
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> direcciones = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (direcciones != null && !direcciones.isEmpty()) {
                    Address dir = direcciones.get(0);

                    // ✅ Forzar dirección legible (sin plus code)
                    if (dir.getThoroughfare() != null) {
                        nombreLugar = dir.getThoroughfare(); // Ejemplo: "Av. Jorge Chavez"
                        if (dir.getSubThoroughfare() != null) {
                            nombreLugar += " " + dir.getSubThoroughfare(); // Ejemplo: "Av. Jorge Chavez 123"
                        }
                    } else if (dir.getAddressLine(0) != null) {
                        nombreLugar = dir.getAddressLine(0); // Dirección completa
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            bundle.putString("nombreLugar", nombreLugar);

            NavController navController = Navigation.findNavController(requireActivity(),
                    R.id.nav_host_fragment_content_contenido_general);
            navController.navigate(R.id.nav_gallery, bundle);
        });
    }
}
