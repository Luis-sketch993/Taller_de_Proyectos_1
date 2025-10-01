package com.example.myapplicationf.ui.slideshow;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplicationf.databinding.FragmentSlideshowBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class SlideshowFragment extends Fragment {

    private FragmentSlideshowBinding binding;
    private FirebaseFirestore db;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentSlideshowBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        db = FirebaseFirestore.getInstance();

        cargarAlertas();

        return root;
    }

    private void cargarAlertas() {
        db.collection("alertas")
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        StringBuilder sb = new StringBuilder();
                        for(QueryDocumentSnapshot doc : task.getResult()){
                            String mensaje = doc.getString("mensaje");
                            String hora = doc.getString("hora");
                            sb.append("[").append(hora).append("] ").append(mensaje).append("\n\n");
                        }
                        binding.textSlideshow.setText(sb.toString());
                    } else {
                        binding.textSlideshow.setText("No se pudieron cargar las alertas.");
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
