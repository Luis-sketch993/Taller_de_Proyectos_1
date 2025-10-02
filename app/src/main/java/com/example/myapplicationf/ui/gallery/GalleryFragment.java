package com.example.myapplicationf.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();

        // 🔹 Recibir lat, lng y nombreLugar desde el HomeFragment
        Bundle args = getArguments();
        if (args != null) {
            lat = args.getDouble("lat", 0);
            lng = args.getDouble("lng", 0);
            nombreLugar = args.getString("nombreLugar", "Sin nombre");

            binding.textGallery.setText("Nuevo reporte:\n" +
                    "📍 Lugar: " + nombreLugar +
                    "\nLat: " + lat +
                    ", Lng: " + lng);
        } else {
            binding.textGallery.setText("Lista de reportes:");
            cargarReportes(); // si no viene de un click en el mapa, cargamos todos
        }

        // 🔹 Guardar reporte cuando se pulse el botón
        binding.btnGuardarReporte.setOnClickListener(v -> {
            String descripcion = binding.edtDescripcion.getText().toString();

            if (descripcion.isEmpty()) {
                binding.textGallery.setText("⚠️ Por favor ingresa una descripción.");
                return;
            }

            // Usamos los valores pasados por el HomeFragment
            Reporte reporte = new Reporte(lat, lng, descripcion, nombreLugar);

            db.collection("reportes").add(reporte)
                    .addOnSuccessListener(docRef -> {
                        binding.textGallery.setText("✅ Reporte guardado con éxito\n" +
                                "📍 Lugar: " + nombreLugar +
                                "\nLat: " + lat + ", Lng: " + lng);
                        binding.edtDescripcion.setText(""); // limpiar campo
                    })
                    .addOnFailureListener(e -> {
                        binding.textGallery.setText("❌ Error al guardar reporte: " + e.getMessage());
                    });
        });

        return root;
    }

    // 🔹 Método para listar reportes cuando no viene desde HomeFragment
    private void cargarReportes() {
        db.collection("reportes")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        StringBuilder sb = new StringBuilder();
                        task.getResult().forEach(doc -> {
                            double lat = doc.getDouble("lat");
                            double lng = doc.getDouble("lng");
                            String descripcion = doc.getString("descripcion");
                            String nombreLugar = doc.getString("nombreLugar");

                            sb.append("Reporte: ").append(descripcion)
                                    .append("\n📍 Lugar: ").append(nombreLugar)
                                    .append("\nLat: ").append(lat)
                                    .append(", Lng: ").append(lng)
                                    .append("\n\n");
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
