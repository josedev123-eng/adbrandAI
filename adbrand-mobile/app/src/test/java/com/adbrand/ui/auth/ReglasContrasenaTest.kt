package com.adbrand.ui.auth

import com.adbrand.ui.auth.viewmodel.reglasDeContrasena
import org.junit.Assert.assertEquals
import org.junit.Test

// Registro: las reglas de la contraseña se ven desde el inicio y se marcan mientras se escribe.
class ReglasContrasenaTest {

    private fun cumplidas(clave: String) = reglasDeContrasena(clave).filter { it.second }.map { it.first }

    @Test
    fun alInicioNingunaReglaSeCumple() {
        assertEquals(emptyList<String>(), cumplidas(""))
    }

    @Test
    fun seMarcanLasReglasQueYaCumple() {
        assertEquals(listOf("Una letra mayúscula", "Una letra minúscula", "Sin espacios"), cumplidas("Clave"))
        assertEquals(5, cumplidas("Clave1234").size)
        assertEquals(false, reglasDeContrasena("Clave 1234").first { it.first == "Sin espacios" }.second)
    }
}
