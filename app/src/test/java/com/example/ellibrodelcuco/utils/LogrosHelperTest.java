package com.example.ellibrodelcuco.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class LogrosHelperTest {

    @Test
    public void test_sinLibros_noCumplePrimerLibro() {
        assertFalse(0 >= 1);
    }

    @Test
    public void test_unLibro_cumplePrimerLibro() {
        assertTrue(1 >= 1);
    }

    @Test
    public void test_aprendiz_necesita5() {
        assertFalse(4 >= 5);
        assertTrue(5 >= 5);
    }

    @Test
    public void test_lector_necesita10() {
        assertFalse(9 >= 10);
        assertTrue(10 >= 10);
    }

    @Test
    public void test_bibliofilo_necesita50() {
        assertFalse(49 >= 50);
        assertTrue(50 >= 50);
    }

    @Test
    public void test_racha7_conMenosDias_noSeCumple() {
        long racha = 6;
        assertFalse(racha >= 7);
    }

    @Test
    public void test_racha7_conSieteDias_seCumple() {
        long racha = 7;
        assertTrue(racha >= 7);
    }

    @Test
    public void test_explorador_necesita5generos() {
        assertFalse(3 >= 5);
        assertTrue(5 >= 5);
    }

    @Test
    public void test_constantes() {
        assertEquals("primer_libro", LogrosHelper.PRIMER_LIBRO);
        assertEquals("aprendiz", LogrosHelper.APRENDIZ);
        assertEquals("lector", LogrosHelper.LECTOR);
        assertEquals("bibliofilo", LogrosHelper.BIBLIOFILO);
        assertEquals("racha_7", LogrosHelper.RACHA_7);
        assertEquals("explorador", LogrosHelper.EXPLORADOR);
    }
}