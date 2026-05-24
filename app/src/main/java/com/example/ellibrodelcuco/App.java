package com.example.ellibrodelcuco;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Registramos el canal de notificaciones globalmente al arrancar la app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "racha_channel",
                    "Racha lectora",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Recordatorios de racha diaria de lectura");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}