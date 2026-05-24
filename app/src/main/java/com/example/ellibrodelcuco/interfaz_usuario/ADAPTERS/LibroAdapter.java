package com.example.ellibrodelcuco.interfaz_usuario.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.modelo.Libro;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador para mostrar la lista de libros en el RecyclerView.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class LibroAdapter extends RecyclerView.Adapter<LibroAdapter.LibroViewHolder> {

    private List<Libro> listaLibros;
    private List<String> titulosGuardados = new ArrayList<>();
    private OnLibroClickListener listener;
    private OnLibroLongClickListener longListener;

    public interface OnLibroClickListener {
        void onLibroClick(Libro libro);
    }

    public interface OnLibroLongClickListener {
        void onLibroLongClick(Libro libro);
    }

    public void setOnLibroLongClickListener(OnLibroLongClickListener listener) {
        this.longListener = listener;
    }

    public LibroAdapter(List<Libro> listaLibros, OnLibroClickListener listener) {
        this.listaLibros = listaLibros;
        this.listener = listener;
    }

    public void setTitulosGuardados(List<String> titulos) {
        this.titulosGuardados = titulos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LibroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Crea la vista (layout) para cada elemento de la lista
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_libro, parent, false);
        return new LibroViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LibroViewHolder holder, int position) {
        Libro libro = listaLibros.get(position);

        // Asignamos datos básicos
        holder.tvTitulo.setText(libro.getTitulo());
        holder.tvAutor.setText(libro.getAutor());

        // Carga la portada con Picasso (segura con https) o pone icono por defecto
        if (libro.getPortadaUrl() != null && !libro.getPortadaUrl().isEmpty()) {
            String urlSegura = libro.getPortadaUrl().replace("http:", "https:");
            Picasso.get().load(urlSegura).fit().centerCrop().into(holder.imgPortada);
        } else {
            holder.imgPortada.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        // Si ya está guardado, ponemos el check verde. Si no, ponemos su estado normal
        if (titulosGuardados.contains(libro.getTitulo())) {
            holder.tvEstado.setVisibility(View.VISIBLE);
            holder.tvEstado.setText("✓ En tu biblioteca");
            holder.tvEstado.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
        } else {
            holder.tvEstado.setTextColor(android.graphics.Color.parseColor("#888888"));
            if (libro.getEstado() != null && !libro.getEstado().isEmpty()) {
                holder.tvEstado.setVisibility(View.VISIBLE);
                holder.tvEstado.setText(libro.getEstado());
            } else {
                holder.tvEstado.setVisibility(View.GONE);
            }
        }

        // Ocultamos botón de guardar si no procede
        View btnGuardar = holder.itemView.findViewById(R.id.btnGuardar);
        if (btnGuardar != null) {
            btnGuardar.setVisibility(View.GONE);
        }

        // Calculamos el porcentaje de páginas leídas y actualizamos la barra
        if (holder.pbMiniProgreso != null) {
            if (libro.getTotalPaginas() > 0) {
                holder.pbMiniProgreso.setVisibility(View.VISIBLE);
                int progreso = libro.getPaginaActual() * 100 / libro.getTotalPaginas();
                holder.pbMiniProgreso.setProgress(progreso);
            } else {
                holder.pbMiniProgreso.setVisibility(View.GONE);
            }
        }

        // Detectamos clics normales o largos
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onLibroClick(libro);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longListener != null) {
                longListener.onLibroLongClick(libro);
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return listaLibros.size();
    }

    public static class LibroViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvAutor, tvEstado;
        ImageView imgPortada;
        ProgressBar pbMiniProgreso;

        public LibroViewHolder(@NonNull View itemView) {
            super(itemView);
            // Buscamos los elementos visuales en el layout item_libro
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvAutor = itemView.findViewById(R.id.tvAutor);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            imgPortada = itemView.findViewById(R.id.imgPortada);
            pbMiniProgreso = itemView.findViewById(R.id.pbMiniProgreso);
        }
    }
}