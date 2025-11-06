package com.example.myapplicationf.ui.contactos;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplicationf.Adapters.ContactoAdapter;
import com.example.myapplicationf.Models.ContactoEmergencia;
import com.example.myapplicationf.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ContactosFragment extends Fragment {

    private EditText etNombre, etTelefono;
    private Button btnAgregar;
    private RecyclerView rvContactos;
    private TextView tvNoContactos;
    private ContactoAdapter adapter;
    private List<ContactoEmergencia> listaContactos = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String userId;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_contactos, container, false);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return root; // Salir si no hay usuario
        }
        userId = currentUser.getUid();

        etNombre = root.findViewById(R.id.etNombreContacto);
        etTelefono = root.findViewById(R.id.etTelefonoContacto);
        btnAgregar = root.findViewById(R.id.btnAgregarContacto);
        rvContactos = root.findViewById(R.id.rvContactos);
        tvNoContactos = root.findViewById(R.id.tvNoContactos);

        setupRecyclerView();
        btnAgregar.setOnClickListener(v -> agregarContacto());

        cargarContactos();

        return root;
    }

    private void setupRecyclerView() {
        adapter = new ContactoAdapter(listaContactos, contacto -> {
            // Lógica para borrar
            db.collection("usuarios").document(userId).collection("contactos")
                    .document(contacto.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Contacto eliminado", Toast.LENGTH_SHORT).show();
                        cargarContactos(); // Recargar lista
                    })
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al eliminar", Toast.LENGTH_SHORT).show());
        });
        rvContactos.setLayoutManager(new LinearLayoutManager(getContext()));
        rvContactos.setAdapter(adapter);
    }

    private void agregarContacto() {
        String nombre = etNombre.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

        if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(telefono)) {
            Toast.makeText(getContext(), "Complete nombre y teléfono", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validación simple de teléfono (solo números y longitud)
        if (!TextUtils.isDigitsOnly(telefono) || telefono.length() < 9) {
            Toast.makeText(getContext(), "Ingrese un número de teléfono válido (9 dígitos, sin símbolos)", Toast.LENGTH_LONG).show();
            return;
        }

        ContactoEmergencia nuevoContacto = new ContactoEmergencia(nombre, telefono);
        db.collection("usuarios").document(userId).collection("contactos")
                .add(nuevoContacto)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(getContext(), "Contacto agregado", Toast.LENGTH_SHORT).show();
                    etNombre.setText("");
                    etTelefono.setText("");
                    cargarContactos(); // Recargar lista
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al agregar", Toast.LENGTH_SHORT).show());
    }

    private void cargarContactos() {
        db.collection("usuarios").document(userId).collection("contactos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listaContactos.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            ContactoEmergencia contacto = document.toObject(ContactoEmergencia.class);
                            contacto.setId(document.getId()); // Guardar el ID del documento
                            listaContactos.add(contacto);
                        }
                        adapter.notifyDataSetChanged();
                        actualizarVistaVacia();
                    } else {
                        Toast.makeText(getContext(), "Error al cargar contactos", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void actualizarVistaVacia() {
        if (listaContactos.isEmpty()) {
            rvContactos.setVisibility(View.GONE);
            tvNoContactos.setVisibility(View.VISIBLE);
        } else {
            rvContactos.setVisibility(View.VISIBLE);
            tvNoContactos.setVisibility(View.GONE);
        }
    }
}