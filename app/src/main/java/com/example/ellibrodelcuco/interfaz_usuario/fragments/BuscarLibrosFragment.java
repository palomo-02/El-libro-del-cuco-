package com.example.ellibrodelcuco.interfaz_usuario.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ellibrodelcuco.data.repository.LibrosApiRepository;
import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.data.callback.SnapshotCallback;
import com.example.ellibrodelcuco.data.repository.AuthRepository;
import com.example.ellibrodelcuco.data.repository.BibliotecaRepository;
import com.example.ellibrodelcuco.interfaz_usuario.activities.DetalleLibroActivity;
import com.example.ellibrodelcuco.interfaz_usuario.adapters.LibroAdapter;
import com.example.ellibrodelcuco.modelo.Libro;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragmento de búsqueda. Gestiona la consulta a la API de Google Books,
 * el escaneo de códigos de barras (ISBN) y la persistencia en la biblioteca del usuario.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class BuscarLibrosFragment extends Fragment {

    private EditText etBusqueda;
    private ProgressBar progressBar;
    private RecyclerView rvResultados;
    private LibroAdapter adapter;
    private List<Libro> listaResultados;

    private GmsBarcodeScanner scanner;
    private ListenerRegistration bibliotecaListener;

    private int currentStartIndex = 0;
    private String currentQuery = "";
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_buscar_libros, container, false);

        // Preparamos el escáner para que solo busque códigos de barras EAN
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8)
                .build();
        scanner = GmsBarcodeScanning.getClient(requireContext(), options);

        etBusqueda   = view.findViewById(R.id.etBusqueda);
        progressBar  = view.findViewById(R.id.progressBar);
        rvResultados = view.findViewById(R.id.rvResultados);

        listaResultados = new ArrayList<>();
        adapter = new LibroAdapter(listaResultados, this::abrirDetalle);
        adapter.setOnLibroLongClickListener(this::mostrarDialogoAcciones);
        rvResultados.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResultados.setAdapter(adapter);

        // Acción al pulsar el botón de buscar
        view.findViewById(R.id.btnBuscar).setOnClickListener(v -> {
            String termino = etBusqueda.getText().toString().trim();
            if (!termino.isEmpty()) {
                currentQuery = termino;
                currentStartIndex = 0;
                listaResultados.clear();
                adapter.notifyDataSetChanged();
                buscarEnGoogle(currentQuery, currentStartIndex);
            }
        });

        // Detección de scroll: si el usuario llega al final, cargamos más resultados automáticamente
        rvResultados.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null && !isLoading) {
                        if (layoutManager.findLastCompletelyVisibleItemPosition() >= adapter.getItemCount() - 2) {
                            cargarMasLibros();
                        }
                    }
                }
            }
        });

        // Activamos la cámara al pulsar el botón de ISBN
        view.findViewById(R.id.btnEscanerISBN)
                .setOnClickListener(v -> iniciarEscaner());

        // Escuchamos la biblioteca para poner los "check" verdes en los libros ya guardados
        sincronizarConBiblioteca();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiamos la suscripción al cerrar la vista para evitar consumos innecesarios
        if (bibliotecaListener != null) {
            bibliotecaListener.remove();
            bibliotecaListener = null;
        }
    }

    // --- Métodos privados ---

    private void sincronizarConBiblioteca() {
        FirebaseUser user = AuthRepository.getInstance().getCurrentUser();
        if (user == null) return;

        // Recibimos en tiempo real los libros del usuario para saber cuáles están en su biblioteca
        bibliotecaListener = BibliotecaRepository.getInstance()
                .escucharBiblioteca(user.getUid(), new SnapshotCallback<Libro>() {
                    @Override
                    public void onChange(List<Libro> result) {
                        if (!isAdded() || getContext() == null) return;
                        List<String> titulos = new ArrayList<>();
                        for (Libro l : result) titulos.add(l.getTitulo());
                        adapter.setTitulosGuardados(titulos);
                    }
                    @Override
                    public void onError(String errorMessage) {}
                });
    }

    private void iniciarEscaner() {
        // Lanzamos el lector de códigos. Si encuentra un ISBN, lo escribimos en el buscador y buscamos
        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    if (!isAdded() || getContext() == null) return;
                    String rawValue = barcode.getRawValue();
                    if (rawValue != null && !rawValue.isEmpty()) {
                        etBusqueda.setText(rawValue);
                        currentQuery = "isbn:" + rawValue;
                        currentStartIndex = 0;
                        listaResultados.clear();
                        adapter.notifyDataSetChanged();
                        buscarEnGoogle(currentQuery, currentStartIndex);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Toast.makeText(getContext(), getString(R.string.error_escaner), Toast.LENGTH_SHORT).show();
                });
    }

    private void cargarMasLibros() {
        // Sumamos 20 al contador para pedir la siguiente página de resultados
        if (currentQuery.isEmpty() || isLoading) return;
        currentStartIndex += 20;
        buscarEnGoogle(currentQuery, currentStartIndex);
    }

    private void buscarEnGoogle(String query, int startIndex) {
        if (!isAdded() || getContext() == null) return;
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        // Llamamos al repositorio para hacer la petición HTTP a la API de Google
        LibrosApiRepository.getInstance(requireContext()).buscarLibrosEnGoogle(query, startIndex, new com.example.ellibrodelcuco.data.callback.Callback<List<Libro>>() {
            @Override
            public void onSuccess(List<Libro> result) {
                if (!isAdded() || getContext() == null) return;
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                if (result != null && !result.isEmpty()) {
                    listaResultados.addAll(result);
                    adapter.notifyDataSetChanged();
                } else if (startIndex == 0) {
                    Toast.makeText(getContext(), getString(R.string.sin_resultados), Toast.LENGTH_SHORT).show();
                    // Si buscamos por ISBN y falla, intentamos una búsqueda por texto por si acaso
                    if (query.startsWith("isbn:")) {
                        String termino = query.replace("isbn:", "").trim();
                        if (!termino.isEmpty()) {
                            currentQuery = termino;
                            currentStartIndex = 0;
                            buscarEnGoogle(currentQuery, currentStartIndex);
                        }
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null) return;
                isLoading = false;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoAcciones(Libro libro) {
        // Diálogo para elegir qué hacer con el libro: ver detalle o añadir a una lista
        if (!isAdded()) return;
        String[] opciones = {
                getString(R.string.accion_ver_detalle),
                getString(R.string.estado_pendiente),
                getString(R.string.estado_leyendo),
                getString(R.string.estado_leido),
                getString(R.string.estado_quiero_leer)
        };
        new AlertDialog.Builder(requireContext())
                .setTitle(libro.getTitulo())
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        abrirDetalle(libro);
                    } else {
                        String[] estados = {"", "Pendiente", "Leyendo", "Leído", "Quiero leer"};
                        libro.setEstado(estados[which]);
                        guardarLibro(libro);
                    }
                })
                .show();
    }

    private void abrirDetalle(Libro libro) {
        // Saltamos a la actividad de detalle pasando el objeto libro
        Intent intent = new Intent(getContext(), DetalleLibroActivity.class);
        intent.putExtra("libro", libro);
        startActivity(intent);
    }

    private void guardarLibro(Libro libro) {
        // Guardamos el libro seleccionado en Firestore bajo la biblioteca del usuario
        FirebaseUser user = AuthRepository.getInstance().getCurrentUser();
        if (user == null) return;
        BibliotecaRepository.getInstance().guardarLibro(user.getUid(), libro, new com.example.ellibrodelcuco.data.callback.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), getString(R.string.libro_anadido), Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || getContext() == null) return;
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }
}