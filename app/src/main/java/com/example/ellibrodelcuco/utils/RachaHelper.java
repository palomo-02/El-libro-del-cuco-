package com.example.ellibrodelcuco.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de lógica de gamificación. Calcula y sincroniza la racha de lectura diaria
 * comparando la actividad actual frente a la última fecha registrada.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class RachaHelper {

    public static final String PREFS_NAME = "racha_prefs";
    public static final String KEY_ULTIMA_ACTIVIDAD = "ultima_actividad";
    public static final String KEY_RACHA_ACTUAL = "racha_actual";

    /**
     * Algoritmo que pilla racha usando la actual y la ultima actividad regstrada.
     */
    public static long calcularNuevaRacha(Date ultimaActividad, long rachaActual) {
        if (ultimaActividad == null) return 1L;

        Calendar hoy = Calendar.getInstance();
        Calendar ultima = Calendar.getInstance();
        ultima.setTime(ultimaActividad);

        // se pone a medianoche para evitar errores por tiempo
        hoy.set(Calendar.HOUR_OF_DAY, 0); hoy.set(Calendar.MINUTE, 0);
        hoy.set(Calendar.SECOND, 0); hoy.set(Calendar.MILLISECOND, 0);

        ultima.set(Calendar.HOUR_OF_DAY, 0); ultima.set(Calendar.MINUTE, 0);
        ultima.set(Calendar.SECOND, 0); ultima.set(Calendar.MILLISECOND, 0);

        long diffDias = (hoy.getTimeInMillis() - ultima.getTimeInMillis()) / (1000L * 60 * 60 * 24);

        if (diffDias == 0) return rachaActual; // Hoy ya se registró
        if (diffDias == 1) return rachaActual + 1; // Continuidad (ayer)
        return 1L; // Racha rota
    }

    /**
     * No lo uso ahora mismo pero me da miedo borrarlo .
     */
    public static void actualizarRacha(String uid, FirebaseFirestore db) {
        actualizarRacha(uid, db, null);
    }

    /**
     * Recupera el estado actual de Firestore, recalcula la racha y guarda los resultados
     * tanto en la nube (Firestore) como en local (SharedPreferences).
     */
    public static void actualizarRacha(String uid, FirebaseFirestore db, Context context) {
        db.collection("usuarios").document(uid).get()
                .addOnSuccessListener(doc -> {
                    long rachaActual = 1;
                    long rachaMaxima = 1;

                    if (doc.exists()) {
                        Long ra = doc.getLong("rachaActual");
                        Long rm = doc.getLong("rachaMaxima");
                        if (ra != null) rachaActual = ra;
                        if (rm != null) rachaMaxima = rm;

                        Date ultimaActividad = doc.getDate("ultimaActividad");
                        long nueva = calcularNuevaRacha(ultimaActividad, rachaActual);

                        // Validación para evitar actualizaciones redundantes
                        if (nueva == rachaActual && ultimaActividad != null) {
                            Calendar hoy = Calendar.getInstance();
                            Calendar ultima = Calendar.getInstance();
                            ultima.setTime(ultimaActividad);
                            // Normalización
                            hoy.set(Calendar.HOUR_OF_DAY, 0); hoy.set(Calendar.MINUTE, 0);
                            ultima.set(Calendar.HOUR_OF_DAY, 0); ultima.set(Calendar.MINUTE, 0);
                            long diff = (hoy.getTimeInMillis() - ultima.getTimeInMillis()) / (1000L * 60 * 60 * 24);
                            if (diff == 0) {
                                guardar(uid, db, rachaActual, rachaMaxima, context);
                                return;
                            }
                        }
                        rachaActual = nueva;
                    }

                    if (rachaActual > rachaMaxima) rachaMaxima = rachaActual;
                    guardar(uid, db, rachaActual, rachaMaxima, context);
                });
    }

    /**
     * Esto es para la persistencia tanto en la nube como en local con el sharedPreferences .
     */
    static void guardar(String uid, FirebaseFirestore db, long rachaActual, long rachaMaxima, Context context) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("rachaActual", rachaActual);
        datos.put("rachaMaxima", rachaMaxima);
        datos.put("ultimaActividad", new Date());
        db.collection("usuarios").document(uid).set(datos, SetOptions.merge());

        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit()
                    .putLong(KEY_ULTIMA_ACTIVIDAD, System.currentTimeMillis())
                    .putLong(KEY_RACHA_ACTUAL, rachaActual)
                    .apply();
        }
    }
}