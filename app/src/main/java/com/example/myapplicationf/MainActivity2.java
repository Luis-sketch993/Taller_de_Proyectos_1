package com.example.myapplicationf;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity2 extends AppCompatActivity {

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button cerrar = findViewById(R.id.btnCerrar);
        TextView usuario = findViewById(R.id.tvUsuario);
        TextView correo = findViewById(R.id.tvdireccion);

        preferences = getSharedPreferences("sesion", MODE_PRIVATE);

        // 🔹 Recibir datos desde MainActivity o MainActivity3
        Intent intent = getIntent();
        String nombre = intent.getStringExtra("nombre");
        String apellido = intent.getStringExtra("apellido");
        String email = intent.getStringExtra("email");

        // 🔹 Mostrar datos
        usuario.setText("Bienvenido, " + nombre + " ");
        correo.setText("Correo: " + email);

        // 🔹 Botón cerrar sesión
        cerrar.setOnClickListener(v -> {
            preferences.edit().clear().apply(); // borrar sesión guardada
            Intent volver = new Intent(this, MainActivity.class);
            startActivity(volver);
            finish();
        });
    }
}
