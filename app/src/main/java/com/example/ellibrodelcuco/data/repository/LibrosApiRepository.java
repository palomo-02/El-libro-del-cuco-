package com.example.ellibrodelcuco.data.repository;

import android.content.Context;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.ellibrodelcuco.data.callback.Callback;
import com.example.ellibrodelcuco.modelo.Libro;
import com.example.ellibrodelcuco.utils.GoogleBooksParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class LibrosApiRepository {

    private static final String TAG = "LibrosApiRepository";
    private static final String GOOGLE_BOOKS_API_KEY = "AIzaSyBhRGr9TK9cf2d9TWA3IY76VJeSfyzKRHw";

    private static LibrosApiRepository instance;
    private final RequestQueue requestQueue;

    private LibrosApiRepository(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized LibrosApiRepository getInstance(Context context) {
        if (instance == null) instance = new LibrosApiRepository(context);
        return instance;
    }

    public void buscarLibrosEnGoogle(String query, int startIndex, Callback<List<Libro>> callback) {
        String queryEncoded;
        try {
            queryEncoded = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            queryEncoded = query.replace(" ", "+");
        }

        String url = "https://www.googleapis.com/books/v1/volumes?q=" + queryEncoded
                + "&maxResults=20&startIndex=" + startIndex
                + "&key=" + GOOGLE_BOOKS_API_KEY;

        Log.d(TAG, "Buscando: " + url);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        List<Libro> resultados = new ArrayList<>();
                        if (response.has("items")) {
                            JSONArray items = response.getJSONArray("items");
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.getJSONObject(i);
                                if (!item.has("volumeInfo")) continue;
                                Libro libro = GoogleBooksParser.parse(item.getJSONObject("volumeInfo"));
                                if (libro != null) resultados.add(libro);
                            }
                        }
                        callback.onSuccess(resultados);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parseando: " + e.getMessage());
                        callback.onError("Error al procesar los resultados");
                    }
                },
                error -> callback.onError(traducirError(error))
        ) {
            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("User-Agent", "ElLibroDelCuco-Android/1.0");
                return headers;
            }
        };

        // le pongo mas tiempo porque con redes lentas daba timeout
        request.setRetryPolicy(new DefaultRetryPolicy(12_000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
    }

    private String traducirError(VolleyError error) {
        if (error.networkResponse != null) {
            switch (error.networkResponse.statusCode) {
                case 400: return "Búsqueda no válida (400)";
                case 403: return "Límite de API alcanzado. Inténtalo más tarde.";
                case 429: return "Demasiadas solicitudes. Espera un momento.";
                case 500: return "Error del servidor de Google";
                default:  return "Error HTTP " + error.networkResponse.statusCode;
            }
        }
        if (error.getCause() != null) {
            String causa = error.getCause().getMessage();
            if (causa != null && causa.contains("Unable to resolve host")) return "Sin conexión a Internet";
            if (causa != null && causa.contains("timeout")) return "Tiempo de espera agotado.";
        }
        return "Error de conexión. Comprueba tu Internet.";
    }
}