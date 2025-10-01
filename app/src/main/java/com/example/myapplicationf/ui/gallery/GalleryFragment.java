package com.example.myapplicationf.ui.gallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplicationf.databinding.FragmentGalleryBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private FirebaseFirestore db;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();

        cargarReportes();

        return root;
    }

    private void cargarReportes() {
        db.collection("reportes")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        StringBuilder sb = new StringBuilder();
                        for(QueryDocumentSnapshot doc : task.getResult()){
                            double lat = doc.getDouble("lat");
                            double lng = doc.getDouble("lng");
                            String estado = doc.getString("estado");
                            sb.append("Reporte: ").append(estado)
                                    .append("\nLat: ").append(lat)
                                    .append(", Lng: ").append(lng)
                                    .append("\n\n");
                        }
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
