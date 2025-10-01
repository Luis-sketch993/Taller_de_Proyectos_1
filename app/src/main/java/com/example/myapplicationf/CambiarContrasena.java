package com.example.myapplicationf;

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

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class CambiarContrasena extends AppCompatActivity {

    private EditText etPassActual, etPassNueva;
    private Button btnCambiar;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cambiar_contrasena);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etPassActual = findViewById(R.id.etPassActual);
        etPassNueva = findViewById(R.id.etPassNueva);
        btnCambiar = findViewById(R.id.btnCambiarPass);
        mAuth = FirebaseAuth.getInstance();

        btnCambiar.setOnClickListener(v -> {
            String passActual = etPassActual.getText().toString().trim();
            String passNueva = etPassNueva.getText().toString().trim();

            if (TextUtils.isEmpty(passActual) || TextUtils.isEmpty(passNueva)) {
                Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null || user.getEmail() == null) {
                Toast.makeText(this, "Debe iniciar sesión nuevamente", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔹 Reautenticación
            user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), passActual))
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // 🔹 Cambiar la contraseña
                            user.updatePassword(passNueva)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            Toast.makeText(this, "Contraseña actualizada correctamente", Toast.LENGTH_LONG).show();
                                        } else {
                                            Toast.makeText(this, "Error: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this, "La contraseña actual es incorrecta", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
