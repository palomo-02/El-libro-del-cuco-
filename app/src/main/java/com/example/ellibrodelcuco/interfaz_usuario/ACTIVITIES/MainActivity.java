package com.example.ellibrodelcuco.interfaz_usuario.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.interfaz_usuario.fragments.BuscarLibrosFragment;
import com.example.ellibrodelcuco.interfaz_usuario.fragments.ComunidadFragment;
import com.example.ellibrodelcuco.interfaz_usuario.fragments.EstadisticasFragment;
import com.example.ellibrodelcuco.interfaz_usuario.fragments.NoticiasFragment;
import com.example.ellibrodelcuco.interfaz_usuario.fragments.PerfilFragment;
import com.example.ellibrodelcuco.workers.RachaWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.concurrent.TimeUnit;

import com.example.ellibrodelcuco.databinding.ActivityMainBinding;

/**
 * Activity principal de la aplicación. Actúa como contenedor del sistema de navegación y lleva servicios en segundo plano y seguridad en tiempo real.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity {

    private static final String WORK_NAME = "racha_diaria";
    private ActivityMainBinding binding;

    // Listener para expulsión en tiempo real
    private com.google.firebase.firestore.ListenerRegistration baneoListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Solicitud de permisos para notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        crearCanalNotificacion();
        programarWorkerRacha();

        // Carga del fragmento inicial al abrir la app
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new BuscarLibrosFragment())
                    .commit();
        }

        // intercambia los Fragments en el contenedor según la pestaña seleccionada
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_inicio) selectedFragment = new BuscarLibrosFragment();
            else if (id == R.id.nav_comunidad) selectedFragment = new ComunidadFragment();
            else if (id == R.id.nav_novedades) selectedFragment = new NoticiasFragment();
            else if (id == R.id.nav_estadisticas) selectedFragment = new EstadisticasFragment();
            else if (id == R.id.nav_perfil) selectedFragment = new PerfilFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        com.google.firebase.auth.FirebaseUser user = com.example.ellibrodelcuco.data.repository.AuthRepository.getInstance().getCurrentUser();
        if (user != null) {
            // Monitoriza los baneos
            baneoListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("usuarios").document(user.getUid())
                    .addSnapshotListener((doc, error) -> {
                        if (error != null) return;

                        if (doc != null && doc.exists()) {
                            String estado = doc.getString("estado");
                            if ("baneado".equals(estado)) {
                                // Se echa directamente si esta baneado
                                com.example.ellibrodelcuco.data.repository.AuthRepository.getInstance().signOut();
                                Toast.makeText(MainActivity.this, "⛔ Tu cuenta ha sido suspendida por un administrador.", Toast.LENGTH_LONG).show();

                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Optimización de recursos: apagamos el listener cuando la app pasa a segundo plano
        if (baneoListener != null) {
            baneoListener.remove();
            baneoListener = null;
        }
    }


    private void crearCanalNotificacion() {
        // Se crea el canal de notificaciones para que vaya en segundo plano
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    RachaWorker.CHANNEL_ID,
                    "Racha lectora",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Recordatorio diario para mantener tu racha lectora");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void programarWorkerRacha() {
        //  asegura la ejecución cada 24 horas aunque la app se cierre o el dispositivo se reinicie
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                RachaWorker.class, 24, TimeUnit.HOURS
        ).build();

        // La política KEEP evita que se creen múltiples tareas idénticas si el usuario abre la app repetidamente
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
        );
    }
}