package com.example.ellibrodelcuco.interfaz_usuario.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.data.callback.Callback;
import com.example.ellibrodelcuco.data.callback.SnapshotCallback;
import com.example.ellibrodelcuco.data.repository.AuthRepository;
import com.example.ellibrodelcuco.data.repository.BibliotecaRepository;
import com.example.ellibrodelcuco.data.repository.UsuarioRepository;
import com.example.ellibrodelcuco.databinding.FragmentPerfilBinding;
import com.example.ellibrodelcuco.interfaz_usuario.activities.DetalleLibroActivity;
import com.example.ellibrodelcuco.interfaz_usuario.activities.LoginActivity;
import com.example.ellibrodelcuco.interfaz_usuario.adapters.LibroAdapter;
import com.example.ellibrodelcuco.modelo.Libro;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fragment de perfil: Gestiona la identidad del usuario, su progreso anual,
 * logros desbloqueados y biblioteca personal con filtrado dinámico.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding binding;
    private LibroAdapter adapter;

    private List<Libro> listaTodos     = new ArrayList<>();
    private List<Libro> listaFiltrada  = new ArrayList<>();

    private ListenerRegistration bibliotecaListener;
    private ListenerRegistration perfilListener;

    private String estadoActual = "Pendiente";
    private int metaGuardada = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPerfilBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializamos los componentes de la interfaz
        configurarAdapter();
        configurarTabs();
        configurarBusqueda();
        configurarPerfil();
        configurarCerrarSesion();
        configurarMeta();
        cargarBiblioteca();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        //  Cancelamos los listeners al destruir la vista para no dejar hilos abiertos
        if (bibliotecaListener != null) { bibliotecaListener.remove(); bibliotecaListener = null; }
        if (perfilListener    != null) { perfilListener.remove();     perfilListener    = null; }
        binding = null;
    }


    private void configurarAdapter() {
        adapter = new LibroAdapter(listaFiltrada,
                libro -> {
                    if (!isAdded() || getContext() == null) return;
                    Intent intent = new Intent(getContext(), DetalleLibroActivity.class);
                    intent.putExtra("libro", libro);
                    startActivity(intent);
                }
        );

        adapter.setOnLibroLongClickListener(libro -> {
            if (!isAdded() || getContext() == null) return;
            Intent intent = new Intent(getContext(), DetalleLibroActivity.class);
            intent.putExtra("libro", libro);
            startActivity(intent);
        });

        binding.rvBiblioteca.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvBiblioteca.setAdapter(adapter);
    }

    private void configurarTabs() {
        // Al cambiar de tab, actualizamos el estado que queremos filtrar (Leyendo/Leído/etc)
        binding.tabLayoutCategorias.addOnTabSelectedListener(
                new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                    @Override public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                        String[] estados = {"Pendiente", "Leyendo", "Leído", "Quiero leer"};
                        estadoActual = estados[tab.getPosition()];
                        aplicarFiltros(binding.etBuscarBiblioteca.getText().toString());
                    }
                    @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                    @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
                });
    }

    private void configurarBusqueda() {
        // Filtro en tiempo real conforme el usuario escribe
        binding.etBuscarBiblioteca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                aplicarFiltros(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void configurarPerfil() {
        FirebaseUser user = AuthRepository.getInstance().getCurrentUser();
        if (user == null) return;

        binding.tvUserEmail.setText(user.getEmail());

        // Dialogo emergente para cambiar el nombre de usuario
        binding.tvUserName.setOnClickListener(v -> {
            EditText et = new EditText(requireContext());
            et.setText(binding.tvUserName.getText().toString().replace(" ✏️", ""));
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.editar_nombre))
                    .setView(et)
                    .setPositiveButton(getString(R.string.guardar), (d, w) -> {
                        String nombre = et.getText().toString().trim();
                        if (!nombre.isEmpty()) {
                            binding.tvUserName.setText(nombre + " ✏️");
                            guardarNombreFirestore(user.getUid(), nombre);
                        }
                    })
                    .show();
        });

        // Escucha en tiempo real los cambios en el perfil del usuario en Firestore
        perfilListener = UsuarioRepository.getInstance().escucharPerfil(user.getUid(),
                new Callback<Map<String, Object>>() {
                    @Override public void onSuccess(Map<String, Object> data) {
                        if (!isAdded() || binding == null) return;

                        if (data.get("nombre") != null) {
                            binding.tvUserName.setText(data.get("nombre") + " ✏️");
                        }

                        Object fotoUrl = data.get("fotoUrl");
                        if (fotoUrl != null && !fotoUrl.toString().trim().isEmpty()) {
                            Picasso.get().load(fotoUrl.toString())
                                    .placeholder(android.R.drawable.ic_menu_my_calendar)
                                    .into(binding.ivProfilePic);
                        } else {
                            binding.ivProfilePic.setImageResource(android.R.drawable.ic_menu_my_calendar);
                        }

                        if (data.get("metaAnual") != null) {
                            try {
                                metaGuardada = Integer.parseInt(data.get("metaAnual").toString());
                                actualizarBarraReto(metaGuardada);
                            } catch (NumberFormatException e) {
                                metaGuardada = 0;
                            }
                        }

                        binding.tvRacha.setText("🔥 " + (data.get("rachaActual") != null ? data.get("rachaActual") : "0") + " días");
                    }
                    @Override public void onError(String errorMessage) {}
                });
    }

    private void guardarNombreFirestore(String uid, String nombre) {
        // Actualizamos en Firestore y también en el perfil de Auth de Firebase para consistencia
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("usuarios").document(uid)
                .set(java.util.Collections.singletonMap("nombre", nombre),
                        com.google.firebase.firestore.SetOptions.merge());

        AuthRepository.getInstance().updateDisplayName(nombre, new Callback<Void>() {
            @Override public void onSuccess(Void result) {}
            @Override public void onError(String errorMessage) {}
        });
    }

    private void configurarCerrarSesion() {
        binding.btnCerrarSesion.setOnClickListener(v -> {
            AuthRepository.getInstance().signOut();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void configurarMeta() {
        binding.btnCambiarMeta.setOnClickListener(v -> {
            EditText et = new EditText(requireContext());
            et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            new AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.meta_anual))
                    .setView(et)
                    .setPositiveButton(getString(R.string.guardar), (d, w) -> {
                        String input = et.getText().toString().trim();
                        if (!input.isEmpty()) {
                            long meta = Long.parseLong(input);
                            FirebaseUser user = AuthRepository.getInstance().getCurrentUser();
                            UsuarioRepository.getInstance().actualizarMetaAnual(user.getUid(), meta, new Callback<Void>() {
                                @Override public void onSuccess(Void r) { actualizarBarraReto((int) meta); }
                                @Override public void onError(String msg) {}
                            });
                        }
                    }).show();
        });
    }

    private void cargarBiblioteca() {
        FirebaseUser user = AuthRepository.getInstance().getCurrentUser();
        if (user == null) return;

        // Carga los libros del usuario en tiempo real
        bibliotecaListener = BibliotecaRepository.getInstance()
                .escucharBiblioteca(user.getUid(), new SnapshotCallback<Libro>() {
                    @Override public void onChange(List<Libro> result) {
                        if (!isAdded() || binding == null) return;
                        listaTodos.clear();
                        listaTodos.addAll(result);
                        aplicarFiltros(binding.etBuscarBiblioteca.getText().toString());
                        actualizarEstadisticas(result);
                        actualizarLogros();
                        actualizarBarraReto(metaGuardada);
                    }
                    @Override public void onError(String msg) {}
                });
    }

    private void actualizarEstadisticas(List<Libro> libros) {
        int leidos = 0;
        for (Libro l : libros) if ("Leído".equals(l.getEstado())) leidos++;
        binding.tvCountLeidos.setText(String.valueOf(leidos));
    }

    private void actualizarLogros() {

        FirebaseUser user = AuthRepository.getInstance().getCurrentUser();
        if (user == null) return;
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("usuarios").document(user.getUid()).collection("logros").get()
                .addOnSuccessListener(snap -> {
                    if (binding == null) return;
                    binding.chipGroupLogros.removeAllViews();
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : snap) {
                        com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(requireContext());
                        chip.setText(doc.getId());
                        binding.chipGroupLogros.addView(chip);
                    }
                });
    }

    private void actualizarBarraReto(int meta) {
        // Calcula el porcentaje de avance respecto a la meta anual
        if (binding == null) return;
        int leidos = 0;
        for (Libro l : listaTodos) if ("Leído".equals(l.getEstado())) leidos++;

        int pct = meta > 0 ? Math.min(leidos * 100 / meta, 100) : 0;
        binding.progressReto.setProgress(pct);
        binding.tvProgresoReto.setText(leidos + " / " + meta + " libros (" + pct + "%)");
    }

    private void aplicarFiltros(String query) {
        //  compara estado actual (tab) y búsqueda de texto
        listaFiltrada.clear();
        for (Libro libro : listaTodos) {
            if (!estadoActual.equals(libro.getEstado())) continue;
            if (!query.isEmpty()) {
                String q = query.toLowerCase();
                if (!(libro.getTitulo().toLowerCase().contains(q))) continue;
            }
            listaFiltrada.add(libro);
        }
        adapter.notifyDataSetChanged();
    }
}