package com.example.ellibrodelcuco.interfaz_usuario.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.data.callback.SnapshotCallback;
import com.example.ellibrodelcuco.data.repository.AuthRepository;
import com.example.ellibrodelcuco.data.repository.BibliotecaRepository;
import com.example.ellibrodelcuco.data.repository.EstadisticasRepository;
import com.example.ellibrodelcuco.databinding.FragmentEstadisticasBinding;
import com.example.ellibrodelcuco.modelo.Libro;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragmento de estadísticas: Visualiza el progreso del usuario mediante gráficos
 * (barras y sectores) procesando los datos de su biblioteca en tiempo real.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class EstadisticasFragment extends Fragment {

    private FragmentEstadisticasBinding binding;
    private ListenerRegistration bibliotecaListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEstadisticasBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cargarEstadisticas(); // Inicia la escucha en tiempo real
        cargarMediaValoraciones(); // Consulta puntual para la nota media
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiamos el listener para evitar que siga consumiendo datos en segundo plano
        if (bibliotecaListener != null) {
            bibliotecaListener.remove();
            bibliotecaListener = null;
        }
        binding = null;
    }

    private void cargarEstadisticas() {
        FirebaseUser user = AuthRepository.getInstance().getCurrentUser();
        if (user == null) return;

        // Nos suscribimos a la biblioteca para que el gráfico se actualice solo si el usuario añade un libro
        bibliotecaListener = BibliotecaRepository.getInstance()
                .escucharBiblioteca(user.getUid(), new SnapshotCallback<Libro>() {
                    @Override
                    public void onChange(List<Libro> result) {
                        if (!isAdded() || binding == null) return;
                        actualizarUI(result); // Recalculamos los gráficos al recibir cambios
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (!isAdded() || binding == null) return;
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void cargarMediaValoraciones() {
        FirebaseUser user = AuthRepository.getInstance().getCurrentUser();
        if (user == null) return;

        // Consultamos la media usando agregaciones de Firestore (eficiente en costes)
        EstadisticasRepository.getInstance().obtenerMediaValoraciones(user.getUid(),
                new com.example.ellibrodelcuco.data.callback.Callback<Double>() {
                    @Override
                    public void onSuccess(Double media) {
                        if (!isAdded() || binding == null) return;
                        if (media != null && media > 0) {
                            binding.tvMediaValoraciones.setText(String.format("%.1f ⭐", media));
                        } else {
                            binding.tvMediaValoraciones.setText("—");
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (binding != null) binding.tvMediaValoraciones.setText("—");
                    }
                });
    }

    private void actualizarUI(List<Libro> libros) {
        if (binding == null) return;

        int leidos = 0;
        int totalPaginas = 0;
        Map<String, Integer> librosPorMes = new HashMap<>();
        Map<String, Integer> librosPorGenero = new HashMap<>();

        Calendar cal = Calendar.getInstance();
        String[] meses = { "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic" };

        // Procesamiento de datos: recorremos la lista para clasificar los libros
        for (Libro libro : libros) {
            if ("Leído".equals(libro.getEstado())) {
                leidos++;
                if (libro.getTotalPaginas() > 0) { // Sumamos páginas totales
                    totalPaginas += libro.getTotalPaginas();
                }

                // Agrupamos por mes para el gráfico de barras
                Date fechaLeido = libro.getFechaLeido();
                if (fechaLeido != null) { // Protección para evitar errores si la fecha es nula
                    cal.setTime(fechaLeido);
                    int mes = cal.get(Calendar.MONTH);
                    String mesKey = meses[mes];
                    librosPorMes.put(mesKey, librosPorMes.getOrDefault(mesKey, 0) + 1);
                }

                // Agrupamos por género para el gráfico de tarta
                String genero = libro.getGenero();
                if (genero != null && !genero.isEmpty()) {
                    librosPorGenero.put(genero, librosPorGenero.getOrDefault(genero, 0) + 1);
                }
            }
        }

        // Actualizamos los textos informativos
        binding.tvTotalLeidos.setText(String.valueOf(leidos));
        binding.tvTotalPaginas.setText(String.valueOf(totalPaginas));

        // Determinamos el género favorito comparando los valores del mapa
        String generoFav = librosPorGenero.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");
        binding.tvGeneroFavorito.setText(generoFav);

        // Disparamos la actualización de los gráficos
        actualizarBarChart(librosPorMes, meses);
        actualizarPieChart(librosPorGenero);
    }

    private void actualizarBarChart(Map<String, Integer> librosPorMes, String[] meses) {
        if (binding == null) return;
        List<BarEntry> entries = new ArrayList<>();

        // Transformamos el mapa de meses en "BarEntry" que es lo que entiende la librería
        for (int i = 0; i < meses.length; i++) {
            int val = librosPorMes.getOrDefault(meses[i], 0);
            entries.add(new BarEntry(i, val));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Libros leídos");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS); // Estilo visual predefinido
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        // Configuración de ejes y animaciones
        binding.barChart.setData(data);
        binding.barChart.getDescription().setEnabled(false);
        binding.barChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(meses));
        binding.barChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        binding.barChart.getAxisRight().setEnabled(false); // Ocultamos eje derecho para limpieza visual
        binding.barChart.animateY(500);
        binding.barChart.invalidate(); // Refrescamos el gráfico
    }

    private void actualizarPieChart(Map<String, Integer> librosPorGenero) {
        if (binding == null) return;
        List<PieEntry> entries = new ArrayList<>();

        // Transformamos el mapa de géneros en "PieEntry"
        for (Map.Entry<String, Integer> e : librosPorGenero.entrySet()) {
            entries.add(new PieEntry(e.getValue(), e.getKey()));
        }

        // Si no hay datos, mostramos un sector vacío para evitar que el gráfico esté "triste"
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, "Sin datos"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Géneros");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData data = new PieData(dataSet);

        // Configuración visual del donut chart
        binding.pieChart.setData(data);
        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleRadius(40f);
        binding.pieChart.setCenterText("Géneros");
        binding.pieChart.invalidate();
    }
}