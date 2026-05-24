package com.example.ellibrodelcuco.modelo;

import java.util.Date;

/**
 * Datos que se guardan en Firestore para representar una reseña usuario sobre un libro de la comunidad.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class Resena {

    private String uid;
    private String nombreUsuario;
    private String texto;
    private float nota;
    private Date fecha;


      //sConstructor vacío requerido obligatoriamente por Firestore.

    public Resena() {
    }

   // Constructor principal para inicializar una nueva reseña lista para publicarse.
    public Resena(String uid, String nombreUsuario, String texto, float nota, Date fecha) {
        this.uid = uid;
        this.nombreUsuario = nombreUsuario;
        this.texto = texto;
        this.nota = nota;
        this.fecha = fecha;
    }

    public String getUid() {
        return uid;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getTexto() {
        return texto;
    }

    public float getNota() {
        return nota;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public void setNota(float nota) {
        this.nota = nota;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }
}