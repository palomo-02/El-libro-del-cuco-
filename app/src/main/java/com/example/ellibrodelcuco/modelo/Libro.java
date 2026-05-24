package com.example.ellibrodelcuco.modelo;
/**
 * Representa la entidad principal de un  libro, almacenando
 * datos y el estado de progreso del usuario.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */

import java.io.Serializable;
import java.util.Date;


public class Libro implements Serializable {

    private String titulo;
    private String autor;
    private String sinopsis;
    private String portadaUrl;
    private String estado;
    private String genero;
    private int paginaActual;
    private int totalPaginas;
    private Date fechaLeido;

  //Constructor vacío requerido obligatoriamente por Firestore.
    public Libro() {
    }

 //Constructor principal para inicializar un nuevo libro en la biblioteca
    public Libro(String titulo, String autor, String sinopsis, String portadaUrl, String estado) {
        this.titulo = titulo;
        this.autor = autor;
        this.sinopsis = sinopsis;
        this.portadaUrl = portadaUrl;
        this.estado = estado;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutor() {
        return autor;
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public String getPortadaUrl() {
        return portadaUrl;
    }

    public String getEstado() {
        return estado;
    }

    public String getGenero() {
        return genero;
    }

    public int getPaginaActual() {
        return paginaActual;
    }

    public int getTotalPaginas() {
        return totalPaginas;
    }

    public Date getFechaLeido() {
        return fechaLeido;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public void setSinopsis(String sinopsis) {
        this.sinopsis = sinopsis;
    }

    public void setPortadaUrl(String portadaUrl) {
        this.portadaUrl = portadaUrl;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public void setPaginaActual(int paginaActual) {
        this.paginaActual = paginaActual;
    }

    public void setTotalPaginas(int totalPaginas) {
        this.totalPaginas = totalPaginas;
    }

    public void setFechaLeido(Date fechaLeido) {
        this.fechaLeido = fechaLeido;
    }
}
