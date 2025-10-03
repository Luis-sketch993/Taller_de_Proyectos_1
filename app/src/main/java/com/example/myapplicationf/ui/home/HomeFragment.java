package com.example.myapplicationf.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.graphics.Color;
import android.widget.TextView;
import android.widget.Toast;

import com.example.myapplicationf.Models.Reporte;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.CircleOptions;

import com.google.firebase.firestore.FirebaseFirestore;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.gms.common.api.Status;

import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;

import java.util.ArrayList;
import java.util.Arrays;


public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    private Location ultimaUbicacion;

    private String apiKey = "AIzaSyAbiXfffKiKJMOIfeD4A2RQaPq_Vuq4Vec"; // API Key ya integrada
    private String idiomaSeleccionado = "es"; // idioma por defecto

    private Spinner spinnerFiltro;
    private TextView tvTiempo;


    private Marker marcadorOrigen;
    private Marker marcadorDestino;


    private com.google.android.gms.maps.model.Polyline rutaActual;

    private String modoTransporte = "walking"; // por defecto

    private PlacesClient placesClient;
    private AutoCompleteTextView etOrigen, etDestino;
    private LatLng origenLatLng, destinoLatLng;



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


        spinnerFiltro = root.findViewById(R.id.spinnerFiltro);
        tvTiempo = root.findViewById(R.id.tvTiempo);
        Button btnCalcularRuta = root.findViewById(R.id.btnCalcularRuta);

        etOrigen = root.findViewById(R.id.etOrigen);
        etDestino = root.findViewById(R.id.etDestino);

        // Usar autocompletado predefinido
        setAutocompletePredefinido(etOrigen, true);
        setAutocompletePredefinido(etDestino, false);



        AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();


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


        // Spinner Filtro
        String[] filtros = {"Ninguno", "Zonas de riesgo", "Lugares turísticos", "Ambos"};
        ArrayAdapter<String> adapterFiltro = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, filtros);
        adapterFiltro.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltro.setAdapter(adapterFiltro);

        spinnerFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mMap == null || ultimaUbicacion == null) return;

                limpiarRuta();
                switch (position) {
                    case 0:
                        dibujarZonasDesdeFirestore();
                        break;
                    case 1:
                        mostrarLugaresTuristicos(new LatLng(ultimaUbicacion.getLatitude(), ultimaUbicacion.getLongitude()));
                        break;
                    case 2:
                        dibujarZonasDesdeFirestore();
                        mostrarLugaresTuristicos(new LatLng(ultimaUbicacion.getLatitude(), ultimaUbicacion.getLongitude()));
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nada por ahora
            }
        });

        // Spinner Modo de transporte
        Spinner spinnerModo = root.findViewById(R.id.spinnerModo);
        String[] modos = {"A pie", "Bicicleta", "Carro"};
        ArrayAdapter<String> adapterModo = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, modos);
        adapterModo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModo.setAdapter(adapterModo);

        spinnerModo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: modoTransporte = "walking"; break;   //
                    case 1: modoTransporte = "bicycling"; break; //
                    case 2: modoTransporte = "driving"; break;   //
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });


        // Inicializar Places (solo una vez en toda la app)
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), apiKey, Locale.getDefault());

        }
        placesClient = Places.createClient(requireContext());

        // Referencias
                etOrigen = root.findViewById(R.id.etOrigen);
                etDestino = root.findViewById(R.id.etDestino);

        // Activar autocompletado en ambos EditText
                setAutocomplete(etOrigen, true);   // Origen
                setAutocomplete(etDestino, false); // Destino



        btnCalcularRuta.setOnClickListener(v -> {
            if (origenLatLng != null && destinoLatLng != null) {
                // Limpiar mapa antes de dibujar nueva ruta
                limpiarRuta();

                // Marcadores en origen y destino
                mMap.addMarker(new MarkerOptions().position(origenLatLng).title("Origen"));
                mMap.addMarker(new MarkerOptions().position(destinoLatLng).title("Destino"));

                // Centrar cámara en origen
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origenLatLng, 15));

                // Llamar a calcular ruta
                calcularRuta(origenLatLng, destinoLatLng);

            } else {
                Toast.makeText(requireContext(), "Selecciona origen y destino válidos", Toast.LENGTH_SHORT).show();
            }
        });



        return root;
    }


    private void setAutocompletePredefinido(AutoCompleteTextView editText, boolean esOrigen) {
        ArrayAdapter<PlaceItem> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                lugaresPredefinidos
        );
        editText.setAdapter(adapter);
        editText.setThreshold(1);

        editText.setOnItemClickListener((parent, view, position, id) -> {
            PlaceItem seleccionado = (PlaceItem) parent.getItemAtPosition(position);
            if (esOrigen) {
                origenLatLng = seleccionado.latLng;
                Toast.makeText(requireContext(), "Origen: " + seleccionado.nombre, Toast.LENGTH_SHORT).show();
            } else {
                destinoLatLng = seleccionado.latLng;
                Toast.makeText(requireContext(), "Destino: " + seleccionado.nombre, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setAutocomplete(AutoCompleteTextView editText, boolean esOrigen) {
        editText.setThreshold(1); // sugiere desde 1 letra
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().isEmpty()) {
                    AutocompleteSessionToken token = AutocompleteSessionToken.newInstance();

                    FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                            .setQuery(s.toString())
                            .setSessionToken(token)
                            .setCountries("PE") // 🔹 opcional: restringir a Perú
                            .build();

                    placesClient.findAutocompletePredictions(request)
                            .addOnSuccessListener(response -> {
                                List<String> sugerencias = new ArrayList<>();
                                List<String> placeIds = new ArrayList<>();

                                for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                                    sugerencias.add(prediction.getFullText(null).toString());
                                    placeIds.add(prediction.getPlaceId());
                                }

                                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                        requireContext(),
                                        android.R.layout.simple_dropdown_item_1line,
                                        sugerencias
                                );
                                editText.setAdapter(adapter);

                                editText.setOnItemClickListener((parent, view, position, id) -> {
                                    String placeId = placeIds.get(position);

                                    List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
                                    FetchPlaceRequest placeRequest = FetchPlaceRequest.newInstance(placeId, fields);

                                    placesClient.fetchPlace(placeRequest).addOnSuccessListener(fetchResponse -> {
                                        Place place = fetchResponse.getPlace();
                                        if (esOrigen) {
                                            origenLatLng = place.getLatLng();
                                            Toast.makeText(requireContext(), "Origen: " + place.getName(), Toast.LENGTH_SHORT).show();
                                        } else {
                                            destinoLatLng = place.getLatLng();
                                            Toast.makeText(requireContext(), "Destino: " + place.getName(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                });
                            });
                }
            }
        });
    }




    private void calcularRuta(LatLng origen, LatLng destino) {
        // Limpiar ruta anterior
        if (rutaActual != null) {
            rutaActual.remove();
            rutaActual = null;
        }

        // Limpiar marcadores anteriores
        if (marcadorOrigen != null) marcadorOrigen.remove();
        if (marcadorDestino != null) marcadorDestino.remove();

        // Agregar nuevos marcadores
        marcadorOrigen = mMap.addMarker(new MarkerOptions().position(origen).title("Origen"));
        marcadorDestino = mMap.addMarker(new MarkerOptions().position(destino).title("Destino"));

        // Construir URL según modo de transporte
        String url = construirUrlDirecciones(origen, destino, modoTransporte);

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String status = response.getString("status");
                        Log.d("DebugRuta", "Directions API status: " + status);

                        if (!status.equals("OK")) {
                            // Si no hay ruta y el modo era driving, intentar walking
                            if (modoTransporte.equals("driving")) {
                                Log.d("DebugRuta", "No se encontró ruta en driving, intentando walking...");
                                String walkingUrl = construirUrlDirecciones(origen, destino, "walking");
                                JsonObjectRequest walkingRequest = new JsonObjectRequest(Request.Method.GET, walkingUrl, null,
                                        walkingResponse -> procesarRespuestaRuta(walkingResponse, origen, destino),
                                        error -> {
                                            error.printStackTrace();
                                            calcularTiempoAproximado(origen, destino); // fallback si walking también falla
                                        });
                                queue.add(walkingRequest);
                            } else {
                                calcularTiempoAproximado(origen, destino); // mostrar tiempo aproximado
                            }
                            return;
                        }

                        // Procesar la respuesta normalmente
                        procesarRespuestaRuta(response, origen, destino);

                    } catch (Exception e) {
                        e.printStackTrace();
                        calcularTiempoAproximado(origen, destino);
                    }
                },
                error -> {
                    error.printStackTrace();
                    calcularTiempoAproximado(origen, destino);
                }
        );
        queue.add(request);
    }

    // Método para calcular tiempo aproximado
    private void calcularTiempoAproximado(LatLng origen, LatLng destino) {
        float[] distancia = new float[1];
        Location.distanceBetween(
                origen.latitude, origen.longitude,
                destino.latitude, destino.longitude,
                distancia
        );

        double velocidadMps;
        switch (modoTransporte) {
            case "driving":
                velocidadMps = 4.17; // ~15 km/h promedio urbano realista
                break;
            case "bicycling":
                velocidadMps = 2.78; // ~10 km/h
                break;
            default:
                velocidadMps = 1.11; // ~4 km/h caminando
                break;
        }

        // Factor por curvas, semáforos, tráfico
        double factorCurvas = 1.10 + Math.random() * 0.15; // +10% a +25% en carro
        double factorAleatorio = 0.95 + Math.random() * 0.10; // ±5%

        velocidadMps *= factorAleatorio / factorCurvas;

        int tiempoSegundos = (int)(distancia[0] / velocidadMps);
        int horas = tiempoSegundos / 3600;
        int minutos = (tiempoSegundos % 3600) / 60;

        String tiempoTexto = (horas > 0) ? (horas + " h " + minutos + " min") : (minutos + " min");
        tvTiempo.setText("Tiempo estimado aprox.: " + tiempoTexto);
    }


    // Construir URL de Directions
    private String construirUrlDirecciones(LatLng origen, LatLng destino, String modo) {
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origen.latitude + "," + origen.longitude +
                "&destination=" + destino.latitude + "," + destino.longitude +
                "&mode=" + modo +
                "&language=" + idiomaSeleccionado +
                "&key=" + apiKey;
    }

    // Procesar respuesta JSON y dibujar ruta
    private void procesarRespuestaRuta(JSONObject response, LatLng origen, LatLng destino) {
        try {
            JSONArray routes = response.getJSONArray("routes");

            if (routes.length() > 0) {
                JSONObject route = routes.getJSONObject(0);
                JSONObject leg = route.getJSONArray("legs").getJSONObject(0);

                String duration = leg.has("duration") ? leg.getJSONObject("duration").getString("text") : "Tiempo no disponible";
                tvTiempo.setText("Tiempo estimado: " + duration);

                String polyline = route.getJSONObject("overview_polyline").getString("points");
                List<LatLng> puntos = decodePolyline(polyline);

                // Limpiar ruta anterior
                limpiarRuta();
                rutaActual = mMap.addPolyline(new com.google.android.gms.maps.model.PolylineOptions()
                        .addAll(puntos)
                        .color(Color.BLUE)
                        .width(10));

                // Volver a agregar marcadores
                mMap.addMarker(new MarkerOptions().position(origen).title("Origen"));
                mMap.addMarker(new MarkerOptions().position(destino).title("Destino"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origen, 15));
            } else {
                tvTiempo.setText("No se encontró ruta");
            }

        } catch (Exception e) {
            e.printStackTrace();
            tvTiempo.setText("Error procesando ruta");
        }
    }




    // 🔹 Decodificar polyline (Google Directions)
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new java.util.ArrayList<>();
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

            LatLng p = new LatLng(
                    (((double) lat / 1E5)),
                    (((double) lng / 1E5))
            );
            poly.add(p);
        }

        return poly;
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

        limpiarRuta(); // solo elimina la ruta anterior

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
        mMap.setOnMapClickListener(latLng -> {
            Bundle bundle = new Bundle();
            bundle.putDouble("lat", latLng.latitude);
            bundle.putDouble("lng", latLng.longitude);
            bundle.putString("nombreLugar", "Lugar seleccionado en el mapa");

            NavController navController = Navigation.findNavController(
                    requireActivity(),
                    R.id.nav_host_fragment_content_contenido_general
            );
            navController.navigate(R.id.nav_gallery, bundle);
        });


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


    // Lista de destinos preestablecidos (nombre y coordenadas)
    private final List<PlaceItem> lugaresPredefinidos = Arrays.asList(
            new PlaceItem("Plaza de Armas Cusco", new LatLng(-13.5161, -71.9780)),
            new PlaceItem("Sacsayhuamán", new LatLng(-13.5090, -71.9815)),
            new PlaceItem("Qorikancha", new LatLng(-13.5215, -71.9775)),
            new PlaceItem("Barrio de San Blas", new LatLng(-13.5165, -71.9785))
    );

    // Clase auxiliar para manejar nombre y coordenadas
    private static class PlaceItem {
        String nombre;
        LatLng latLng;

        PlaceItem(String nombre, LatLng latLng) {
            this.nombre = nombre;
            this.latLng = latLng;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private void limpiarRuta() {
        if (rutaActual != null) {
            rutaActual.remove();
            rutaActual = null;
        }
    }

    interface TranslationCallback {
        void onTranslated(String textoTraducido);
    }
}
