package com.adbrand.data

import com.adbrand.core.network.Resultado
import com.adbrand.data.repository.AccesoCuentaRepository
import com.adbrand.data.repository.AccesoCuentaRepositoryFirebase
import com.adbrand.data.repository.AuthRepository
import com.adbrand.data.repository.AuthRepositoryFirebase
import com.adbrand.data.repository.ProveedorFirebase
import com.adbrand.data.repository.ResultadoFirebase
import com.adbrand.data.repository.ServidorFirebase
import com.adbrand.domain.model.Usuario
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Inicio de sesión y registro del móvil con Firebase Authentication, con Firebase y Spring simulados.
class FirebaseAuthRepositoryTest {

    private val rosa = Usuario(id = 1L, nombre = "Rosa", nombreEmpresa = "Panadería Rosa", correo = "rosa@hotmail.com")

    private class FirebaseFalso(
        var disponibleEn: Boolean = true,
        var alEntrar: ResultadoFirebase = ResultadoFirebase.Ok,
        var alCrear: ResultadoFirebase = ResultadoFirebase.Ok,
        var verificado: Boolean = true
    ) : ProveedorFirebase {
        val acciones = mutableListOf<String>()
        override fun disponible() = disponibleEn
        override suspend fun entrar(correo: String, contrasena: String) = alEntrar.also { acciones += "entrar" }
        override suspend fun crearCuenta(correo: String, contrasena: String) = alCrear.also { acciones += "crear" }
        override suspend fun correoVerificado() = verificado
        override suspend fun idToken(): String = "token-firebase"
        override suspend fun enviarVerificacion() { acciones += "verificacion" }
        override suspend fun eliminarActual() { acciones += "eliminar" }
        override fun salir() { acciones += "salir" }
    }

    private class SpringFalso(var alEntrar: Resultado<Usuario>) : AuthRepository {
        var entradas = 0
        override suspend fun iniciarSesion(correo: String, contrasena: String): Resultado<Usuario> = alEntrar.also { entradas++ }
        override suspend fun cerrarSesion(): Resultado<Unit> = Resultado.Exito(Unit)
    }

    private class ServidorFalso(
        var alEntrar: Resultado<Usuario>,
        var alRegistrar: Resultado<String> = Resultado.Exito("ok")
    ) : ServidorFirebase {
        val vinculados = mutableListOf<String>()
        var registros = 0
        override suspend fun iniciarSesion(idToken: String) = alEntrar
        override suspend fun registrar(idToken: String, nombre: String, empresa: String) = alRegistrar.also { registros++ }
        override suspend fun vincular(idToken: String): Resultado<Unit> { vinculados += idToken; return Resultado.Exito(Unit) }
    }

    private val incorrectos = Resultado.Error("Correo o contraseña incorrectos.", 401)

    @Test
    fun conFirebaseYCorreoVerificadoEntraConLaSesionDeSpring() = runTest {
        val servidor = ServidorFalso(Resultado.Exito(rosa))
        val spring = SpringFalso(incorrectos)
        val repo = AuthRepositoryFirebase(FirebaseFalso(), spring, servidor)

        val resultado = repo.iniciarSesion(" rosa@hotmail.com ", "Clave2026")

        assertEquals(Resultado.Exito(rosa), resultado)
        assertEquals(0, spring.entradas)
    }

    @Test
    fun unaCuentaDeLaWebSeMigraSolaLaPrimeraVez() = runTest {
        val firebase = FirebaseFalso(alEntrar = ResultadoFirebase.CredencialesInvalidas)
        val servidor = ServidorFalso(incorrectos)
        val repo = AuthRepositoryFirebase(firebase, SpringFalso(Resultado.Exito(rosa)), servidor)

        val resultado = repo.iniciarSesion("rosa@hotmail.com", "Clave2026")

        assertEquals(Resultado.Exito(rosa), resultado)
        assertTrue("crea su acceso en Firebase", "crear" in firebase.acciones)
        assertEquals(listOf("token-firebase"), servidor.vinculados)
        assertTrue("manda el correo de verificación", "verificacion" in firebase.acciones)
    }

    @Test
    fun conLaContrasenaMalNoEntraNiCreaNada() = runTest {
        val firebase = FirebaseFalso(alEntrar = ResultadoFirebase.CredencialesInvalidas)
        val repo = AuthRepositoryFirebase(firebase, SpringFalso(incorrectos), ServidorFalso(incorrectos))

        val resultado = repo.iniciarSesion("rosa@hotmail.com", "Mala1234")

        assertEquals(incorrectos, resultado)
        assertTrue("crear" !in firebase.acciones)
    }

    @Test
    fun unaCuentaNuevaSinConfirmarElCorreoNoEntraYSeLeReenviaElEnlace() = runTest {
        val firebase = FirebaseFalso(verificado = false)
        val repo = AuthRepositoryFirebase(firebase, SpringFalso(incorrectos), ServidorFalso(Resultado.Exito(rosa)))

        val resultado = repo.iniciarSesion("luis@outlook.com", "Clave2026")

        assertTrue(resultado is Resultado.Error && resultado.mensaje.contains("Confirma tu correo"))
        assertTrue("verificacion" in firebase.acciones)
        assertTrue("salir" in firebase.acciones)
    }

    @Test
    fun sinFirebaseEnElTelefonoUsaElLoginDeSiempre() = runTest {
        val spring = SpringFalso(Resultado.Exito(rosa))
        val firebase = FirebaseFalso(disponibleEn = false)
        val repo = AuthRepositoryFirebase(firebase, spring, ServidorFalso(incorrectos))

        assertEquals(Resultado.Exito(rosa), repo.iniciarSesion("rosa@hotmail.com", "Clave2026"))
        assertEquals(1, spring.entradas)
        assertTrue(firebase.acciones.isEmpty())
    }

    private class AccesoFalso : AccesoCuentaRepository {
        var registros = 0
        override suspend fun registrar(nombre: String, empresa: String, correo: String, contrasena: String): Resultado<String> {
            registros++
            return Resultado.Exito("registro de siempre")
        }
        override suspend fun solicitarCodigo(correo: String): Resultado<String> = Resultado.Exito("codigo")
        override suspend fun cambiarContrasena(correo: String, codigo: String, contrasena: String): Resultado<String> =
            Resultado.Exito("cambiada")
    }

    @Test
    fun elRegistroCreaElAccesoEnFirebaseLaEmpresaEnSpringYMandaLaVerificacion() = runTest {
        val firebase = FirebaseFalso()
        val servidor = ServidorFalso(incorrectos)
        val repo = AccesoCuentaRepositoryFirebase(firebase, servidor, AccesoFalso())

        val resultado = repo.registrar("Luis", "Ferretería Luis", "luis@outlook.com", "Clave2026")

        assertTrue(resultado is Resultado.Exito && (resultado as Resultado.Exito).dato.contains("luis@outlook.com"))
        assertEquals(1, servidor.registros)
        assertEquals(listOf("crear", "verificacion", "salir"), firebase.acciones)
    }

    @Test
    fun siElCorreoYaEsDeUnaCuentaDeLaWebNoDejaElAccesoAMedias() = runTest {
        val firebase = FirebaseFalso()
        val servidor = ServidorFalso(incorrectos, alRegistrar = Resultado.Error("Ya existe", 409))
        val repo = AccesoCuentaRepositoryFirebase(firebase, servidor, AccesoFalso())

        val resultado = repo.registrar("Rosa", "Panadería", "rosa@hotmail.com", "Clave2026")

        assertTrue(resultado is Resultado.Error && (resultado as Resultado.Error).codigo == 409)
        assertTrue("borra el acceso recién creado", "eliminar" in firebase.acciones)
    }

    @Test
    fun recuperarLaContrasenaSigueConElCodigoDeSpring() = runTest {
        val repo = AccesoCuentaRepositoryFirebase(FirebaseFalso(), ServidorFalso(incorrectos), AccesoFalso())

        assertEquals(Resultado.Exito("codigo"), repo.solicitarCodigo("rosa@hotmail.com"))
    }
}
