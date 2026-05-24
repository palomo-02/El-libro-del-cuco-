package com.example.ellibrodelcuco.interfaz_usuario.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.data.repository.ComunidadRepository;
import com.example.ellibrodelcuco.interfaz_usuario.activities.DetalleLibroActivity;
import com.example.ellibrodelcuco.interfaz_usuario.adapters.LibroAdapter;
import com.example.ellibrodelcuco.modelo.Libro;
import com.example.ellibrodelcuco.databinding.FragmentComunidadBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragmento de la comunidad: muestra el ranking global, permite filtrar libros
 * y carga datos bajo demanda mediante paginación.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class ComunidadFragment extends Fragment {

    private FragmentComunidadBinding binding;
    private LibroAdapter adapter;
    private List<Libro> listaTodos;
    private List<Libro> listaFiltrada;

    private com.google.firebase.firestore.DocumentSnapshot ultimoDocumento = null;
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentComunidadBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        listaTodos = new ArrayList<>();
        listaFiltrada = new ArrayList<>();

        adapter = new LibroAdapter(listaFiltrada, libro -> {
            if (!isAdded() || getContext() == null) return;
            Intent intent = new Intent(getContext(), DetalleLibroActivity.class);
            intent.putExtra("libro", libro);
            startActivity(intent);
        });

        binding.rvComunidad.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvComunidad.setAdapter(adapter);

        // Filtro por texto: escaneamos la lista cargada en memoria cada vez que el usuario escribe
        binding.etBusquedaComunidad.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrar(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnBuscarComunidad.setOnClickListener(v -> filtrar(binding.etBusquedaComunidad.getText().toString()));

        // Scroll infinito: detectamos cuando el usuario llega al final de la lista para pedir más datos
        binding.rvComunidad.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                    if (layoutManager != null && !isLoading) {
                        if (layoutManager.findLastCompletelyVisibleItemPosition() >= adapter.getItemCount() - 2) {
                            cargarLibrosComunidad();
                        }
                    }
                }
            }
        });

        cargarLibrosComunidad();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Evitamos fugas de memoria limpiando el binding
    }

    private void cargarLibrosComunidad() {
        if (binding == null || isLoading) return;
        isLoading = true;
        binding.progressBarComunidad.setVisibility(View.VISIBLE);

        // Llamada al repositorio con paginación usando 'ultimoDocumento' para marcar el cursor
        ComunidadRepository.getInstance().obtenerLibrosPorRating(20, ultimoDocumento, new com.example.ellibrodelcuco.data.callback.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onSuccess(java.util.Map<String, Object> result) {
                if (!isAdded() || binding == null) return;
                isLoading = false;
                binding.progressBarComunidad.setVisibility(View.GONE);

                List<Libro> libros = (List<Libro>) result.get("libros");
                // Actualizamos el puntero para la siguiente carga paginada
                ultimoDocumento = (com.google.firebase.firestore.DocumentSnapshot) result.get("ultimoDocumento");

                if (libros != null && !libros.isEmpty()) {
                    listaTodos.addAll(libros);
                    filtrar(binding.etBusquedaComunidad.getText().toString());
                }

                binding.tvVacioComunidad.setVisibility(listaTodos.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onError(String errorMessage) {
                if (!isAdded() || binding == null) return;
                isLoading = false;
                binding.progressBarComunidad.setVisibility(View.GONE);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filtrar(String query) {
        listaFiltrada.clear();
        if (query.isEmpty()) {
            listaFiltrada.addAll(listaTodos);
        } else {
            // Lógica de búsqueda local: comparamos el texto introducido con título y autor
            String q = query.toLowerCase();
            for (Libro libro : listaTodos) {
                if ((libro.getTitulo() != null && libro.getTitulo().toLowerCase().contains(q)) ||
                        (libro.getAutor() != null && libro.getAutor().toLowerCase().contains(q))) {
                    listaFiltrada.add(libro);
                }
            }
        }
        adapter.notifyDataSetChanged(); // Refrescamos el adaptador con los resultados filtrados
    }
}