package com.example.myapplicationf.ui.home;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import android.content.Intent;
import android.os.Build;
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

import com.example.myapplicationf.Utils.Notificaciones_Zonas;
import androidx.core.content.ContextCompat;

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
import com.example.myapplicationf.Models.HistorialRuta;
import com.example.myapplicationf.Models.Reporte;
import com.example.myapplicationf.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private PlacesClient placesClient;

    private AutoCompleteTextView etOrigen, etDestino;
    private LatLng origenLatLng, destinoLatLng;
    private String origenNombre, destinoNombre;
    private List<Reporte> listaDeReportes = new ArrayList<>();
    private TextView tvTiempo;
    private Button btnCalcularRuta;

    private Notificaciones_Zonas notificacionesZonas;
    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    // 🔹 CORRECCIÓN 1 (TYPO) 🔹
    // Se corrigió el nombre de `polylinesActualES` a `polylinesActuales`
    private List<Polyline> polylinesActuales = new ArrayList<>();
    private List<Circle> circleReportesActuales = new ArrayList<>();
    private List<Marker> markersActuales = new ArrayList<>();

    private List<Marker> markerReportesActuales = new ArrayList<>();
    private int filtroRiesgo = 0; // 0=Todos, 1=Seguro, 2=Moderado, 3=Inseguro

    private String modoTransporte = "driving";
    private String idiomaSeleccionado = "es";
    private Button btnCompartir, btnGuardarRuta;
    private FusedLocationProviderClient fusedLocationClient;
    private Handler handler = new Handler();
    private Runnable runnable;
    private long intervaloEnvio;


    private boolean notificacionesActivadas = true;
    private long intervaloNotificacion;
    private long ultimaNotificacion = 0;
    private boolean mostrarZonaTranquila = true;

    private Handler handlerAlertas = new Handler();
    private Runnable runnableAlertas;


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

        // --- Configuración del Spinner de Filtro ---
        Spinner spinnerFiltro = root.findViewById(R.id.spinnerFiltro);
        ArrayAdapter<CharSequence> adapterFiltro = ArrayAdapter.createFromResource(requireContext(),
                R.array.filtro_riesgo_array, android.R.layout.simple_spinner_item);
        adapterFiltro.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiltro.setAdapter(adapterFiltro);

        spinnerFiltro.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filtroRiesgo = position;
                dibujarReportesEnMapa();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                filtroRiesgo = 0;
            }
        });
        // --- Fin Spinner Filtro ---

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
                String urlMaps = "http://googleusercontent.com/maps/google.com/0" +
                        "&origin=" + origenLatLng.latitude + "," + origenLatLng.longitude +
                        "&destination=" + destinoLatLng.latitude + "," + destinoLatLng.longitude +
                        "&travelmode=" + modoTransporte;

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.setPackage("com.whatsapp");
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

        // 🔹 CORRECCIÓN 2 (GUARDAR RUTA) 🔹
        // Se reemplazó el listener anterior y se movió la lógica de guardado aquí.
        // Ya no usamos el método `guardarRutaEnHistorial()` separado.
        btnGuardarRuta.setOnClickListener(v -> {
            // Verificamos que el contexto (fragmento) aún exista
            if (getContext() == null || !isAdded()) {
                return;
            }

            // Obtenemos los nombres *directamente* de los campos de texto
            String nombreOrigen = etOrigen.getText().toString();
            String nombreDestino = etDestino.getText().toString();

            // Validamos que tengamos coordenadas Y que los campos de texto no estén vacíos
            if (origenLatLng != null && destinoLatLng != null &&
                    !nombreOrigen.isEmpty() && !nombreDestino.isEmpty()) {

                // --- Lógica de guardado (antes en guardarRutaEnHistorial) ---
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if (user == null) {
                    Toast.makeText(getContext(), getString(R.string.historial_no_login), Toast.LENGTH_SHORT).show();
                    return;
                }
                String uid = user.getUid();

                HistorialRuta nuevaRuta = new HistorialRuta(
                        uid,
                        nombreOrigen,  // Usamos el texto del campo
                        nombreDestino, // Usamos el texto del campo
                        origenLatLng.latitude,
                        origenLatLng.longitude,
                        destinoLatLng.latitude,
                        destinoLatLng.longitude,
                        new Timestamp(new Date())
                );

                db.collection("historialRutas")
                        .add(nuevaRuta)
                        .addOnSuccessListener(documentReference -> {
                            if (getContext() != null && isAdded()) {
                                Toast.makeText(getContext(), getString(R.string.historial_ruta_guardada), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (getContext() != null && isAdded()) {
                                Toast.makeText(getContext(), getString(R.string.historial_error_guardar), Toast.LENGTH_SHORT).show();
                            }
                        });
                // --- Fin de la lógica de guardado ---

            } else {
                Toast.makeText(requireContext(), getString(R.string.historial_selecciona_ruta), Toast.LENGTH_SHORT).show();
            }
        });
        // --- FIN DE LA CORRECCIÓN 2 ---


        actualizarTextosUI();
        solicitarPermisoNotificaciones();
        notificacionesZonas = new Notificaciones_Zonas(requireContext());

        intervaloEnvio = notificacionesZonas.getIntervaloAlertas();
        intervaloNotificacion = intervaloEnvio;

        iniciarAlertasZonas();
        return root;
    }


    // 🔹 CORRECCIÓN 2 (GUARDAR RUTA) 🔹
    // Este método ya no es necesario, su lógica se movió al OnClickListener del botón.
    // private void guardarRutaEnHistorial() { ... }


    private void iniciarEnvioUbicacion() {
        runnable = new Runnable() {
            @Override
            public void run() {
                // Verificación de seguridad (Crash Fix)
                if (getContext() == null || !isAdded()) {
                    handler.postDelayed(this, intervaloEnvio);
                    return;
                }

                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        // Lógica de envío de ubicación
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

        if (getArguments() != null && getArguments().containsKey("origenLat")) {
            Bundle args = getArguments();
            origenLatLng = new LatLng(args.getDouble("origenLat"), args.getDouble("origenLng"));
            destinoLatLng = new LatLng(args.getDouble("destinoLat"), args.getDouble("destinoLng"));
            // Usamos las variables de clase para los nombres, que SÍ se reciben del historial
            origenNombre = args.getString("origenNombre");
            destinoNombre = args.getString("destinoNombre");

            etOrigen.setText(origenNombre);
            etDestino.setText(destinoNombre);

            new Handler().postDelayed(() -> {
                if (mMap != null) {
                    btnCalcularRuta.performClick();
                }
            }, 500);
        }

        // Clic largo para reportar
        mMap.setOnMapLongClickListener(latLng -> {
            String nombreLugar = getString(R.string.ubicacion_desconocida);

            if (getContext() == null || !isAdded()) {
                return;
            }

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

            if (getActivity() != null) {
                NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_contenido_general);
                navController.navigate(R.id.nav_gallery, bundle);
            }
        });

        mMap.setOnMapClickListener(null);
    }

    private void setAutocomplete(AutoCompleteTextView editText, boolean esOrigen) {
        editText.setThreshold(1);
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().isEmpty()) {
                    // Si el texto se borra, reseteamos las variables
                    if (esOrigen) {
                        origenLatLng = null;
                        origenNombre = null;
                    } else {
                        destinoLatLng = null;
                        destinoNombre = null;
                    }
                    return;
                }

                FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(s.toString()).setSessionToken(AutocompleteSessionToken.newInstance()).setCountries("PE").build();

                placesClient.findAutocompletePredictions(request).addOnSuccessListener(response -> {
                    if (getContext() == null || !isAdded()) {
                        return;
                    }

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
                                // Aquí SÍ guardamos el nombre en la variable de clase
                                origenNombre = place.getName();
                                toastMessage = getString(R.string.origen_fijado, place.getName());
                            } else {
                                destinoLatLng = place.getLatLng();
                                // Aquí SÍ guardamos el nombre en la variable de clase
                                destinoNombre = place.getName();
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
            if (e != null || getContext() == null || !isAdded()) {
                return;
            }

            listaDeReportes.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                listaDeReportes.add(doc.toObject(Reporte.class));
            }
            if (mMap != null) {
                dibujarReportesEnMapa();
            }
        });
    }

    private void dibujarReportesEnMapa() {
        if (mMap == null) return;

        for (Circle circle : circleReportesActuales) { circle.remove(); }
        circleReportesActuales.clear();
        for (Marker marker : markerReportesActuales) { marker.remove(); }
        markerReportesActuales.clear();

        for (Reporte reporte : listaDeReportes) {

            if (filtroRiesgo != 0 && reporte.getRiesgo() != filtroRiesgo) {
                continue;
            }

            int color;
            float hue;
            String riesgoStr;

            switch (reporte.getRiesgo()) {
                case 1:
                    color = Color.parseColor("#5533FF33"); // Verde
                    hue = BitmapDescriptorFactory.HUE_GREEN;
                    riesgoStr = getString(R.string.riesgo_seguro);
                    break;
                case 2:
                    color = Color.parseColor("#55FFA500"); // Naranja
                    hue = BitmapDescriptorFactory.HUE_ORANGE;
                    riesgoStr = getString(R.string.riesgo_moderado);
                    break;
                default:
                    color = Color.parseColor("#55FF3333"); // Rojo
                    hue = BitmapDescriptorFactory.HUE_RED;
                    riesgoStr = getString(R.string.riesgo_inseguro);
                    break;
            }

            Circle circle = mMap.addCircle(new CircleOptions()
                    .center(new LatLng(reporte.getLat(), reporte.getLng()))
                    .radius(75)
                    .strokeWidth(0)
                    .fillColor(color)
                    .clickable(false));

            circleReportesActuales.add(circle);

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(reporte.getLat(), reporte.getLng()))
                    .title(riesgoStr + ": " + reporte.getNombreLugar())
                    .snippet(reporte.getDescripcion())
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));

            markerReportesActuales.add(marker);
        }
    }

    private void calcularRutas(LatLng origen, LatLng destino) {
        // Verificación de seguridad
        if (getContext() == null || !isAdded()) {
            return;
        }

        String url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + origen.latitude + "," + origen.longitude +
                "&destination=" + destino.latitude + "," + destino.longitude +
                "&mode=" + modoTransporte +
                "&alternatives=true" +
                "&language=" + idiomaSeleccionado +
                "&key=" + getApiKey();

        RequestQueue queue = Volley.newRequestQueue(requireContext());
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null, this::procesarRespuestaDeRutas,
                error -> {
                    if (getContext() != null && isAdded()) {
                        Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        queue.add(request);
    }

    private void procesarRespuestaDeRutas(JSONObject response) {
        if (getContext() == null || !isAdded()) {
            return;
        }

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

                // Lógica de Ruta más Corta (sin cambios, ya era correcta)
                if (distanciaActual < menorDistancia) {
                    menorDistancia = distanciaActual;
                    rutaMasCortaIndex = i;
                }

                // Lógica de Ruta más Segura (con puntaje modificado)
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

    // 🔹 CORRECCIÓN 3 (RUTA SEGURA) 🔹
    // Se modificaron las penalizaciones para que la Ruta Segura
    // evite activamente las zonas de Riesgo 3.
    private int calcularPuntajeRiesgo(List<LatLng> puntos) {
        int puntajeRiesgo = 0;
        for (LatLng punto : puntos) {
            for (Reporte reporte : listaDeReportes) {
                float[] distancia = new float[1];
                Location.distanceBetween(punto.latitude, punto.longitude, reporte.getLat(), reporte.getLng(), distancia);

                if (distancia[0] < 75) { // Si un punto de la ruta está dentro de los 75m de un reporte
                    switch (reporte.getRiesgo()) {
                        // case 1 (Seguro): Añade 0 puntos. (Preferido)
                        case 2: // Moderado
                            puntajeRiesgo += 5; // Penalización baja
                            break;
                        case 3: // Inseguro
                            puntajeRiesgo += 1000; // Penalización MUY ALTA
                            break;
                    }
                }
            }
        }
        return puntajeRiesgo;
    }
    // --- FIN DE LA CORRECCIÓN 3 ---

    private void dibujarRuta(List<LatLng> puntos, int color, float ancho, String tag) {
        if (mMap == null) return;
        Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(puntos).color(color).width(ancho).clickable(true));
        polyline.setTag(tag);
        // 🔹 CORRECCIÓN 1 (TYPO) 🔹
        polylinesActuales.add(polyline);
    }

    private void limpiarRutasYMarcadores() {
        if (mMap == null) return;
        // 🔹 CORRECCIÓN 1 (TYPO) 🔹
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

    private void iniciarAlertasZonas() {
        runnableAlertas = new Runnable() {
            @Override
            public void run() {
                if (getContext() == null || !isAdded()) {
                    handlerAlertas.postDelayed(this, intervaloNotificacion);
                    return;
                }

                if (!notificacionesActivadas) {
                    handlerAlertas.postDelayed(this, intervaloNotificacion);
                    return;
                }

                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (getContext() == null || !isAdded()) {
                        return;
                    }

                    if (location != null && System.currentTimeMillis() - ultimaNotificacion >= intervaloNotificacion) {
                        ultimaNotificacion = System.currentTimeMillis();

                        LatLng posicionActual = new LatLng(location.getLatitude(), location.getLongitude());
                        boolean enZona = false;

                        for (Reporte reporte : listaDeReportes) {
                            float[] distancia = new float[1];
                            Location.distanceBetween(
                                    posicionActual.latitude,
                                    posicionActual.longitude,
                                    reporte.getLat(),
                                    reporte.getLng(),
                                    distancia
                            );

                            if (distancia[0] < 75) {
                                enZona = true;
                                switch (reporte.getRiesgo()) {
                                    case 1:
                                        notificacionesZonas.mostrarNotificacion(
                                                getString(R.string.riesgo_seguro),
                                                "Te encuentras en una zona verde. Sin riesgos.",
                                                Color.GREEN
                                        );
                                        break;
                                    case 2:
                                        notificacionesZonas.mostrarNotificacion(
                                                getString(R.string.riesgo_moderado),
                                                "Zona amarilla detectada. Mantente alerta.",
                                                Color.YELLOW
                                        );
                                        break;
                                    case 3:
                                        notificacionesZonas.mostrarNotificacion(
                                                getString(R.string.riesgo_inseguro),
                                                "¡Estás en una zona roja! Evita permanecer aquí.",
                                                Color.RED
                                        );
                                        break;
                                }
                                break;
                            }
                        }

                        if (!enZona && mostrarZonaTranquila) {
                            notificacionesZonas.mostrarNotificacion(
                                    "Zona tranquila",
                                    "No hay reportes cercanos. Todo en calma.",
                                    Color.CYAN
                            );
                        }
                    }
                });

                handlerAlertas.postDelayed(this, intervaloNotificacion);
            }
        };

        handlerAlertas.post(runnableAlertas);
    }

    private void solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        requireActivity(),
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Permiso de notificación concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Permiso de notificación denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}