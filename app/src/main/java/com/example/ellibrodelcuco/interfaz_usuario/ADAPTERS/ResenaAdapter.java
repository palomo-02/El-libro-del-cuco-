package com.example.ellibrodelcuco.interfaz_usuario.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.modelo.Resena;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adaptador para gestionar la lista de reseñas de un libro en la interfaz de la comunidad.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class ResenaAdapter extends RecyclerView.Adapter<ResenaAdapter.ResenaViewHolder> {

    private List<Resena> listaResenas;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public ResenaAdapter(List<Resena> listaResenas) {
        this.listaResenas = listaResenas;
    }

    @NonNull
    @Override
    public ResenaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el diseño visual para cada fila de reseña
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_resena, parent, false);
        return new ResenaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ResenaViewHolder holder, int position) {
        Resena resena = listaResenas.get(position);

        // Ponemos el nombre del usuario (o "Anónimo" si falta)
        holder.tvNombreUsuario.setText(resena.getNombreUsuario() != null ? resena.getNombreUsuario() : "Anónimo");
        // Ajustamos las estrellas según la nota
        holder.rbNota.setRating(resena.getNota());
        // Mostramos el comentario
        holder.tvTexto.setText(resena.getTexto());

        // Formateamos la fecha para que sea legible
        if (resena.getFecha() != null) {
            holder.tvFecha.setText(sdf.format(resena.getFecha()));
        }
    }

    @Override
    public int getItemCount() {
        return listaResenas.size();
    }

    public static class ResenaViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombreUsuario, tvTexto, tvFecha;
        RatingBar rbNota;

        public ResenaViewHolder(@NonNull View itemView) {
            super(itemView);
            // Vincula las variables con los elementos visuales del XML
            tvNombreUsuario = itemView.findViewById(R.id.tvNombreUsuarioResena);
            rbNota = itemView.findViewById(R.id.rbNotaResena);
            tvTexto = itemView.findViewById(R.id.tvTextoResena);
            tvFecha = itemView.findViewById(R.id.tvFechaResena);
        }
    }
}