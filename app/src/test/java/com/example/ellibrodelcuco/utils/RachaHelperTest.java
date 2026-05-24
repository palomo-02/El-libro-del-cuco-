package com.example.ellibrodelcuco.utils;

import org.junit.Test;
import java.util.Calendar;
import java.util.Date;
import static org.junit.Assert.assertEquals;

public class RachaHelperTest {

    private Date haceDias(int dias) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -dias);
        return cal.getTime();
    }

    @Test
    public void racha_siHoyYaSeContabilizo_noSube() {
        assertEquals(5L, RachaHelper.calcularNuevaRacha(haceDias(0), 5L));
    }

    @Test
    public void racha_actividadAyer_subeEn1() {
        assertEquals(4L, RachaHelper.calcularNuevaRacha(haceDias(1), 3L));
    }

    @Test
    public void racha_sinActividad2dias_vuelveA1() {
        assertEquals(1L, RachaHelper.calcularNuevaRacha(haceDias(2), 7L));
    }

    @Test
    public void racha_primeraVez_empieza1() {
        // Si no hay actividad previa arranca desde 1
        assertEquals(1L, RachaHelper.calcularNuevaRacha(null, 0L));
    }

    @Test
    public void racha_hace1semana_seresetea() {
        assertEquals(1L, RachaHelper.calcularNuevaRacha(haceDias(7), 14L));
    }
}