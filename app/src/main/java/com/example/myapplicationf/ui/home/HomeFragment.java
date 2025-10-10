package com.example.myapplicationf.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.myapplicationf.Models.Alertas;
import com.example.myapplicationf.Models.Reporte;
import com.example.myapplicationf.R;
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
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private PlacesClient placesClient;

    // --- 🔹 NUEVAS VARIABLES PARA RUTAS MÚLTIPLES ---
    private List<Reporte> listaDeReportes = new ArrayList<>();
    private List<Polyline> polylinesEnMapa = new ArrayList<>();
    private static final double RADIO_DE_RIESGO = 75; // en metros

    // --- Variables de UI y de estado ---
    private AutoCompleteTextView etOrigen, etDestino;
    private TextView tvTiempo;
    private LatLng origenLatLng, destinoLatLng;
    private String modoTransporte = "walking";
    private String idiomaSeleccionado = "es";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        NotificacionHelper.crearCanal(requireContext());

        // Inicializar Places (usando la API Key del Manifest)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getApiKeyFromManifest());
        }
        placesClient = Places.createClient(requireContext());

        // Configuración de Vistas
        inicializarVistas(root);
        prepararActualizacionUbicacion();

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return root;
    }

    private void inicializarVistas(View root) {
        tvTiempo = root.findViewById(R.id.tvTiempo);
        etOrigen = root.findViewById(R.id.etOrigen);
        etDestino = root.findViewById(R.id.etDestino);
        Button btnCalcularRuta = root.findViewById(R.id.btnCalcularRuta);

        setAutocomplete(etOrigen, true);
        setAutocomplete(etDestino, false);

        // Configuración de Spinners (Modo, Filtro, Idioma)
        setupSpinners(root);

        btnCalcularRuta.setOnClickListener(v -> {
            if (origenLatLng != null && destinoLatLng != null) {
                mMap.clear(); // Limpia todo (marcadores, círculos, polylines)
                dibujarZonasDeRiesgo(); // Vuelve a dibujar los círculos de riesgo
                calcularRutas();
            } else {
                Toast.makeText(requireContext(), "Selecciona origen y destino válidos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng cusco = new LatLng(-13.53195, -71.967463);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cusco, 15));

        // Listener para reportar (al hacer click en el mapa)
        mMap.setOnMapClickListener(this::abrirFormularioDeReporte);

        // 🔹 Carga inicial de los reportes para el análisis de rutas
        cargarReportesDesdeFirestore();
    }

    /**
     * 🔹 PASO 1: Cargar todos los reportes de Firestore y guardarlos en una lista local.
     * Se usa addSnapshotListener para que la lista siempre esté actualizada en tiempo real.
     */
    private void cargarReportesDesdeFirestore() {
        db.collection("reportes").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w("Firestore", "Error al escuchar.", e);
                return;
            }
            if (snapshots == null) return;

            listaDeReportes.clear();
            for (var doc : snapshots.getDocuments()) {
                Reporte reporte = doc.toObject(Reporte.class);
                if (reporte != null) {
                    listaDeReportes.add(reporte);
                }
            }
            Log.d("Firestore", "Lista de reportes actualizada. Total: " + listaDeReportes.size());
            // Una vez cargados, dibujamos las zonas en el mapa
            dibujarZonasDeRiesgo();
        });
    }

    /**
     * Dibuja los círculos de riesgo en el mapa basándose en la lista local de reportes.
     */
    private void dibujarZonasDeRiesgo() {
        if (mMap == null) return;
        // No limpiamos el mapa aquí para no borrar las rutas calculadas

        for (Reporte reporte : listaDeReportes) {
            int color;
            switch (reporte.getRiesgo()) {
                case 1: color = Color.parseColor("#4400FF00"); break; // Verde (Seguro)
                case 2: color = Color.parseColor("#44FFD700"); break; // Amarillo (Moderado)
                default: color = Color.parseColor("#44FF0000"); break; // Rojo (Inseguro)
            }

            LatLng posicion = new LatLng(reporte.getLat(), reporte.getLng());
            mMap.addCircle(new CircleOptions()
                    .center(posicion)
                    .radius(RADIO_DE_RIESGO) // Radio definido globalmente
                    .strokeColor(Color.TRANSPARENT)
                    .fillColor(color)
            );
        }
    }


    /**
     * 🔹 PASO 2: Solicitar a la API de Google TODAS las rutas alternativas.
     */
    private void calcularRutas() {
        limpiarPolylines();
        mMap.addMarker(new MarkerOptions().position(origenLatLng).title("Origen"));
        mMap.addMarker(new MarkerOptions().position(destinoLatLng).title("Destino"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origenLatLng, 14));

        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origenLatLng.latitude + "," + origenLatLng.longitude +
                "&destination=" + destinoLatLng.latitude + "," + destinoLatLng.longitude +
                "&mode=" + modoTransporte +
                "&alternatives=true" +  // ¡La clave! Pedimos rutas alternativas.
                "&language=" + idiomaSeleccionado +
                "&key=" + getApiKeyFromManifest();

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                this::procesarRespuestaDeRutas,
                error -> {
                    error.printStackTrace();
                    tvTiempo.setText("Error al obtener rutas.");
                }
        );
        queue.add(request);
    }

    /**
     * 🔹 PASO 3: Procesar TODAS las rutas, calcular su puntaje de riesgo y dibujarlas.
     */
    private void procesarRespuestaDeRutas(JSONObject response) {
        try {
            JSONArray routes = response.getJSONArray("routes");
            if (routes.length() == 0) {
                tvTiempo.setText("No se encontraron rutas.");
                return;
            }

            List<RutaAnalizada> rutasAnalizadas = new ArrayList<>();

            for (int i = 0; i < routes.length(); i++) {
                JSONObject route = routes.getJSONObject(i);
                JSONObject leg = route.getJSONArray("legs").getJSONObject(0);

                String duration = leg.getJSONObject("duration").getString("text");
                int distance = leg.getJSONObject("distance").getInt("value"); // en metros
                String polylineEncoded = route.getJSONObject("overview_polyline").getString("points");
                List<LatLng> puntosDeRuta = decodePolyline(polylineEncoded);

                int puntajeDeRiesgo = calcularPuntajeDeRiesgo(puntosDeRuta);

                rutasAnalizadas.add(new RutaAnalizada(puntosDeRuta, duration, distance, puntajeDeRiesgo));
            }

            // Ordenar para encontrar la más corta y la más segura
            RutaAnalizada rutaMasCorta = Collections.min(rutasAnalizadas, Comparator.comparingInt(r -> r.distancia));
            RutaAnalizada rutaMasSegura = Collections.min(rutasAnalizadas, Comparator.comparingInt(r -> r.puntajeRiesgo));

            // Dibujar las rutas en el mapa
            dibujarRuta(rutaMasSegura, Color.GREEN, 15f); // Verde y más gruesa para la segura
            dibujarRuta(rutaMasCorta, Color.BLUE, 10f);   // Azul y más delgada para la corta

            // Actualizar UI
            String infoRutas = "Ruta Corta: " + rutaMasCorta.duracion + "\nRuta Segura: " + rutaMasSegura.duracion;
            tvTiempo.setText(infoRutas);

            if (rutaMasCorta.equals(rutaMasSegura)) {
                tvTiempo.setText("La ruta más corta también es la más segura: " + rutaMasCorta.duracion);
                // Si son la misma, dibujamos solo una vez (en verde)
                limpiarPolylines();
                dibujarRuta(rutaMasSegura, Color.GREEN, 15f);
            }

        } catch (Exception e) {
            e.printStackTrace();
            tvTiempo.setText("Error procesando las rutas.");
        }
    }

    /**
     * 🔹 PASO 4: El núcleo de la lógica. Calcula un puntaje de riesgo para una ruta.
     * A mayor puntaje, más peligrosa es la ruta.
     */
    private int calcularPuntajeDeRiesgo(List<LatLng> puntosDeRuta) {
        int puntajeTotal = 0;
        if (listaDeReportes.isEmpty()) {
            return 0; // Si no hay reportes, todas las rutas son seguras.
        }

        for (LatLng punto : puntosDeRuta) {
            for (Reporte reporte : listaDeReportes) {
                LatLng puntoReporte = new LatLng(reporte.getLat(), reporte.getLng());
                float[] distancia = new float[1];
                Location.distanceBetween(punto.latitude, punto.longitude, puntoReporte.latitude, puntoReporte.longitude, distancia);

                if (distancia[0] < RADIO_DE_RIESGO) {
                    // El punto de la ruta está dentro de una zona de riesgo
                    switch (reporte.getRiesgo()) {
                        case 2: puntajeTotal += 1; break; // Penalización por riesgo moderado
                        case 3: puntajeTotal += 5; break; // Penalización mayor por riesgo alto
                    }
                }
            }
        }
        return puntajeTotal;
    }

    private void dibujarRuta(RutaAnalizada ruta, int color, float width) {
        Polyline polyline = mMap.addPolyline(new PolylineOptions()
                .addAll(ruta.puntos)
                .color(color)
                .width(width)
                .clickable(true));
        polylinesEnMapa.add(polyline);
    }

    private void limpiarPolylines() {
        for (Polyline polyline : polylinesEnMapa) {
            polyline.remove();
        }
        polylinesEnMapa.clear();
    }


    // --- Métodos de ayuda y lógica existente (sin cambios mayores) ---

    private void abrirFormularioDeReporte(LatLng latLng) {
        String nombreLugar = "Ubicación desconocida";
        try {
            Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                nombreLugar = addresses.get(0).getAddressLine(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bundle bundle = new Bundle();
        bundle.putDouble("lat", latLng.latitude);
        bundle.putDouble("lng", latLng.longitude);
        bundle.putString("nombreLugar", nombreLugar);

        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_contenido_general);
        navController.navigate(R.id.nav_gallery, bundle);
    }

    private String getApiKeyFromManifest() {
        try {
            return requireActivity().getPackageManager()
                    .getApplicationInfo(requireActivity().getPackageName(), PackageManager.GET_META_DATA)
                    .metaData.getString("com.google.android.geo.API_KEY");
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            poly.add(new LatLng((((double) lat / 1E5)), (((double) lng / 1E5))));
        }
        return poly;
    }

    private void setupSpinners(View root) {
        // Spinner Modo de transporte
        Spinner spinnerModo = root.findViewById(R.id.spinnerModo);
        ArrayAdapter<CharSequence> adapterModo = ArrayAdapter.createFromResource(requireContext(),
                R.array.modos_transporte, android.R.layout.simple_spinner_item);
        adapterModo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModo.setAdapter(adapterModo);
        spinnerModo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: modoTransporte = "walking"; break;
                    case 1: modoTransporte = "bicycling"; break;
                    case 2: modoTransporte = "driving"; break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
        // Aquí puedes configurar los otros spinners (idioma, filtro) si lo necesitas
    }

    private void setAutocomplete(AutoCompleteTextView editText, boolean esOrigen) {
        // Tu código de autocompletado existente va aquí, funciona bien.
        // ...
    }

    private void prepararActualizacionUbicacion() {
        // Tu código de actualización de ubicación en tiempo real va aquí.
        // ...
    }


    /**
     * 🔹 Clase de Ayuda para manejar la información de cada ruta analizada.
     */
    private static class RutaAnalizada {
        final List<LatLng> puntos;
        final String duracion;
        final int distancia; // en metros
        final int puntajeRiesgo;

        RutaAnalizada(List<LatLng> puntos, String duracion, int distancia, int puntajeRiesgo) {
            this.puntos = puntos;
            this.duracion = duracion;
            this.distancia = distancia;
            this.puntajeRiesgo = puntajeRiesgo;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RutaAnalizada that = (RutaAnalizada) o;
            return distancia == that.distancia && puntajeRiesgo == that.puntajeRiesgo;
        }
    }
}
