package com.example.ellibrodelcuco.data.repository;

import com.example.ellibrodelcuco.data.callback.Callback;
import com.example.ellibrodelcuco.data.callback.SnapshotCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Map;

public class UsuarioRepository {

    private static UsuarioRepository instance;
    private final FirebaseFirestore db;

    private UsuarioRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized UsuarioRepository getInstance() {
        if (instance == null) instance = new UsuarioRepository();
        return instance;
    }

    public ListenerRegistration escucharPerfil(String uid, Callback<Map<String, Object>> callback) {
        return db.collection("usuarios").document(uid)
                .addSnapshotListener((doc, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (doc != null && doc.exists()) {
                        callback.onSuccess(doc.getData());
                    }
                });
    }

    public void actualizarMetaAnual(String uid, long meta, Callback<Void> callback) {
        db.collection("usuarios").document(uid)
                .update("metaAnual", meta)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}