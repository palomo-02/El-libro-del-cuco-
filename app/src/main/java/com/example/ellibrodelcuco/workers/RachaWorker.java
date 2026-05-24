package com.example.ellibrodelcuco.workers;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.ellibrodelcuco.R;
import com.example.ellibrodelcuco.utils.RachaHelper;

import java.util.Calendar;

/**
 * Worker de segundo plano encargado de la retención de usuarios.
 * Analiza la actividad local y dispara notificaciones locales si el usuario
 * no ha registrado actividad lectora en las últimas 24 horas.
 *
 * @author José Manuel Palomo Zambrano
 * @version 1.0
 */
public class RachaWorker extends Worker {

    public static final String CHANNEL_ID = "racha_channel";


      //Constructor del Worker requerido por el framework WorkManager.

    public RachaWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

 //Comparacion para la racha
    @NonNull
    @Override
    public Result doWork() {
        Context ctx = getApplicationContext();
        SharedPreferences prefs = ctx.getSharedPreferences(RachaHelper.PREFS_NAME, Context.MODE_PRIVATE);

        long ultimaActividadMs = prefs.getLong(RachaHelper.KEY_ULTIMA_ACTIVIDAD, 0L);
        long rachaActual = prefs.getLong(RachaHelper.KEY_RACHA_ACTUAL, 0L);

        // Si nunca hubo actividad, el worker finaliza sin notificar
        if (ultimaActividadMs == 0L) return Result.success();

        // Normalización de fechas a medianoche para cálculo preciso de diferencia en días
        Calendar hoy = getNormalisedCalendar(Calendar.getInstance());
        Calendar ultimaCal = getNormalisedCalendar(Calendar.getInstance());
        ultimaCal.setTimeInMillis(ultimaActividadMs);

        long diffDias = (hoy.getTimeInMillis() - ultimaCal.getTimeInMillis()) / (1000L * 60 * 60 * 24);

        // Si ha pasado al menos un día desde la última lectura, se lanza la notificación
        if (diffDias >= 1) {
            String texto = rachaActual > 0
                    ? "No rompas tu racha de " + rachaActual + " días 🔥"
                    : "¡Empieza tu racha lectora hoy!";

            NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_menu_info_details)
                    .setContentTitle("¿Has leído hoy? 📖")
                    .setContentText(texto)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(1001, builder.build());
            }
        }

        return Result.success();
    }

  //metodo de reseteo de horas
    private Calendar getNormalisedCalendar(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }
}