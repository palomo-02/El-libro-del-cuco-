package com.example.ellibrodelcuco.data.repository;

import com.example.ellibrodelcuco.data.callback.Callback;
import com.google.firebase.firestore.AggregateField;
import com.google.firebase.firestore.AggregateQuery;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class EstadisticasRepository {

    private static EstadisticasRepository instance;
    private final FirebaseFirestore db;

    private EstadisticasRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized EstadisticasRepository getInstance() {
        if (instance == null) instance = new EstadisticasRepository();
        return instance;
    }

    // saca la media de todas las reseñas del usuario usando agregacion de firestore
    public void obtenerMediaValoraciones(String uid, Callback<Double> callback) {
        Query query = db.collectionGroup("resenas").whereEqualTo("uid", uid);
        AggregateQuery aggregateQuery = query.aggregate(AggregateField.average("nota"));

        aggregateQuery.get(AggregateSource.SERVER).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AggregateQuerySnapshot snapshot = task.getResult();
                Double media = snapshot.get(AggregateField.average("nota"));
                callback.onSuccess(media != null ? media : 0.0);
            } else {
                callback.onError(task.getException() != null
                        ? task.getException().getMessage() : "Error calculando media");
            }
        });
    }
}