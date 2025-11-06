package com.example.myapplicationf.Utils;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import com.example.myapplicationf.R;

public class Notificaciones_Zonas {

    private static final String CHANNEL_ID = "safe_route_alertas";
    private static final String CHANNEL_NAME = "Alertas de Zonas SafeRoute";

    // Preferencias
    private static final String PREFS_NAME = "SafeRoutePrefs";
    private static final String KEY_ALERTAS_ACTIVADAS = "alertas_activadas";
    private static final String KEY_INTERVALO_ALERTAS = "intervalo_alertas";

    private final Context context;
    private final SharedPreferences prefs;

    // Constructor (Versión simple)
    public Notificaciones_Zonas(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        crearCanalNotificacion();
    }

    // Crear canal de notificación (solo una vez)
    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notificaciones cuando entras a zonas seguras o peligrosas");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // Verificar permiso de notificación (Android 13+)
    private boolean tienePermisoNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true; // En versiones anteriores no se necesita permiso
    }

    // Mostrar notificación general
    public void mostrarNotificacion(String titulo, String mensaje, int color) {
        if (!alertasActivadas()) return; // Usuario desactivó alertas
        if (!tienePermisoNotificacion()) return; // No hay permiso de notificación

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notification_important_24)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setColor(color);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            int notificationId = (int) (System.currentTimeMillis() & 0xfffffff);
            manager.notify(notificationId, builder.build());
        }
    }

    // Tipos de notificaciones de zona
    public void notificarZonaVerde() {
        mostrarNotificacion("Zona Segura", "Te encuentras en una zona verde.", 0xFF4CAF50);
    }

    public void notificarZonaAmarilla() {
        mostrarNotificacion("Zona de Precaución", "Ten cuidado, estás en una zona amarilla.", 0xFFFFC107);
    }

    public void notificarZonaRoja() {
        mostrarNotificacion("Zona Peligrosa", "Evita permanecer aquí, estás en una zona roja.", 0xFFF44336);
    }

    public void notificarZonaTranquila() {
        mostrarNotificacion("Zona Tranquila", "No estás en ninguna zona de riesgo.", 0xFF2196F3);
    }

    // ----------------------------------------------------------------
    // 🔹 MÉTODOS DE PREFERENCIAS (ESTO FALTABA) 🔹
    // ----------------------------------------------------------------
    public void setAlertasActivadas(boolean activadas) {
        prefs.edit().putBoolean(KEY_ALERTAS_ACTIVADAS, activadas).apply();
    }

    public boolean alertasActivadas() {
        return prefs.getBoolean(KEY_ALERTAS_ACTIVADAS, true);
    }

    public void setIntervaloAlertas(long milisegundos) {
        prefs.edit().putLong(KEY_INTERVALO_ALERTAS, milisegundos).apply();
    }

    /**
     * Devuelve el intervalo de alertas en milisegundos.
     * Por defecto son 2 minutos (2 * 60 * 1000).
     */
    public long getIntervaloAlertas() {
        // El error estaba aquí porque este método faltaba.
        return prefs.getLong(KEY_INTERVALO_ALERTAS, 2 * 60 * 1000); // 2 min por defecto
    }
}