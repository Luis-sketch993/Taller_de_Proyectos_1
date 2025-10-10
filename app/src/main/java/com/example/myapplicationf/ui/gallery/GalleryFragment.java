package com.example.myapplicationf.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplicationf.Models.Reporte;
import com.example.myapplicationf.databinding.FragmentGalleryBinding;
import com.google.firebase.firestore.FirebaseFirestore;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private FirebaseFirestore db;
    private double lat, lng;
    private String nombreLugar;

    // 🔹 PASO 1: DECLARAR LA VARIABLE QUE CAUSABA EL ERROR
    // La declaramos aquí, a nivel de clase, para que sea accesible en todo el fragmento.
    // Le damos un valor por defecto de '1' que corresponde a "Seguro".
    private int nivelRiesgoSeleccionado = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();

        // Recibir lat, lng y nombreLugar desde el HomeFragment (esto ya lo tenías bien)
        Bundle args = getArguments();
        if (args != null) {
            lat = args.getDouble("lat", 0);
            lng = args.getDouble("lng", 0);
            nombreLugar = args.getString("nombreLugar", "Sin nombre");
            binding.textGallery.setText("Nuevo reporte en:\n" + nombreLugar);
        } else {
            binding.textGallery.setText("Lista de reportes:");
            cargarReportes();
        }

        // 🔹 PASO 2: CONFIGURAR EL SPINNER PARA ACTUALIZAR LA VARIABLE
        Spinner spinnerRiesgo = binding.spinnerRiesgo; // Asegúrate que el ID en tu XML es "spinnerRiesgo"

        String[] nivelesRiesgo = {"Seguro", "Riesgo Moderado", "Inseguro"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, nivelesRiesgo);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRiesgo.setAdapter(adapter);

        // Listener para capturar la selección del usuario
        spinnerRiesgo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // La posición es 0, 1 o 2. Le sumamos 1 para obtener el nivel de riesgo (1, 2 o 3).
                nivelRiesgoSeleccionado = position + 1;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                nivelRiesgoSeleccionado = 1; // Si no se selecciona nada, por defecto es seguro.
            }
        });


        // 🔹 PASO 3: USAR LA VARIABLE CORREGIDA EN EL CONSTRUCTOR
        binding.btnGuardarReporte.setOnClickListener(v -> {
            String descripcion = binding.edtDescripcion.getText().toString();

            // Ahora el constructor coincide con el de la clase Reporte, ya que le pasamos los 5 argumentos.
            Reporte reporte = new Reporte(lat, lng, descripcion, nombreLugar, nivelRiesgoSeleccionado);

            db.collection("reportes").add(reporte)
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(getContext(), "Reporte guardado con éxito", Toast.LENGTH_SHORT).show();
                        binding.edtDescripcion.setText(""); // limpiar campo

                        // Opcional: Regresar al mapa automáticamente después de guardar
                        if (getParentFragmentManager() != null) {
                            getParentFragmentManager().popBackStack();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Error al guardar reporte: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        return root;
    }

    private void cargarReportes() {
        db.collection("reportes")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        StringBuilder sb = new StringBuilder();
                        task.getResult().forEach(doc -> {
                            Reporte r = doc.toObject(Reporte.class);
                            if (r != null) {
                                sb.append("Lugar: ").append(r.getNombreLugar())
                                        .append("\nRiesgo: ").append(r.getRiesgo())
                                        .append("\nDescripción: ").append(r.getDescripcion())
                                        .append("\n\n");
                            }
                        });
                        binding.textGallery.setText(sb.toString());
                    } else {
                        binding.textGallery.setText("No se pudieron cargar los reportes.");
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}