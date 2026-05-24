package com.example.ellibrodelcuco.data.repository;

import com.example.ellibrodelcuco.data.callback.Callback;
import com.example.ellibrodelcuco.data.callback.SnapshotCallback;
import com.example.ellibrodelcuco.modelo.Libro;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;

public class BibliotecaRepository {

    private static BibliotecaRepository instance;
    private final FirebaseFirestore db;

    private BibliotecaRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized BibliotecaRepository getInstance() {
        if (instance == null) instance = new BibliotecaRepository();
        return instance;
    }

    public ListenerRegistration escucharBiblioteca(String uid, SnapshotCallback<Libro> callback) {
        return db.collection("usuarios")
                .document(uid)
                .collection("biblioteca")
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (snap == null) return;
                    List<Libro> libros = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snap) {
                        Libro libro = doc.toObject(Libro.class);
                        // la fecha la tengo que leer aparte porque Firestore no mapea Timestamp a Date solo
                        java.util.Date fecha = doc.getDate("fechaLeido");
                        if (fecha != null) {
                            libro.setFechaLeido(fecha);
                        }
                        libros.add(libro);
                    }
                    callback.onChange(libros);
                });
    }

    // genera un id a partir del titulo y autor para que no haya colisiones
    public static String generarId(String titulo, String autor) {
        String t = titulo != null ? titulo : "desconocido";
        String a = autor != null ? autor : "anonimo";
        // quito los caracteres que petan en firestore
        return (t + "_" + a).replaceAll("[/\\\\.\\[\\]#]", "-").toLowerCase();
    }

    public void guardarLibro(String uid, Libro libro, Callback<Void> callback) {
        String docId = generarId(libro.getTitulo(), libro.getAutor());
        db.collection("usuarios").document(uid).collection("biblioteca")
                .document(docId)
                .set(libro, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void eliminarLibro(String uid, String titulo, String autor, Callback<Void> callback) {
        String docId = generarId(titulo, autor);
        db.collection("usuarios").document(uid).collection("biblioteca").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void obtenerLibro(String uid, String titulo, String autor, Callback<Libro> callback) {
        String docId = generarId(titulo, autor);
        db.collection("usuarios").document(uid).collection("biblioteca").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Libro libro = doc.toObject(Libro.class);
                        java.util.Date fecha = doc.getDate("fechaLeido");
                        if (fecha != null && libro != null) libro.setFechaLeido(fecha);
                        callback.onSuccess(libro);
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}