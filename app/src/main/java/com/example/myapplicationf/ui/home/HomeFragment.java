package com.example.myapplicationf.ui.home;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.content.Intent;
import android.os.Handler;



import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.myapplicationf.ContenidoGeneral;
import com.example.myapplicationf.Models.Reporte;
import com.example.myapplicationf.R;
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

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private PlacesClient placesClient;

    private AutoCompleteTextView etOrigen, etDestino;
    private LatLng origenLatLng, destinoLatLng;
    private List<Reporte> listaDeReportes = new ArrayList<>();
    private TextView tvTiempo;
    private Button btnCalcularRuta;

    private List<Polyline> polylinesActuales = new ArrayList<>();
    private List<Circle> circleReportesActuales = new ArrayList<>();
    private List<Marker> markersActuales = new ArrayList<>();

    private String modoTransporte = "driving";
    private String idiomaSeleccionado = "es";
    private Button btnCompartir, btnGuardarRuta;
    private FusedLocationProviderClient fusedLocationClient;
    private Handler handler = new Handler();
    private Runnable runnable;
    private long intervaloEnvio = 10000; // 10 segundos



    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getApiKey(), Locale.getDefault());
        }
        placesClient = Places.createClient(requireContext());

        etOrigen = root.findViewById(R.id.etOrigen);
        etDestino = root.findViewById(R.id.etDestino);
        tvTiempo = root.findViewById(R.id.tvTiempo);
        btnCalcularRuta = root.findViewById(R.id.btnCalcularRuta);
        btnCompartir = root.findViewById(R.id.btnCompartir);
        btnGuardarRuta = root.findViewById(R.id.btnGuardarRuta);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());



        Spinner spinnerModo = root.findViewById(R.id.spinnerModo);
        Spinner spinnerIdiomas = root.findViewById(R.id.spinnerIdiomas);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setAutocomplete(etOrigen, true);
        setAutocomplete(etDestino, false);

        ArrayAdapter<CharSequence> adapterModo = ArrayAdapter.createFromResource(requireContext(), R.array.modos_transporte, android.R.layout.simple_spinner_item);
        adapterModo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModo.setAdapter(adapterModo);
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

        ArrayAdapter<CharSequence> adapterIdioma = ArrayAdapter.createFromResource(requireContext(), R.array.idiomas_array, android.R.layout.simple_spinner_item);
        adapterIdioma.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIdiomas.setAdapter(adapterIdioma);

        // Pre-select the spinner with the current language
        String currentLang = Locale.getDefault().getLanguage();
        if (currentLang.equals("en")) {
            spinnerIdiomas.setSelection(1);
        } else {
            spinnerIdiomas.setSelection(0);
        }

        spinnerIdiomas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String lang = (position == 0) ? "es" : "en";
                // Only trigger the change if the selected language is different
                if (!Locale.getDefault().getLanguage().equals(lang)) {
                    if (getActivity() instanceof ContenidoGeneral) {
                        ((ContenidoGeneral) getActivity()).setLocale(lang);
                    }
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnCalcularRuta.setOnClickListener(v -> {
            if (origenLatLng != null && destinoLatLng != null) {
                limpiarRutasYMarcadores();
                Marker origenMarker = mMap.addMarker(new MarkerOptions().position(origenLatLng).title(getString(R.string.hint_origen)));
                Marker destinoMarker = mMap.addMarker(new MarkerOptions().position(destinoLatLng).title(getString(R.string.hint_destino)));
                markersActuales.add(origenMarker);
                markersActuales.add(destinoMarker);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origenLatLng, 15));
                calcularRutas(origenLatLng, destinoLatLng);
            } else {
                Toast.makeText(requireContext(), getString(R.string.seleccion_valida), Toast.LENGTH_LONG).show();
            }
        });

        btnCompartir.setOnClickListener(v -> {
            if (origenLatLng != null && destinoLatLng != null) {
                // Generar enlace de Google Maps con ruta
                String urlMaps = "https://www.google.com/maps/dir/?api=1" +
                        "&origin=" + origenLatLng.latitude + "," + origenLatLng.longitude +
                        "&destination=" + destinoLatLng.latitude + "," + destinoLatLng.longitude +
                        "&travelmode=" + modoTransporte; // driving, walking, bicycling

                // Crear intent para WhatsApp (puedes cambiar a SMS si quieres)
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.setPackage("com.whatsapp"); // eliminar esta línea para usar cualquier app de mensajería
                intent.putExtra(Intent.EXTRA_TEXT, "Mira esta ruta: " + urlMaps);

                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(requireContext(), "WhatsApp no está instalado.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "Selecciona origen y destino primero", Toast.LENGTH_SHORT).show();
            }
        });

        btnGuardarRuta.setOnClickListener(v -> {
            if (origenLatLng != null && destinoLatLng != null) {
                // Aquí puedes guardar la ruta en Firestore o local
                Toast.makeText(requireContext(), "Ruta guardada exitosamente", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Selecciona origen y destino primero", Toast.LENGTH_SHORT).show();
            }
        });


        actualizarTextosUI();
        return root;
    }

    private void iniciarEnvioUbicacion() {
        runnable = new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    return;
                }

                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        // Enviar ubicación actual
                        String mensaje = "Ubicación actual: " + location.getLatitude() + "," + location.getLongitude();

                        // Aquí puedes enviar vía Intent, Firestore, o lo que necesites
                        Toast.makeText(requireContext(), "Enviando: " + mensaje, Toast.LENGTH_SHORT).show();
                    }
                });

                handler.postDelayed(this, intervaloEnvio);
            }
        };
        handler.post(runnable);
    }

    private void detenerEnvioUbicacion() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }


    private void actualizarTextosUI() {
        etOrigen.setHint(getString(R.string.hint_origen));
        etDestino.setHint(getString(R.string.hint_destino));
        btnCalcularRuta.setText(getString(R.string.boton_calcular_ruta));
        tvTiempo.setText(getString(R.string.tiempo_estimado_inicial));
        idiomaSeleccionado = Locale.getDefault().getLanguage();
    }

    @Override
    public void onResume() {
        super.onResume();
        iniciarEnvioUbicacion();
    }

    @Override
    public void onPause() {
        super.onPause();
        detenerEnvioUbicacion();
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

        mMap.setOnPolylineClickListener(polyline -> {
            if (polyline.getTag() != null) {
                Toast.makeText(getContext(), polyline.getTag().toString(), Toast.LENGTH_LONG).show();
            }
        });

        cargarReportesDesdeFirestore();

        mMap.setOnMapClickListener(latLng -> {
            String nombreLugar = getString(R.string.ubicacion_desconocida);
            try {
                Geocoder geocoder = new Geocoder(requireContext(), new Locale(idiomaSeleccionado));
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
                            String toastMessage;
                            if (esOrigen) {
                                origenLatLng = place.getLatLng();
                                toastMessage = getString(R.string.origen_fijado, place.getName());
                            } else {
                                destinoLatLng = place.getLatLng();
                                toastMessage = getString(R.string.destino_fijado, place.getName());
                            }
                            Toast.makeText(getContext(), toastMessage, Toast.LENGTH_SHORT).show();
                        });
                    });
                });
            }
        });
    }

    private void cargarReportesDesdeFirestore() {
        db.collection("reportes").addSnapshotListener((snapshots, e) -> {
            if (e != null) { return; }
            listaDeReportes.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                listaDeReportes.add(doc.toObject(Reporte.class));
            }
            if (mMap != null) { dibujarReportesEnMapa(); }
        });
    }

    private void dibujarReportesEnMapa() {
        if (mMap == null) return;
        for (Circle circle : circleReportesActuales) { circle.remove(); }
        circleReportesActuales.clear();
        for (Reporte reporte : listaDeReportes) {
            int color;
            switch (reporte.getRiesgo()) {
                case 1: color = Color.parseColor("#5533FF33"); break;
                case 2: color = Color.parseColor("#55FFA500"); break;
                default: color = Color.parseColor("#55FF3333"); break;
            }
            Circle circle = mMap.addCircle(new CircleOptions().center(new LatLng(reporte.getLat(), reporte.getLng())).radius(75).strokeWidth(0).fillColor(color));
            circleReportesActuales.add(circle);
        }
    }

    private void calcularRutas(LatLng origen, LatLng destino) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origen.latitude + "," + origen.longitude +
                "&destination=" + destino.latitude + "," + destino.longitude +
                "&mode=" + modoTransporte +
                "&alternatives=true" +
                "&language=" + idiomaSeleccionado +
                "&key=" + getApiKey();

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, this::procesarRespuestaDeRutas,
                error -> Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show());
        queue.add(request);
    }

    private void procesarRespuestaDeRutas(JSONObject response) {
        try {
            if (!response.getString("status").equals("OK")) {
                Toast.makeText(getContext(), getString(R.string.no_se_encontraron_rutas), Toast.LENGTH_SHORT).show();
                return;
            }

            JSONArray routes = response.getJSONArray("routes");
            if (routes.length() == 0) {
                Toast.makeText(getContext(), getString(R.string.no_se_encontraron_rutas), Toast.LENGTH_SHORT).show();
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
                List<LatLng> puntos = decodePolyline(route.getJSONObject("overview_polyline").getString("points"));
                int puntajeRiesgo = calcularPuntajeRiesgo(puntos);
                if (puntajeRiesgo < mejorPuntajeRiesgo) {
                    mejorPuntajeRiesgo = puntajeRiesgo;
                    rutaMasSeguraIndex = i;
                }
            }

            String tiempoRutaCorta = routes.getJSONObject(rutaMasCortaIndex).getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getString("text");
            String tiempoRutaSegura = routes.getJSONObject(rutaMasSeguraIndex).getJSONArray("legs").getJSONObject(0).getJSONObject("duration").getString("text");

            if (rutaMasCortaIndex == rutaMasSeguraIndex) {
                tvTiempo.setText(getString(R.string.ruta_optima, tiempoRutaCorta));
                String polyline = routes.getJSONObject(rutaMasCortaIndex).getJSONObject("overview_polyline").getString("points");
                String tagUnica = getString(R.string.tag_ruta_optima, tiempoRutaCorta, mejorPuntajeRiesgo);
                dibujarRuta(decodePolyline(polyline), Color.GREEN, 20, tagUnica);
            } else {
                tvTiempo.setText(getString(R.string.comparando_rutas, tiempoRutaCorta, tiempoRutaSegura));
                String polylineCorta = routes.getJSONObject(rutaMasCortaIndex).getJSONObject("overview_polyline").getString("points");
                int puntajeRiesgoCorta = calcularPuntajeRiesgo(decodePolyline(polylineCorta));
                String tagCorta = getString(R.string.tag_ruta_corta, tiempoRutaCorta, puntajeRiesgoCorta);
                dibujarRuta(decodePolyline(polylineCorta), Color.BLUE, 22, tagCorta);
                String polylineSegura = routes.getJSONObject(rutaMasSeguraIndex).getJSONObject("overview_polyline").getString("points");
                String tagSegura = getString(R.string.tag_ruta_segura, tiempoRutaSegura, mejorPuntajeRiesgo);
                dibujarRuta(decodePolyline(polylineSegura), Color.GREEN, 15, tagSegura);
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error procesando las rutas", Toast.LENGTH_SHORT).show();
        }
    }

    private int calcularPuntajeRiesgo(List<LatLng> puntos) {
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
        return puntajeRiesgo;
    }

    private void dibujarRuta(List<LatLng> puntos, int color, float ancho, String tag) {
        if (mMap == null) return;
        Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(puntos).color(color).width(ancho).clickable(true));
        polyline.setTag(tag);
        polylinesActuales.add(polyline);
    }

    private void limpiarRutasYMarcadores() {
        if (mMap == null) return;
        for (Polyline polyline : polylinesActuales) { polyline.remove(); }
        polylinesActuales.clear();
        for (Marker marker : markersActuales) { marker.remove(); }
        markersActuales.clear();
    }

    private String getApiKey() {
        try {
            return requireActivity().getPackageManager().getApplicationInfo(requireActivity().getPackageName(), PackageManager.GET_META_DATA).metaData.getString("com.google.android.geo.API_KEY");
        } catch (Exception e) { return ""; }
    }

    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0; result = 0;
            do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }
        return poly;
    }
}

