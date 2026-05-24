package com.example.ellibrodelcuco.utils;

import com.example.ellibrodelcuco.modelo.Libro;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Utilidad estática para transformar el JSON de Google Books API en un modelo Libro.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class GoogleBooksParser {


    public static Libro parse(JSONObject volumeInfo) {
        try {
            // Extracción segura de datos con valores por defecto
            String titulo = volumeInfo.optString("title", "Sin título");

            JSONArray autoresArray = volumeInfo.optJSONArray("authors");
            String autor = (autoresArray != null) ? autoresArray.getString(0) : "Autor desconocido";

            String sinopsisRaw = volumeInfo.optString("description", null);
            String sinopsis = (sinopsisRaw != null && !sinopsisRaw.isEmpty()) ? sinopsisRaw : "Sin sinopsis disponible.";

            // Si no se pone htpps pueden darse bloqueos
            JSONObject imageLinks = volumeInfo.optJSONObject("imageLinks");
            String portadaRaw = (imageLinks != null) ? imageLinks.optString("thumbnail", null) : null;
            String portada = (portadaRaw != null && !portadaRaw.isEmpty()) ? portadaRaw.replace("http:", "https:") : "";

            int pageCount = volumeInfo.optInt("pageCount", 0);

            JSONArray categoriesArray = volumeInfo.optJSONArray("categories");
            String genero = (categoriesArray != null && categoriesArray.length() > 0)
                    ? categoriesArray.optString(0, "Sin clasificar") : "Sin clasificar";

            // Construcción del objeto modelo
            Libro libro = new Libro(titulo, autor, sinopsis, portada, "Pendiente");
            libro.setTotalPaginas(pageCount);
            libro.setGenero(genero);

            return libro;
        } catch (Exception e) {
            // devuelve null si el JSON está corrupto o falta info esnecial
            return null;
        }
    }
}