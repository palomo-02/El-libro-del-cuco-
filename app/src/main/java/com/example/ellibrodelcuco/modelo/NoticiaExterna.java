package com.example.ellibrodelcuco.modelo;

/**
 * Modelo transitorio para estructurar los datos extraídos de la API REST
 * de noticias literarias. No se persiste en la base de datos.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class NoticiaExterna {

    private String titulo;
    private String descripcion;
    private String fuente;
    private String urlArticulo;
    private String urlImagen;
    private String fechaPublicacion;


//     Constructor vacío requerido obligatoriamente por Firestore.
    public NoticiaExterna() {
    }

  //Constructor principal para inicializar y mapear una noticia externa.
    public NoticiaExterna(String titulo, String descripcion, String fuente,
                          String urlArticulo, String urlImagen, String fechaPublicacion) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fuente = fuente;
        this.urlArticulo = urlArticulo;
        this.urlImagen = urlImagen;
        this.fechaPublicacion = fechaPublicacion;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getFuente() {
        return fuente;
    }

    public String getUrlArticulo() {
        return urlArticulo;
    }

    public String getUrlImagen() {
        return urlImagen;
    }

    public String getFechaPublicacion() {
        return fechaPublicacion;
    }
}