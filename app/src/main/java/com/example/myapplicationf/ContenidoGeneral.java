package com.example.myapplicationf;

// 🔹 Imports originales y nuevos 🔹
import android.content.Context;
import android.content.Intent; // <--- Nuevo para Logout
import android.content.SharedPreferences; // <--- Nuevo para Logout
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth; // <--- Nuevo para Logout

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplicationf.databinding.ActivityContenidoGeneralBinding;

import java.util.Locale;

public class ContenidoGeneral extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityContenidoGeneralBinding binding;

    // 🔹 Variable añadida para el Logout 🔹
    private NavController navController;

    // 🔹 Método original para el idioma 🔹
    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("Settings", MODE_PRIVATE);
        String language = prefs.getString("My_Lang", Locale.getDefault().getLanguage());
        super.attachBaseContext(updateBaseContextLocale(newBase, language));
    }

    // 🔹 Método original para el idioma 🔹
    private Context updateBaseContextLocale(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityContenidoGeneralBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarContenidoGeneral.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();

        // 🔹 Variable asignada 🔹
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_contenido_general);

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        // 🔹 CÓDIGO MODIFICADO PARA MANEJAR "CERRAR SESIÓN" 🔹
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_logout) {
                // 1. Cerrar sesión en Firebase
                FirebaseAuth.getInstance().signOut();

                // 2. Limpiar SharedPreferences (para "mantenerSesion")
                SharedPreferences prefs = getSharedPreferences("sesion", MODE_PRIVATE);
                prefs.edit().clear().apply(); // Limpia todos los datos de sesión

                // 3. Redirigir a MainActivity (Login)
                Intent intent = new Intent(ContenidoGeneral.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Cierra ContenidoGeneral

                return true; // Click manejado
            }

            // Para los demás ítems (Home, Gallery, Slideshow)
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            if (handled) {
                drawer.closeDrawers(); // Cierra el menú si se navega
            }
            return handled;
        });
        // --- FIN DE LA MODIFICACIÓN ---
    }

    // 🔹 ESTE ES EL MÉTODO QUE CAUSABA EL ERROR (PORQUE FALTABA) 🔹
    // This method saves the selected language and restarts the activity to apply changes.
    public void setLocale(String lang) {
        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("My_Lang", lang);
        editor.apply();

        recreate();
    }

    // 🔹 Método original 🔹
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.contenido_general, menu);
        return true;
    }

    // 🔹 Método original 🔹
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_contenido_general);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}