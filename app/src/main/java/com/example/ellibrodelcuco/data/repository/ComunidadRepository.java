package com.example.ellibrodelcuco.data.repository;

import com.example.ellibrodelcuco.data.callback.Callback;
import com.example.ellibrodelcuco.data.callback.SnapshotCallback;
import com.example.ellibrodelcuco.modelo.Libro;
import com.example.ellibrodelcuco.modelo.Resena;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComunidadRepository {

    private static ComunidadRepository instance;
    private final FirebaseFirestore db;

    private ComunidadRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized ComunidadRepository getInstance() {
        if (instance == null) instance = new ComunidadRepository();
        return instance;
    }

    public void obtenerLibrosPorRating(int limite, com.google.firebase.firestore.DocumentSnapshot ultimoDocumento,
                                       Callback<Map<String, Object>> callback) {
        Query query = db.collection("libros_globales")
                .orderBy("mediaNotas", Query.Direction.DESCENDING)
                .limit(limite);

        if (ultimoDocumento != null) {
            query = query.startAfter(ultimoDocumento);
        }

        query.get()
                .addOnSuccessListener(snap -> {
                    List<Libro> libros = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        libros.add(mapearLibroGlobal(doc));
                    }
                    com.google.firebase.firestore.DocumentSnapshot lastVisible = null;
                    if (!snap.isEmpty()) {
                        lastVisible = snap.getDocuments().get(snap.size() - 1);
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("libros", libros);
                    result.put("ultimoDocumento", lastVisible);
                    callback.onSuccess(result);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public ListenerRegistration escucharResenas(String uid, String tituloLibro, SnapshotCallback<Resena> callback) {
        return db.collection("usuarios")
                .document(uid)
                .collection("biblioteca")
                .document(tituloLibro)
                .collection("resenas")
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (snap == null) return;
                    List<Resena> resenas = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        resenas.add(doc.toObject(Resena.class));
                    }
                    callback.onChange(resenas);
                });
    }

    public void publicarResena(String uid, String nombreUsuario, Libro libro,
                               float nota, String texto, Callback<Void> callback) {
        Resena resena = new Resena(uid, nombreUsuario, texto, nota, new Date());
        db.collection("usuarios")
                .document(uid)
                .collection("biblioteca")
                .document(libro.getTitulo())
                .collection("resenas")
                .document(uid)
                .set(resena)
                .addOnSuccessListener(aVoid -> {
                    actualizarContadorGlobal(libro, nota, nombreUsuario, texto);
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // meto los datos del libro global en un objeto Libro reutilizando el campo estado para mostrar la nota
    private Libro mapearLibroGlobal(QueryDocumentSnapshot doc) {
        String titulo = doc.getString("titulo");
        String autor = doc.getString("autor");
        String portadaUrl = doc.getString("portadaUrl");
        Double media = doc.getDouble("mediaNotas");
        Long totalResenas = doc.getLong("totalResenas");

        String estadoDisplay = "⭐ " + (media != null ? String.format("%.1f", media) : "—")
                + (totalResenas != null ? "  ·  " + totalResenas + " reseña" + (totalResenas == 1 ? "" : "s") : "");

        return new Libro(
                titulo != null ? titulo : "",
                autor != null ? autor : "",
                "",
                portadaUrl != null ? portadaUrl.replace("http:", "https:") : "",
                estadoDisplay
        );
    }

    // recalculo la media cada vez que alguien reseña, no es lo mas eficiente pero funciona
    private void actualizarContadorGlobal(Libro libro, float nuevaNota, String nombreUsuario, String texto) {
        String titulo = libro.getTitulo();
        db.collection("libros_globales").document(titulo)
                .get()
                .addOnSuccessListener(doc -> {
                    long totalResenas = 1;
                    double mediaNotas = nuevaNota;
                    if (doc.exists()) {
                        Long tr = doc.getLong("totalResenas");
                        Double mn = doc.getDouble("mediaNotas");
                        if (tr != null && mn != null && tr > 0) {
                            totalResenas = tr + 1;
                            mediaNotas = ((mn * tr) + nuevaNota) / totalResenas;
                        }
                    }
                    Map<String, Object> datos = new HashMap<>();
                    datos.put("titulo", titulo);
                    datos.put("autor", libro.getAutor() != null ? libro.getAutor() : "");
                    datos.put("portadaUrl", libro.getPortadaUrl() != null
                            ? libro.getPortadaUrl().replace("http:", "https:") : "");
                    datos.put("mediaNotas", mediaNotas);
                    datos.put("totalResenas", totalResenas);
                    datos.put("fechaUltimaResena", FieldValue.serverTimestamp());
                    datos.put("ultimoUsuarioResena", nombreUsuario != null ? nombreUsuario : "Anónimo");
                    datos.put("ultimaNotaResena", nuevaNota);
                    datos.put("ultimoTextoResena", texto != null ? texto : "");
                    db.collection("libros_globales").document(titulo).set(datos, SetOptions.merge());
                });
    }
}