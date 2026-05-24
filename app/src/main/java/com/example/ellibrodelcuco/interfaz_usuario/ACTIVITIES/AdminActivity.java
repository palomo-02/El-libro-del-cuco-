package com.example.ellibrodelcuco.interfaz_usuario.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.data.repository.AuthRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel de administración: Dashboard para monitorizar métricas globales,
 * listar usuarios y gestionar acciones de moderación (banear).
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class AdminActivity extends AppCompatActivity {

    private TextView tvTotalUsuarios, tvTotalLibros;
    private LinearLayout llTop5;
    private RecyclerView rvUsuarios;
    private MaterialButton btnCerrarSesionAdmin;

    private FirebaseFirestore db;
    private UsuariosAdapter usuariosAdapter;
    private final List<UsuarioItem> listaUsuarios = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();

        tvTotalUsuarios = findViewById(R.id.tvTotalUsuarios);
        tvTotalLibros = findViewById(R.id.tvTotalLibros);
        llTop5 = findViewById(R.id.llTop5);
        rvUsuarios = findViewById(R.id.rvUsuarios);
        btnCerrarSesionAdmin = findViewById(R.id.btnCerrarSesionAdmin);

        usuariosAdapter = new UsuariosAdapter(listaUsuarios);
        rvUsuarios.setLayoutManager(new LinearLayoutManager(this));
        rvUsuarios.setAdapter(usuariosAdapter);
        rvUsuarios.setNestedScrollingEnabled(false);

        btnCerrarSesionAdmin.setOnClickListener(v -> {
            AuthRepository.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        cargarDatos();
    }


    private void cargarDatos() {
        // las tres consultas para que se vean en l apantalla
        cargarUsuarios();
        cargarTotalLibros();
        cargarTop5();
    }

    private void cargarUsuarios() {
        // 1. Usamos una agregación (count) para obtener el total exacto sin descargar documentos
        db.collection("usuarios").count().get(com.google.firebase.firestore.AggregateSource.SERVER)
                .addOnSuccessListener(task -> tvTotalUsuarios.setText(String.valueOf(task.getCount())));

        // 2. Descargamos los primeros 50 usuarios para la lista de gestión
        db.collection("usuarios")
                .limit(50)
                .get()
                .addOnSuccessListener(snap -> {
                    listaUsuarios.clear();
                    for (QueryDocumentSnapshot doc : snap) {
                        listaUsuarios.add(new UsuarioItem(doc.getId(), doc.getString("email"), doc.getLong("rachaActual")));
                    }
                    usuariosAdapter.notifyDataSetChanged();
                });
    }

    private void cargarTotalLibros() {
        // Obtenemos el volumen de libros en la base de datos global
        db.collection("libros_globales")
                .get()
                .addOnSuccessListener(snap -> tvTotalLibros.setText(String.valueOf(snap.size())));
    }

    private void cargarTop5() {
        // Ordenacion de las , medias
        db.collection("libros_globales")
                .orderBy("mediaNotas", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(snap -> {
                    llTop5.removeAllViews();
                    int posicion = 1;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        // Construcción manual de la fila del ranking
                        View fila = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, llTop5, false);
                        ((TextView) fila.findViewById(android.R.id.text1)).setText((posicion==1 ? "🥇" : posicion==2 ? "🥈" : "🥉") + " " + doc.getString("titulo"));
                        ((TextView) fila.findViewById(android.R.id.text2)).setText(doc.getString("autor") + " ⭐ " + doc.getDouble("mediaNotas"));
                        llTop5.addView(fila);
                        posicion++;
                    }
                });
    }

    static class UsuarioItem {
        final String uid; final String email; final long rachaActual;
        UsuarioItem(String uid, String email, long rachaActual) {
            this.uid = uid; this.email = email; this.rachaActual = rachaActual;
        }
    }

    static class UsuariosAdapter extends RecyclerView.Adapter<UsuariosAdapter.VH> {
        private final List<UsuarioItem> lista;
        UsuariosAdapter(List<UsuarioItem> lista) { this.lista = lista; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usuario_admin, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            UsuarioItem u = lista.get(pos);
            h.tvEmail.setText(u.email);
            h.tvRacha.setText("🔥 " + u.rachaActual);

            // El cambio del baneo se manda a FS
            h.btnEliminar.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(h.itemView.getContext())
                        .setTitle("Suspender cuenta")
                        .setPositiveButton("Banear", (d, w) -> {
                            FirebaseFirestore.getInstance().collection("usuarios").document(u.uid)
                                    .update("estado", "baneado");
                        }).show();
            });
        }

        @Override public int getItemCount() { return lista.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvEmail, tvRacha; MaterialButton btnEliminar;
            VH(@NonNull View v) {
                super(v);
                tvEmail = v.findViewById(R.id.tvEmailUsuario);
                tvRacha = v.findViewById(R.id.tvRachaUsuario);
                btnEliminar = v.findViewById(R.id.btnEliminarUsuario);
            }
        }
    }
}