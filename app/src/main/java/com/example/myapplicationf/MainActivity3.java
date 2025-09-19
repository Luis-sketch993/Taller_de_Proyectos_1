package com.example.myapplicationf;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity3 extends AppCompatActivity {

    private EditText registronombre, registroapellido, registroEmail, registrocontraseña, registrocontraseñaconfirmar;
    private Button botonRegistrar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main3);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        registronombre = findViewById(R.id.reNombre);
        registroapellido = findViewById(R.id.reApellido);
        registroEmail = findViewById(R.id.reEmail);
        registrocontraseña = findViewById(R.id.reContraseña);
        registrocontraseñaconfirmar = findViewById(R.id.reConfirmarContraseña);
        botonRegistrar = findViewById(R.id.btnRegistrarre);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        botonRegistrar.setOnClickListener(v -> {
            String nombre = registronombre.getText().toString().trim();
            String apellido = registroapellido.getText().toString().trim();
            String email = registroEmail.getText().toString().trim();
            String pass = registrocontraseña.getText().toString().trim();
            String confirmPass = registrocontraseñaconfirmar.getText().toString().trim();

            if (TextUtils.isEmpty(nombre) || TextUtils.isEmpty(apellido) || TextUtils.isEmpty(email) ||
                    TextUtils.isEmpty(pass) || TextUtils.isEmpty(confirmPass)) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(confirmPass)) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Guardar en Firestore
                            String userId = mAuth.getCurrentUser().getUid();
                            Map<String, Object> usuario = new HashMap<>();
                            usuario.put("nombre", nombre);
                            usuario.put("apellido", apellido);
                            usuario.put("email", email);

                            db.collection("usuarios").document(userId)
                                    .set(usuario)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(this, MainActivity2.class);
                                        intent.putExtra("email", email);
                                        intent.putExtra("nombre", nombre);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Error al guardar en Firestore", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
