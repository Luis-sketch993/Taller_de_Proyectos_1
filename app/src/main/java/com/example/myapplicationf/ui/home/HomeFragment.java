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
import com.example.myapplicationf.Models.Reporte;
import com.example.myapplicationf.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "RutaSeguraDebug";

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private PlacesClient placesClient;

    private AutoCompleteTextView etOrigen, etDestino;
    private LatLng origenLatLng, destinoLatLng;
    private List<Reporte> listaDeReportes = new ArrayList<>();
    private TextView tvTiempo;

    // --- 🔹 CAMBIO CLAVE: Listas para gestionar elementos del mapa sin usar mMap.clear() ---
    private List<Polyline> polylinesActuales = new ArrayList<>();
    private List<Circle> circleReportesActuales = new ArrayList<>();
    private List<Marker> markersActuales = new ArrayList<>();

    private String modoTransporte = "driving";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (!Places.isInitialized()) {
            String apiKey = getApiKey();
            if (apiKey.isEmpty()) {
                Toast.makeText(getContext(), "API Key de Google Maps no encontrada.", Toast.LENGTH_LONG).show();
            } else {
                Places.initialize(requireContext(), apiKey, Locale.getDefault());
            }
        }
        placesClient = Places.createClient(requireContext());

        etOrigen = root.findViewById(R.id.etOrigen);
        etDestino = root.findViewById(R.id.etDestino);
        tvTiempo = root.findViewById(R.id.tvTiempo);
        Button btnCalcularRuta = root.findViewById(R.id.btnCalcularRuta);
        Spinner spinnerModo = root.findViewById(R.id.spinnerModo);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setAutocomplete(etOrigen, true);
        setAutocomplete(etDestino, false);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.modos_transporte, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModo.setAdapter(adapter);
        spinnerModo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: modoTransporte = "driving"; break;
                    case 1: modoTransporte = "walking"; break;
                    case 2: modoTransporte = "bicycling"; break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCalcularRuta.setOnClickListener(v -> {
            Log.d(TAG, "Botón 'Calcular Ruta' presionado.");
            if (origenLatLng != null && destinoLatLng != null) {
                Log.d(TAG, "Origen y Destino válidos. Procediendo a calcular.");
                limpiarRutasYMarcadores();

                Marker origenMarker = mMap.addMarker(new MarkerOptions().position(origenLatLng).title("Origen"));
                Marker destinoMarker = mMap.addMarker(new MarkerOptions().position(destinoLatLng).title("Destino"));
                markersActuales.add(origenMarker);
                markersActuales.add(destinoMarker);

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origenLatLng, 15));
                calcularRutas(origenLatLng, destinoLatLng);
            } else {
                Log.e(TAG, "Error: Origen o Destino son nulos.");
                Toast.makeText(requireContext(), "Selecciona un origen y destino válidos de la lista", Toast.LENGTH_LONG).show();
            }
        });

        return root;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng cusco = new LatLng(-13.53195, -71.967463);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cusco, 13));

        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        // --- 🔹 CAMBIO CLAVE: Hacer las rutas clickables ---
        mMap.setOnPolylineClickListener(polyline -> {
            if (polyline.getTag() != null) {
                Toast.makeText(getContext(), polyline.getTag().toString(), Toast.LENGTH_LONG).show();
            }
        });

        cargarReportesDesdeFirestore();

        mMap.setOnMapClickListener(latLng -> {
            String nombreLugar = "Ubicación desconocida";
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    nombreLugar = addresses.get(0).getAddressLine(0);
                }
            } catch (Exception e) { e.printStackTrace(); }

            Bundle bundle = new Bundle();
            bundle.putDouble("lat", latLng.latitude);
            bundle.putDouble("lng", latLng.longitude);
            bundle.putString("nombreLugar", nombreLugar);

            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_contenido_general);
            navController.navigate(R.id.nav_gallery, bundle);
        });
    }

    private void setAutocomplete(AutoCompleteTextView editText, boolean esOrigen) {
        editText.setThreshold(1);
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().isEmpty()) return;

                FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(s.toString()).setSessionToken(AutocompleteSessionToken.newInstance()).setCountries("PE").build();

                placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
                    List<String> sugerencias = new ArrayList<>();
                    List<String> placeIds = new ArrayList<>();
                    for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                        sugerencias.add(prediction.getFullText(null).toString());
                        placeIds.add(prediction.getPlaceId());
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sugerencias);
                    editText.setAdapter(adapter);
                    adapter.notifyDataSetChanged();

                    editText.setOnItemClickListener((parent, view, position, id) -> {
                        String placeId = placeIds.get(position);
                        List<Place.Field> fields = Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME);
                        FetchPlaceRequest placeRequest = FetchPlaceRequest.newInstance(placeId, fields);

                        placesClient.fetchPlace(placeRequest).addOnSuccessListener(fetchResponse -> {
                            Place place = fetchResponse.getPlace();
                            if (esOrigen) {
                                origenLatLng = place.getLatLng();
                                Log.d(TAG, "Origen fijado: " + place.getName() + " -> " + origenLatLng);
                                Toast.makeText(getContext(), "Origen fijado: " + place.getName(), Toast.LENGTH_SHORT).show();
                            } else {
                                destinoLatLng = place.getLatLng();
                                Log.d(TAG, "Destino fijado: " + place.getName() + " -> " + destinoLatLng);
                                Toast.makeText(getContext(), "Destino fijado: " + place.getName(), Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "Error al obtener detalles del lugar", e);
                            Toast.makeText(getContext(), "Error al obtener lugar: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    });
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error en autocompletado", e);
                    Toast.makeText(getContext(), "Error de autocompletado: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void cargarReportesDesdeFirestore() {
        db.collection("reportes").addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Error al escuchar reportes de Firestore.", e);
                return;
            }
            listaDeReportes.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                listaDeReportes.add(doc.toObject(Reporte.class));
            }
            Log.d(TAG, "Reportes cargados: " + listaDeReportes.size() + ". Redibujando en mapa.");
            if (mMap != null) {
                dibujarReportesEnMapa();
            }
        });
    }

    private void dibujarReportesEnMapa() {
        if (mMap == null) return;

        for (Circle circle : circleReportesActuales) {
            circle.remove();
        }
        circleReportesActuales.clear();

        Log.d(TAG, "Dibujando " + listaDeReportes.size() + " reportes en el mapa.");
        for (Reporte reporte : listaDeReportes) {
            int color;
            switch (reporte.getRiesgo()) {
                case 1: color = Color.parseColor("#5533FF33"); break;
                case 2: color = Color.parseColor("#55FFA500"); break;
                default: color = Color.parseColor("#55FF3333"); break;
            }
            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(reporte.getLat(), reporte.getLng()))
                    .radius(75)
                    .strokeWidth(0)
                    .fillColor(color));
            circleReportesActuales.add(circle);
        }
    }

    private void calcularRutas(LatLng origen, LatLng destino) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origen.latitude + "," + origen.longitude +
                "&destination=" + destino.latitude + "," + destino.longitude +
                "&mode=" + modoTransporte +
                "&alternatives=true" +
                "&key=" + getApiKey();

        Log.d(TAG, "URL de Directions API: " + url);

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    Log.d(TAG, "Respuesta de Directions API: " + response.toString());
                    procesarRespuestaDeRutas(response);
                },
                error -> {
                    Log.e(TAG, "Error en la petición a Directions API", error);
                    Toast.makeText(getContext(), "Error al obtener rutas: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
        );
        queue.add(request);
    }

    private void procesarRespuestaDeRutas(JSONObject response) {
        try {
            String status = response.getString("status");
            Log.d(TAG, "Estado de la respuesta: " + status);

            if (!status.equals("OK")) {
                String errorMsg = response.optString("error_message", "Causa desconocida.");
                Toast.makeText(getContext(), "No se encontraron rutas. Estado: " + status + ". Razón: " + errorMsg, Toast.LENGTH_LONG).show();
                return;
            }

            JSONArray routes = response.getJSONArray("routes");
            Log.d(TAG, "Número de rutas encontradas: " + routes.length());
            if (routes.length() == 0) {
                Toast.makeText(getContext(), "No se encontraron rutas.", Toast.LENGTH_SHORT).show();
                return;
            }

            int mejorPuntajeRiesgo = Integer.MAX_VALUE;
            int rutaMasSeguraIndex = -1;
            int rutaMasCortaIndex = 0;
            long menorDistancia = Long.MAX_VALUE;

            for (int i = 0; i < routes.length(); i++) {
                JSONObject route = routes.getJSONObject(i);
                JSONObject leg = route.getJSONArray("legs").getJSONObject(0);
                long distanciaActual = leg.getJSONObject("distance").getLong("value");

                if (distanciaActual < menorDistancia) {
                    menorDistancia = distanciaActual;
                    rutaMasCortaIndex = i;
                }

                String polyline = route.getJSONObject("overview_polyline").getString("points");
                List<LatLng> puntos = decodePolyline(polyline);
                int puntajeRiesgo = 0;
                for (LatLng punto : puntos) {
                    for (Reporte reporte : listaDeReportes) {
                        float[] distancia = new float[1];
                        Location.distanceBetween(punto.latitude, punto.longitude, reporte.getLat(), reporte.getLng(), distancia);
                        if (distancia[0] < 75) {
                            if (reporte.getRiesgo() == 2) puntajeRiesgo += 1;
                            if (reporte.getRiesgo() == 3) puntajeRiesgo += 5;
                        }
                    }
                }
                Log.d(TAG, "Ruta " + i + " - Puntaje de riesgo: " + puntajeRiesgo);

                if (puntajeRiesgo < mejorPuntajeRiesgo) {
                    mejorPuntajeRiesgo = puntajeRiesgo;
                    rutaMasSeguraIndex = i;
                }
            }

            Log.d(TAG, "Ruta más corta: índice " + rutaMasCortaIndex + ". Ruta más segura: índice " + rutaMasSeguraIndex);

            String tiempoRutaCorta = routes.getJSONObject(rutaMasCortaIndex).getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getString("text");
            String tiempoRutaSegura = routes.getJSONObject(rutaMasSeguraIndex).getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getString("text");

            // --- 🔹 CAMBIO CLAVE: Lógica mejorada para mostrar las opciones ---
            if (rutaMasCortaIndex == rutaMasSeguraIndex) {
                tvTiempo.setText("Ruta Óptima (corta y segura): " + tiempoRutaCorta);
                String polyline = routes.getJSONObject(rutaMasCortaIndex).getJSONObject("overview_polyline").getString("points");
                String tagUnica = "Ruta Óptima: " + tiempoRutaCorta + " (Riesgo: " + mejorPuntajeRiesgo + ")";
                dibujarRuta(decodePolyline(polyline), Color.GREEN, 20, tagUnica);
            } else {
                tvTiempo.setText("Comparando Rutas | Azul: Más Corta (" + tiempoRutaCorta + ") | Verde: Más Segura (" + tiempoRutaSegura + ")");

                String polylineCorta = routes.getJSONObject(rutaMasCortaIndex).getJSONObject("overview_polyline").getString("points");
                String tagCorta = "Ruta más corta: " + tiempoRutaCorta;
                dibujarRuta(decodePolyline(polylineCorta), Color.BLUE, 15, tagCorta);

                String polylineSegura = routes.getJSONObject(rutaMasSeguraIndex).getJSONObject("overview_polyline").getString("points");
                String tagSegura = "Ruta más segura: " + tiempoRutaSegura + " (Riesgo: " + mejorPuntajeRiesgo + ")";
                dibujarRuta(decodePolyline(polylineSegura), Color.GREEN, 20, tagSegura);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error al procesar la respuesta de las rutas", e);
            Toast.makeText(getContext(), "Error procesando las rutas", Toast.LENGTH_SHORT).show();
        }
    }

    private void dibujarRuta(List<LatLng> puntos, int color, float ancho, String tag) {
        if (mMap == null) return;
        Log.d(TAG, "Dibujando ruta con tag: " + tag);
        Polyline polyline = mMap.addPolyline(new PolylineOptions()
                .addAll(puntos)
                .color(color)
                .width(ancho)
                .clickable(true)); // Hacemos la ruta clickable
        polyline.setTag(tag); // Guardamos la información en la ruta
        polylinesActuales.add(polyline);
    }

    private void limpiarRutasYMarcadores() {
        if (mMap == null) return;
        for (Polyline polyline : polylinesActuales) {
            polyline.remove();
        }
        polylinesActuales.clear();

        for (Marker marker : markersActuales) {
            marker.remove();
        }
        markersActuales.clear();
    }

    private String getApiKey() {
        try {
            return requireActivity().getPackageManager().getApplicationInfo(requireActivity().getPackageName(), PackageManager.GET_META_DATA).metaData.getString("com.google.android.geo.API_KEY");
        } catch (Exception e) {
            Log.e("ApiKey", "No se pudo leer la API Key del Manifest", e);
            return "";
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
            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
}
