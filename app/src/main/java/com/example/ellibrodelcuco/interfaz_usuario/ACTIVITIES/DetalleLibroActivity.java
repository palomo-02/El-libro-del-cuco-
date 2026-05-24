package com.example.ellibrodelcuco.interfaz_usuario.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.data.repository.BibliotecaRepository;
import com.example.ellibrodelcuco.interfaz_usuario.adapters.ResenaAdapter;
import com.example.ellibrodelcuco.modelo.Libro;
import com.example.ellibrodelcuco.modelo.Resena;
import com.example.ellibrodelcuco.utils.LogrosHelper;
import com.example.ellibrodelcuco.utils.RachaHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla de Detalle de Libro: Centraliza las acciones individuales de lectura
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class DetalleLibroActivity extends AppCompatActivity {

    private ImageView ivPortada;
    private TextView tvTitulo, tvAutor, tvSinopsis, tvEstado, tvPorcentaje, tvMediaResenas;
    private Button btnPendiente, btnLeyendo, btnLeido, btnQuieroLeer, btnEliminar, btnRecomendar, btnGuardarProgreso;
    private ProgressBar pbProgreso;
    private EditText etPaginaActual;
    private RatingBar rbNuevaResena;
    private EditText etTextoResena;
    private Button btnPublicarResena;
    private RecyclerView rvResenas;
    private ResenaAdapter resenaAdapter;
    private List<Resena> listaResenas;

    private Libro libroActual;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private com.google.firebase.firestore.ListenerRegistration resenasListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_libro);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ivPortada = findViewById(R.id.ivDetallePortada);
        tvTitulo = findViewById(R.id.tvDetalleTitulo);
        tvAutor = findViewById(R.id.tvDetalleAutor);
        tvSinopsis = findViewById(R.id.tvDetalleSinopsis);
        tvEstado = findViewById(R.id.tvDetalleEstado);
        tvPorcentaje = findViewById(R.id.tvPorcentaje);
        tvMediaResenas = findViewById(R.id.tvMediaResenas);
        pbProgreso = findViewById(R.id.pbProgreso);
        etPaginaActual = findViewById(R.id.etPaginaActual);
        btnPendiente = findViewById(R.id.btnPendiente);
        btnLeyendo = findViewById(R.id.btnLeyendo);
        btnLeido = findViewById(R.id.btnLeido);
        btnQuieroLeer = findViewById(R.id.btnQuieroLeer);
        btnEliminar = findViewById(R.id.btnEliminar);
        btnRecomendar = findViewById(R.id.btnRecomendar);
        btnGuardarProgreso = findViewById(R.id.btnGuardarProgreso);
        rbNuevaResena = findViewById(R.id.rbNuevaResena);
        etTextoResena = findViewById(R.id.etTextoResena);
        btnPublicarResena = findViewById(R.id.btnPublicarResena);
        rvResenas = findViewById(R.id.rvResenas);

        // Preparamos la lista donde se mostrarán los comentarios de la comunidad
        listaResenas = new ArrayList<>();
        resenaAdapter = new ResenaAdapter(listaResenas);
        rvResenas.setLayoutManager(new LinearLayoutManager(this));
        rvResenas.setAdapter(resenaAdapter);
        rvResenas.setNestedScrollingEnabled(false);

        // Comprobamos si la pantalla anterior nos ha pasado los datos de un libro
        if (getIntent().hasExtra("libro")) {
            libroActual = (Libro) getIntent().getSerializableExtra("libro");
            cargarDatosEnPantalla();
            cargarDatosFirestore();
            cargarResenas();
        } else {
            Toast.makeText(this, "Error al cargar el libro", Toast.LENGTH_SHORT).show();
            finish(); // Si falla, cerramos la pantalla para no mostrarla vacía
        }

        // Asignamos qué hace cada botón al pulsarlo
        btnPendiente.setOnClickListener(v -> actualizarEstado("Pendiente"));
        btnLeyendo.setOnClickListener(v -> actualizarEstado("Leyendo"));
        btnLeido.setOnClickListener(v -> actualizarEstado("Leído"));
        btnQuieroLeer.setOnClickListener(v -> actualizarEstado("Quiero leer"));
        btnEliminar.setOnClickListener(v -> borrarLibro());
        btnRecomendar.setOnClickListener(v -> compartirLibro());
        btnGuardarProgreso.setOnClickListener(v -> guardarProgreso());
        btnPublicarResena.setOnClickListener(v -> publicarResena());
    }

    private void cargarDatosEnPantalla() {
        // Pintamos los datos básicos que vienen del objeto Libro
        tvTitulo.setText(libroActual.getTitulo());
        tvAutor.setText(libroActual.getAutor());
        tvSinopsis.setText(libroActual.getSinopsis());
        tvEstado.setText("ESTADO: " + (libroActual.getEstado() != null ? libroActual.getEstado().toUpperCase() : ""));

        // Descargamos la portada usando Picasso
        if (libroActual.getPortadaUrl() != null && !libroActual.getPortadaUrl().isEmpty()) {
            String url = libroActual.getPortadaUrl().replace("http:", "https:");
            Picasso.get().load(url).into(ivPortada);
        } else {
            ivPortada.setImageResource(R.drawable.logo);
        }

        actualizarProgressBar(libroActual.getPaginaActual(), libroActual.getTotalPaginas());
        if (libroActual.getPaginaActual() > 0) {
            etPaginaActual.setText(String.valueOf(libroActual.getPaginaActual()));
        }
    }

    private void cargarDatosFirestore() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        // Pedimos al repositorio el progreso real guardado en la nube para este usuario
        BibliotecaRepository.getInstance().obtenerLibro(uid, libroActual.getTitulo(), libroActual.getAutor(), new com.example.ellibrodelcuco.data.callback.Callback<Libro>() {
            @Override
            public void onSuccess(Libro libroDB) {
                if (libroDB != null) {
                    libroActual.setPaginaActual(libroDB.getPaginaActual());
                    libroActual.setTotalPaginas(libroDB.getTotalPaginas());

                    if (libroDB.getPaginaActual() > 0) {
                        etPaginaActual.setText(String.valueOf(libroDB.getPaginaActual()));
                    }
                    actualizarProgressBar(libroDB.getPaginaActual(), libroDB.getTotalPaginas());
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(DetalleLibroActivity.this, "Error al cargar datos: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarProgressBar(int pagActual, int pagTotal) {
        // en base al porcentaje se llena mas o menos la barra
        if (pagTotal > 0) {
            int porcentaje = pagActual * 100 / pagTotal;
            pbProgreso.setProgress(porcentaje);
            tvPorcentaje.setText(pagActual + " / " + pagTotal + " páginas (" + porcentaje + "%)");
        } else {
            pbProgreso.setProgress(0);
            tvPorcentaje.setText("Sin información de páginas");
        }
    }

    private void guardarProgreso() {
        if (mAuth.getCurrentUser() == null) return;
        String texto = etPaginaActual.getText().toString().trim();
        if (TextUtils.isEmpty(texto)) return;

        // Comprobaciones de seguridad para evitar que la app pete si el usuario escribe texto en lugar de números
        int nuevaPagina;
        try {
            nuevaPagina = Integer.parseInt(texto);
        } catch (NumberFormatException e) {
            etPaginaActual.setError("Introduce un número válido");
            return;
        }
        if (nuevaPagina < 0) {
            etPaginaActual.setError("El número de página no puede ser negativo");
            return;
        }
        if (libroActual.getTotalPaginas() > 0 && nuevaPagina > libroActual.getTotalPaginas()) {
            etPaginaActual.setError("Página mayor que el total (" + libroActual.getTotalPaginas() + ")");
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        libroActual.setPaginaActual(nuevaPagina);

        // Mandamos el progreso actualizado al repositorio para que lo guarde en Firestore
        BibliotecaRepository.getInstance().guardarLibro(uid, libroActual, new com.example.ellibrodelcuco.data.callback.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                actualizarProgressBar(nuevaPagina, libroActual.getTotalPaginas());
                Toast.makeText(DetalleLibroActivity.this, "Progreso guardado", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(DetalleLibroActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarEstado(String nuevoEstado) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        libroActual.setEstado(nuevoEstado);
        if ("Leído".equalsIgnoreCase(nuevoEstado)) {
            libroActual.setFechaLeido(new java.util.Date()); // Guardamos la fecha para las estadísticas
        }

        // Actualizamos la base de datos y verificamos los logros y tal
        BibliotecaRepository.getInstance().guardarLibro(uid, libroActual, new com.example.ellibrodelcuco.data.callback.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(DetalleLibroActivity.this, "Libro movido a " + nuevoEstado, Toast.LENGTH_SHORT).show();
                tvEstado.setText("ESTADO: " + nuevoEstado.toUpperCase());
                RachaHelper.actualizarRacha(uid, db, DetalleLibroActivity.this);
                LogrosHelper.comprobar(uid, db, DetalleLibroActivity.this);
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(DetalleLibroActivity.this, "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void borrarLibro() {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();


        new AlertDialog.Builder(this)
                .setTitle("Eliminar libro")
                .setMessage("¿Seguro que quieres eliminar \"" + libroActual.getTitulo() + "\" de tu biblioteca?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    BibliotecaRepository.getInstance().eliminarLibro(uid, libroActual.getTitulo(), libroActual.getAutor(), new com.example.ellibrodelcuco.data.callback.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(DetalleLibroActivity.this, "Libro eliminado", Toast.LENGTH_SHORT).show();
                            finish(); // para volver a la pantalla anterior
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Toast.makeText(DetalleLibroActivity.this, "Error al eliminar: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void compartirLibro() {
        // Sistema de recomendacion nativo de AS
        String mensaje = "¡Te recomiendo '" + libroActual.getTitulo() + "' de "
                + libroActual.getAutor() + "! \uD83D\uDCDA";
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mensaje);
        startActivity(Intent.createChooser(intent, "Compartir libro con..."));
    }

    private void cargarResenas() {
        if (mAuth.getCurrentUser() == null) return;
        String tituloGlobal = libroActual.getTitulo();

        // Escuchamos en tiempo real las valoraciones públicas de este libro concreto
        resenasListener = db.collection("libros_globales")
                .document(tituloGlobal)
                .collection("resenas")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        listaResenas.clear();
                        float sumaNotas = 0;
                        for (QueryDocumentSnapshot doc : value) {
                            Resena r = doc.toObject(Resena.class);
                            listaResenas.add(r);
                            sumaNotas += r.getNota();
                        }
                        resenaAdapter.notifyDataSetChanged();

                        // Recalculamos la nota media para mostrarla visualmente
                        int total = listaResenas.size();
                        if (total > 0) {
                            float media = sumaNotas / total;
                            tvMediaResenas.setText(String.format("⭐ %.1f (%d reseña%s)", media, total, total == 1 ? "" : "s"));
                        } else {
                            tvMediaResenas.setText("Sin valoraciones aún");
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resenasListener != null) {
            resenasListener.remove();
            resenasListener = null;
        }
    }

    private void publicarResena() {
        if (mAuth.getCurrentUser() == null) return;
        FirebaseUser user = mAuth.getCurrentUser();
        String texto = etTextoResena.getText().toString().trim();
        float nota = rbNuevaResena.getRating();

        if (TextUtils.isEmpty(texto) || nota == 0) {
            Toast.makeText(this, "Añade una valoración y texto", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombre = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                ? user.getDisplayName() : "Lector Cuco";
        Resena resena = new Resena(user.getUid(), nombre, texto, nota, new Date());

        String tituloGlobal = libroActual.getTitulo();

        // Subimos la reseña a la colección pública. Si el usuario ya comentó, se sobreescribe la suya
        db.collection("libros_globales")
                .document(tituloGlobal)
                .collection("resenas")
                .document(user.getUid())
                .set(resena)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Reseña publicada en la Comunidad", Toast.LENGTH_SHORT).show();
                    etTextoResena.setText("");
                    rbNuevaResena.setRating(0);
                    LogrosHelper.comprobarResena(user.getUid(), db, this);

                    actualizarLibroGlobal(nota);
                });
    }

    private void actualizarLibroGlobal(float nuevaNota) {
        // Conteo rapido para qeu sea mas accesible
        String titulo = libroActual.getTitulo();
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
                            // Fórmula matemática de la nueva media ponderada
                            mediaNotas = ((mn * tr) + nuevaNota) / totalResenas;
                        }
                    }

                    Map<String, Object> datos = new HashMap<>();
                    datos.put("titulo", titulo);
                    datos.put("autor", libroActual.getAutor() != null ? libroActual.getAutor() : "");
                    datos.put("portadaUrl", libroActual.getPortadaUrl() != null
                            ? libroActual.getPortadaUrl().replace("http:", "https:") : "");
                    datos.put("mediaNotas", mediaNotas);
                    datos.put("totalResenas", totalResenas);

                    db.collection("libros_globales").document(titulo)
                            .set(datos, SetOptions.merge());
                });
    }
}