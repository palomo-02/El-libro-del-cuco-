package com.example.ellibrodelcuco.interfaz_usuario.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.modelo.NoticiaExterna;
import com.google.android.material.card.MaterialCardView;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Adaptador para mostrar la lista de noticias externas en un RecyclerView.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class NoticiasExternasAdapter extends RecyclerView.Adapter<NoticiasExternasAdapter.NoticiaViewHolder> {

    private List<NoticiaExterna> lista;

    public NoticiasExternasAdapter(List<NoticiaExterna> lista) {
        this.lista = lista;
    }

    @NonNull
    @Override
    public NoticiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Infla el layout de la tarjeta de noticia
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_noticia_externa, parent, false);
        return new NoticiaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NoticiaViewHolder holder, int position) {
        NoticiaExterna noticia = lista.get(position);

        // Asignación básica de textos
        holder.tvTitulo.setText(noticia.getTitulo() != null ? noticia.getTitulo() : "");
        holder.tvDescripcion.setText(noticia.getDescripcion() != null ? noticia.getDescripcion() : "");
        holder.tvFuente.setText(noticia.getFuente() != null ? noticia.getFuente() : "");
        holder.tvFecha.setText(noticia.getFechaPublicacion() != null ? noticia.getFechaPublicacion() : "");

        // Carga de imagen con Picasso; oculta el ImageView si no hay URL válida
        if (noticia.getUrlImagen() != null && !noticia.getUrlImagen().isEmpty()) {
            holder.ivImagen.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(noticia.getUrlImagen())
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_placeholder)
                    .into(holder.ivImagen);
        } else {
            holder.ivImagen.setVisibility(View.GONE);
        }

        // Abre la noticia en el navegador externo al hacer clic
        holder.card.setOnClickListener(v -> {
            if (noticia.getUrlArticulo() != null && !noticia.getUrlArticulo().isEmpty()) {
                Context ctx = v.getContext();
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(noticia.getUrlArticulo()));
                ctx.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    public static class NoticiaViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView ivImagen;
        TextView tvTitulo, tvDescripcion, tvFuente, tvFecha;

        public NoticiaViewHolder(@NonNull View itemView) {
            super(itemView);
            // Vincula las vistas del layout
            card = itemView.findViewById(R.id.cardNoticia);
            ivImagen = itemView.findViewById(R.id.ivImagenNoticia);
            tvTitulo = itemView.findViewById(R.id.tvTituloNoticia);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcionNoticia);
            tvFuente = itemView.findViewById(R.id.tvFuenteNoticia);
            tvFecha = itemView.findViewById(R.id.tvFechaNoticia);
        }
    }
}