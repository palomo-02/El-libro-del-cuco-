package com.example.ellibrodelcuco.interfaz_usuario.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.ellibrodelcuco.R;
import com.google.android.material.button.MaterialButton;

import java.util.Arrays;
import java.util.List;

/**
 * Pantalla de introducción (Onboarding) de la aplicación.
 * Muestra un pequeño carrusel explicativo solo la primera vez que se instala la app.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class OnboardingActivity extends AppCompatActivity {

    // Claves para guardar en la memoria interna del teléfono si ya vimos el tutorial
    public static final String PREFS_NAME    = "onboarding_prefs";
    public static final String KEY_COMPLETADO = "onboarding_completado";

    private ViewPager2   vpOnboarding;
    private LinearLayout llIndicadores;
    private MaterialButton btnSiguiente;
    private TextView     tvSaltar;

    private List<SlideData> slides;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Comprobación de seguridad: Si ya hicimos el tutorial antes, saltamos directo al Login
        if (yaCompletado()) {
            irAlLogin();
            return;
        }

        setContentView(R.layout.activity_onboarding);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        inicializarSlides();
        bindViews();
        configurarViewPager();
        configurarIndicadores();
        configurarBotones();
    }

    // ── Datos de los slides ───────────────────────────────────────────────────

    private void inicializarSlides() {
        // Textos estáticos que verá el usuario en cada página del tutorial
        slides = Arrays.asList(
                new SlideData("📚", "Tu biblioteca personal", "Busca cualquier libro por título o escanea su ISBN con la cámara. Guárdalo en Pendiente, Leyendo, Leído o Quiero leer."),
                new SlideData("🔥", "Mantén tu racha lectora", "Cada día que registres actividad sumas un día a tu racha. ¡Desbloquea logros y conviértete en un auténtico Bibliófilo!"),
                new SlideData("📰", "Noticias y estadísticas", "Mantente al día con las últimas noticias literarias. Visualiza tus hábitos de lectura con gráficas de géneros y páginas leídas.")
        );
    }


    private void bindViews() {
        vpOnboarding  = findViewById(R.id.vpOnboarding);
        llIndicadores = findViewById(R.id.llIndicadores);
        btnSiguiente  = findViewById(R.id.btnSiguiente);
        tvSaltar      = findViewById(R.id.tvSaltar);
    }

    private void configurarViewPager() {
        vpOnboarding.setAdapter(new SlideAdapter(slides));
        vpOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                // Cada vez que cambiamos de página se actualiza
                actualizarIndicadores(position);
                actualizarBotones(position);
            }
        });
    }

    private void configurarIndicadores() {
        // el puntito para cada pagina del onboarding
        for (int i = 0; i < slides.size(); i++) {
            View punto = new View(this);
            int size = dpToPx(10);
            int margen = dpToPx(6);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margen, 0, margen, 0);
            punto.setLayoutParams(params);
            punto.setBackgroundResource(R.drawable.ic_book_placeholder);
            llIndicadores.addView(punto);
        }
        actualizarIndicadores(0);
    }

    private void actualizarIndicadores(int posicionActual) {
        // marcamso el puntito cuando se pasa al siguiente
        for (int i = 0; i < llIndicadores.getChildCount(); i++) {
            View punto = llIndicadores.getChildAt(i);
            punto.setAlpha(i == posicionActual ? 1.0f : 0.35f);
            punto.setBackgroundColor(i == posicionActual ? 0xFFE8A820 : 0xFF666666);
        }
    }

    private void actualizarBotones(int posicion) {
        // Si estamos en la última página, cambiamos el texto del botón y ocultamos lo de saltar
        boolean esUltimo = posicion == slides.size() - 1;
        btnSiguiente.setText(esUltimo ? "¡Empezar!" : "Siguiente");
        tvSaltar.setVisibility(esUltimo ? View.GONE : View.VISIBLE);
    }

    private void configurarBotones() {
        btnSiguiente.setOnClickListener(v -> {
            int actual = vpOnboarding.getCurrentItem();
            if (actual < slides.size() - 1) {
                vpOnboarding.setCurrentItem(actual + 1); // Avanza a la siguiente página
            } else {
                completarOnboarding(); // Termina el tutorial
            }
        });
        tvSaltar.setOnClickListener(v -> completarOnboarding());
    }


    private void completarOnboarding() {
        marcarCompletado();
        irAlLogin();
    }

    private void irAlLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish(); // se cierra para que no se pueda volver hacia atras
    }

    private boolean yaCompletado() {
        // Leemos la memoria del dispositivo para saber si existe la marca de tutorial completado
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_COMPLETADO, false);
    }

    private void marcarCompletado() {
        // Guardamos una "marca" permanente en el teléfono indicando que ya hicimos el tutorial
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_COMPLETADO, true)
                .apply();
    }


    private int dpToPx(int dp) {
        // Convierte píxeles independientes (dp) a píxeles reales de la pantalla actual
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }


    static class SlideData {
        final String emoji;
        final String titulo;
        final String descripcion;

        SlideData(String emoji, String titulo, String descripcion) {
            this.emoji = emoji;
            this.titulo = titulo;
            this.descripcion = descripcion;
        }
    }

    static class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.SlideViewHolder> {

        private final List<SlideData> slides;

        SlideAdapter(List<SlideData> slides) {
            this.slides = slides;
        }

        @NonNull
        @Override
        public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_slide, parent, false);
            return new SlideViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
            SlideData slide = slides.get(position);
            holder.tvEmoji.setText(slide.emoji);
            holder.tvTitulo.setText(slide.titulo);
            holder.tvDesc.setText(slide.descripcion);
        }

        @Override
        public int getItemCount() {
            return slides.size();
        }

        static class SlideViewHolder extends RecyclerView.ViewHolder {
            TextView tvEmoji, tvTitulo, tvDesc;

            SlideViewHolder(@NonNull View itemView) {
                super(itemView);
                tvEmoji  = itemView.findViewById(R.id.tvEmoji);
                tvTitulo = itemView.findViewById(R.id.tvTituloSlide);
                tvDesc   = itemView.findViewById(R.id.tvDescSlide);
            }
        }
    }
}