package com.example.ellibrodelcuco.utils;

import android.content.Context;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Gestor de lógica de gamificación. Valida el cumplimiento de requisitos
 * para otorgar logros y notificar al usuario mediante componentes UI.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class LogrosHelper {

    // constantes de los logros disponibles
    public static final String PRIMER_LIBRO = "primer_libro";
    public static final String APRENDIZ = "aprendiz";
    public static final String LECTOR = "lector";
    public static final String BIBLIOFILO = "bibliofilo";
    public static final String PRIMERA_RESENA = "primera_resena";
    public static final String RACHA_7 = "racha_7";
    public static final String EXPLORADOR = "explorador";

    private static final Map<String, String> NOMBRES = new HashMap<String, String>() {{
        //los nombres de los logros
        put(PRIMER_LIBRO, "Primer libro");
        put(APRENDIZ, "Aprendiz");
        put(LECTOR, "Lector");
        put(BIBLIOFILO, "Bibliófilo");
        put(PRIMERA_RESENA, "Primera reseña");
        put(RACHA_7, "Racha de 7 días");
        put(EXPLORADOR, "Explorador");
    }};


     //Valida el estado de los logros del usuario

    public static void comprobar(String uid, FirebaseFirestore db, Context ctx) {
        //  Se obtienen los logros del usuario para no erepetir
        db.collection("usuarios").document(uid).collection("logros").get()
                .addOnSuccessListener(logrosSnap -> {
                    Set<String> yaDesbloqueados = new HashSet<>();
                    for (QueryDocumentSnapshot d : logrosSnap) {
                        yaDesbloqueados.add(d.getId());
                    }

                    //  Obtenemos su biblioteca para contar libros y los otros datos
                    db.collection("usuarios").document(uid).collection("biblioteca").get()
                            .addOnSuccessListener(bibSnap -> {
                                int leidosCount = 0;
                                Set<String> generos = new HashSet<>();
                                for (QueryDocumentSnapshot d : bibSnap) {
                                    String estado = d.getString("estado");
                                    String genero = d.getString("genero");
                                    if ("Leído".equalsIgnoreCase(estado)) leidosCount++;
                                    if (genero != null && !genero.isEmpty()) generos.add(genero);
                                }

                                final int leidos = leidosCount;
                                final int generosDistintos = generos.size();

                                //  Obtenemos la racha actual
                                db.collection("usuarios").document(uid).get()
                                        .addOnSuccessListener(userDoc -> {
                                            long racha = (userDoc.exists() && userDoc.getLong("rachaActual") != null)
                                                    ? userDoc.getLong("rachaActual") : 0;

                                            // Condicionales basicas para mapear el tema de los logros y requerimientos
                                            Map<String, Boolean> candidatos = new HashMap<>();
                                            if (leidos >= 1) candidatos.put(PRIMER_LIBRO, true);
                                            if (leidos >= 5) candidatos.put(APRENDIZ, true);
                                            if (leidos >= 10) candidatos.put(LECTOR, true);
                                            if (leidos >= 50) candidatos.put(BIBLIOFILO, true);
                                            if (racha >= 7) candidatos.put(RACHA_7, true);
                                            if (generosDistintos >= 5) candidatos.put(EXPLORADOR, true);

                                            // Ultimo filtro por si el logro no esyaba en la primera lista
                                            for (Map.Entry<String, Boolean> entry : candidatos.entrySet()) {
                                                if (!yaDesbloqueados.contains(entry.getKey())) {
                                                    desbloquear(uid, db, ctx, entry.getKey());
                                                }
                                            }
                                        });
                            });
                });
    }

    public static void comprobarResena(String uid, FirebaseFirestore db, Context ctx) {
        db.collection("usuarios").document(uid).collection("logros").document(PRIMERA_RESENA).get()
                //este es diferente pq tiene que llamar a otra clase
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) desbloquear(uid, db, ctx, PRIMERA_RESENA);
                });
    }


    private static void desbloquear(String uid, FirebaseFirestore db, Context ctx, String logroId) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("desbloqueado", true);
        datos.put("fecha", new Date());
//ya se comprueba y si no lo tiene y y cumple se fggguarda en firebase y se deja claro por mensaje
        db.collection("usuarios").document(uid).collection("logros").document(logroId)
                .set(datos)
                .addOnSuccessListener(aVoid -> {
                    if (ctx instanceof android.app.Activity) {
                        android.view.View rootView = ((android.app.Activity) ctx).getWindow().getDecorView().getRootView();
                        String nombre = NOMBRES.containsKey(logroId) ? NOMBRES.get(logroId) : logroId;
                        Snackbar.make(rootView, "¡Logro desbloqueado: " + nombre + "!", Snackbar.LENGTH_LONG).show();
                    }
                });
    }
}