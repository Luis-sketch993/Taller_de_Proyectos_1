package com.example.myapplicationf.ui.slideshow;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplicationf.Adapters.HistorialAdapter;
import com.example.myapplicationf.Models.HistorialRuta;
import com.example.myapplicationf.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SlideshowFragment extends Fragment {

    private static final String TAG = "HistorialDebug";

    private RecyclerView recyclerView;
    private HistorialAdapter adapter;
    private List<HistorialRuta> historialRutaList;
    private FirebaseFirestore db;
    private TextView tvEmptyHistory;
    private ListenerRegistration historialListener; // <-- Para gestionar el listener

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_slideshow, container, false);

        recyclerView = root.findViewById(R.id.recyclerViewHistorial);
        tvEmptyHistory = root.findViewById(R.id.tvEmptyHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        historialRutaList = new ArrayList<>();
        adapter = new HistorialAdapter(historialRutaList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        // --- 🔹 CAMBIO CLAVE: Iniciar el listener en onStart ---
        cargarHistorial();
    }

    @Override
    public void onStop() {
        super.onStop();
        // --- 🔹 CAMBIO CLAVE: Detener el listener en onStop para evitar fugas de memoria ---
        if (historialListener != null) {
            historialListener.remove();
        }
    }

    private void cargarHistorial() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            tvEmptyHistory.setText(getString(R.string.historial_no_login));
            tvEmptyHistory.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            return;
        }
        String uid = user.getUid();

        Query query = db.collection("historialRutas")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING);

        historialListener = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.e(TAG, "Error al escuchar el historial.", e);
                tvEmptyHistory.setText(getString(R.string.historial_error_cargar));
                tvEmptyHistory.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                return;
            }

            if (snapshots != null) {
                Log.d(TAG, "Actualización recibida. Número de documentos: " + snapshots.size());
                // --- 🔹 CAMBIO CLAVE: Lógica de actualización más segura ---
                List<HistorialRuta> nuevasRutas = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    nuevasRutas.add(doc.toObject(HistorialRuta.class));
                }

                historialRutaList.clear();
                historialRutaList.addAll(nuevasRutas);
                adapter.notifyDataSetChanged();

                if (historialRutaList.isEmpty()) {
                    Log.d(TAG, "La lista del historial está vacía. Mostrando mensaje.");
                    tvEmptyHistory.setText(getString(R.string.historial_vacio));
                    tvEmptyHistory.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    Log.d(TAG, "La lista del historial tiene datos. Mostrando RecyclerView.");
                    tvEmptyHistory.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            } else {
                Log.w(TAG, "La actualización de snapshots es nula.");
            }
        });
    }
}

