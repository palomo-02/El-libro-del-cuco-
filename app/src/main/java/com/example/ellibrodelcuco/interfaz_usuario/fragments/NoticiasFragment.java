package com.example.ellibrodelcuco.interfaz_usuario.fragments;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.interfaz_usuario.adapters.NoticiasExternasAdapter;
import com.example.ellibrodelcuco.modelo.NoticiaExterna;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragmento de noticias: consume datos de la API de The Guardian.
 * Gestiona peticiones HTTP, parseo de JSON y estados de la UI (cargando/error).
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class NoticiasFragment extends Fragment {

    private static final String TAG = "NoticiasFragment";

    private RecyclerView rvNoticias;
    private View progressBar;
    private View llEstadoVacio;
    private NoticiasExternasAdapter adapter;
    private final List<NoticiaExterna> listaNoticias = new ArrayList<>();
    private RequestQueue requestQueue;

    private static final String BASE_URL = "https://content.guardianapis.com/search"
            + "?show-fields=headline,byline,thumbnail,trailText"
            + "&page-size=20"
            + "&q=libros";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_noticias, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvNoticias = view.findViewById(R.id.rvNoticias);
        progressBar = view.findViewById(R.id.progressBarNoticias);
        llEstadoVacio = view.findViewById(R.id.llEstadoVacio);

        adapter = new NoticiasExternasAdapter(listaNoticias);
        rvNoticias.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNoticias.setAdapter(adapter);

        view.findViewById(R.id.btnReintentar)
                .setOnClickListener(v -> cargarNoticias(BASE_URL + "&section=books", "The Guardian Books"));

        requestQueue = Volley.newRequestQueue(requireContext().getApplicationContext());

        // Lanzamos la carga inicial
        cargarNoticias(BASE_URL + "&section=books", "The Guardian Books");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cancelamos las peticiones pendientes para no malgastar recursos al salir del fragmento
        if (requestQueue != null) requestQueue.cancelAll(this);
    }

    // ── Carga de datos ──────────────────────────────────────────────────────

    private void cargarNoticias(String urlBase, String fuente) {
        if (!isAdded() || getContext() == null) return;
        mostrarCargando(true);

        String apiKey = getString(R.string.guardian_api_key);
        String url = urlBase + "&api-key=" + apiKey;

        // Petición HTTP usando Volley: obtiene los datos de la web
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    if (!isAdded() || getContext() == null) return;
                    mostrarCargando(false);

                    // Procesamos el JSON recibido y actualizamos la lista
                    List<NoticiaExterna> noticias = parsearRespuesta(response, fuente);
                    listaNoticias.clear();
                    listaNoticias.addAll(noticias);

                    if (listaNoticias.isEmpty()) {
                        mostrarEstadoVacio(true);
                    } else {
                        mostrarEstadoVacio(false);
                        rvNoticias.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                },
                error -> {
                    // Gestión de errores: si falla la red, avisamos al usuario
                    if (!isAdded() || getContext() == null) return;
                    mostrarCargando(false);
                    mostrarEstadoVacio(true);
                    Toast.makeText(getContext(), R.string.error_conexion, Toast.LENGTH_SHORT).show();
                }) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> h = new java.util.HashMap<>();
                h.put("User-Agent", "ElLibroDelCuco-Android/1.0");
                return h;
            }
        };

        req.setTag(this);
        // Política de reintentos: 15s de espera y 1 reintento si falla la conexión
        req.setRetryPolicy(new DefaultRetryPolicy(15_000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(req);
    }

    // ── Parser JSON ──────────────────────────────────────────────────────────

    private List<NoticiaExterna> parsearRespuesta(JSONObject root, String fuente) {
        List<NoticiaExterna> noticias = new ArrayList<>();
        try {
            JSONObject response = root.optJSONObject("response");
            if (response == null) return noticias;

            JSONArray results = response.optJSONArray("results");
            if (results == null) return noticias;

            // Recorremos el JSON para convertir cada objeto en un modelo NoticiaExterna
            for (int i = 0; i < results.length(); i++) {
                JSONObject item = results.getJSONObject(i);
                JSONObject fields = item.optJSONObject("fields");

                // Limpiamos los textos porque la API devuelve etiquetas HTML crudas
                String titulo = limpiarHtml(fields != null ? fields.optString("headline", item.optString("webTitle")) : item.optString("webTitle"));
                String desc = limpiarHtml(fields != null ? fields.optString("trailText", "") : "");
                String autor = (fields != null) ? fields.optString("byline", fuente) : fuente;
                String fecha = recortarFecha(item.optString("webPublicationDate", ""));
                String imgUrl = (fields != null) ? fields.optString("thumbnail", "").replace("http:", "https:") : "";
                String link = item.optString("webUrl", "");

                if (!titulo.isEmpty()) {
                    noticias.add(new NoticiaExterna(titulo, desc, autor, link, imgUrl, fecha));
                }
            }
        } catch (Exception e) { /* Error silencioso */ }
        return noticias;
    }

    // ── Utilidades de limpieza ───────────────────────────────────────────────

    private String limpiarHtml(String texto) {
        // Convertimos etiquetas como <b> o <p> en texto plano
        if (texto == null || texto.isEmpty()) return "";
        return Html.fromHtml(texto, Html.FROM_HTML_MODE_COMPACT).toString().trim();
    }

    private String recortarFecha(String texto) {
        // Formateamos la fecha larga (ISO) a una cadena más corta
        if (texto == null || texto.isEmpty()) return "";
        if (texto.contains("T")) texto = texto.substring(0, 10);
        return texto;
    }

    // ── Helpers de UI ────────────────────────────────────────────────────────

    private void mostrarCargando(boolean cargando) {
        if (progressBar == null) return;
        progressBar.setVisibility(cargando ? View.VISIBLE : View.GONE);
        if (cargando) {
            if (llEstadoVacio != null) llEstadoVacio.setVisibility(View.GONE);
            rvNoticias.setVisibility(View.GONE);
        }
    }

    private void mostrarEstadoVacio(boolean vacio) {
        if (llEstadoVacio == null) return;
        llEstadoVacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        rvNoticias.setVisibility(vacio ? View.GONE : View.VISIBLE);
    }
}