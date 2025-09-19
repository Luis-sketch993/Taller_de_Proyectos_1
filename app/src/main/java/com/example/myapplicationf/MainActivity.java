package com.example.myapplicationf;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private EditText email, contraseña;
    private Button botonIniciar, botonregistrarse;
    private CheckBox mantenerSesion;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = findViewById(R.id.emEmail);
        contraseña = findViewById(R.id.etContraseña);
        mantenerSesion = findViewById(R.id.chkSesion);
        botonIniciar = findViewById(R.id.btnIniciar);
        botonregistrarse = findViewById(R.id.btnRegistrarse);

        mAuth = FirebaseAuth.getInstance();
        //Aquí se establece la conexión con Firestore
        db = FirebaseFirestore.getInstance();
        preferences = getSharedPreferences("sesion", MODE_PRIVATE);

        // 🔹 Verificar si ya hay sesión guardada
        boolean sesionActiva = preferences.getBoolean("mantenerSesion", false);
        String savedEmail = preferences.getString("email", null);

        if (sesionActiva && savedEmail != null) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                cargarDatosUsuario(user.getUid());
            }
        }

        // BOTÓN REGISTRARSE -> pasa a MainActivity3
        botonregistrarse.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity3.class);
            startActivity(intent);
        });

        // BOTÓN INICIAR SESIÓN
        botonIniciar.setOnClickListener(view -> {
            String userEmail = email.getText().toString().trim();
            String userPass = contraseña.getText().toString().trim();

            if (!TextUtils.isEmpty(userEmail) && !TextUtils.isEmpty(userPass)) {
                mAuth.signInWithEmailAndPassword(userEmail, userPass)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    if (mantenerSesion.isChecked()) {
                                        preferences.edit()
                                                .putBoolean("mantenerSesion", true)
                                                .putString("email", userEmail)
                                                .apply();
                                    }
                                    cargarDatosUsuario(user.getUid());
                                }
                            } else {
                                Toast.makeText(this, "Usuario no encontrado. Regístrese.", Toast.LENGTH_LONG).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Ingrese email y contraseña", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔹 Cargar datos desde Firestore
    private void cargarDatosUsuario(String uid) {
        db.collection("usuarios").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String nombre = documentSnapshot.getString("nombre");
                        String apellido = documentSnapshot.getString("apellido");
                        String email = documentSnapshot.getString("email");

                        showHome(nombre, apellido, email);
                    } else {
                        Toast.makeText(this, "No se encontraron datos en Firestore", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error al obtener datos: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // REDIRECCIONAR AL HOME
    private void showHome(String nombre, String apellido, String email) {
        Intent homeIntent = new Intent(this, MainActivity2.class);
        homeIntent.putExtra("nombre", nombre);
        homeIntent.putExtra("apellido", apellido);
        homeIntent.putExtra("email", email);
        startActivity(homeIntent);
        finish();
    }
}
