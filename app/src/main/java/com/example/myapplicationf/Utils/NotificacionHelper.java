package com.example.myapplicationf.Utils;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificacionHelper {

    private static final String CANAL_ID = "safe_route_channel";
    private static final int NOTIFICACION_ID = 100;

    public static void crearCanal(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence nombre = "Canal SafeRoute";
            String descripcion = "Notificaciones de SafeRoute";
            int importancia = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel canal = new NotificationChannel(CANAL_ID, nombre, importancia);
            canal.setDescription(descripcion);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if(manager != null) manager.createNotificationChannel(canal);
        }
    }

    public static void mostrar(Context context, String mensaje) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("SafeRoute")
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.notify(NOTIFICACION_ID, builder.build());
    }
}