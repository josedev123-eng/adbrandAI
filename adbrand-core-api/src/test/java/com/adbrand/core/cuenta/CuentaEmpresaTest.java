package com.adbrand.core.cuenta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

// HU 23: datos de la empresa. @SpringBootTest carga FiltroJwt, así se prueba con tokens reales.
// Usa la base local; cada prueba se deshace al terminar (@Transactional).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CuentaEmpresaTest {

    private static final String URL = "/api/cuenta/empresa";
    private static final String DATOS_A = """
            {"ruc": "20611112222", "razonSocial": "Bodega Prueba A S.A.C.",
             "telefono": "987654321", "direccion": "Av. Lima 100, Surco"}""";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private ServicioJwt jwt;

    private Usuario empresaA;
    private Usuario empresaB;

    @BeforeEach
    void crearCuentas() {
        empresaA = usuarios.saveAndFlush(nuevaCuenta("empresa.a@adbrand.pe", "Bodega Prueba A"));
        empresaB = usuarios.saveAndFlush(nuevaCuenta("empresa.b@adbrand.pe", "Café Prueba B"));
    }

    private Usuario nuevaCuenta(String correo, String empresa) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Prueba");
        usuario.setNombreEmpresa(empresa);
        usuario.setCorreo(correo);
        usuario.setContrasenaHash("$2a$10$hashDePruebaQueNoSeUsaParaEntrar.............");
        return usuario;
    }

    private ResultActions guardar(Usuario usuario, String cuerpo) throws Exception {
        return mvc.perform(put(URL).header("Authorization", "Bearer " + jwt.generar(usuario))
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private ResultActions ver(Usuario usuario) throws Exception {
        return mvc.perform(get(URL).header("Authorization", "Bearer " + jwt.generar(usuario)));
    }

    @Test
    void conTokenSeVenLosDatosDeLaPropiaEmpresa() throws Exception {
        ver(empresaA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreEmpresa").value("Bodega Prueba A"))
                .andExpect(jsonPath("$.correo").value("empresa.a@adbrand.pe"))
                .andExpect(jsonPath("$.ruc").isEmpty());
    }

    @Test
    void conTokenSeGuardanLosDatosYAlVolverAEntrarSiguenAhi() throws Exception {
        guardar(empresaA, DATOS_A)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc").value("20611112222"))
                .andExpect(jsonPath("$.razonSocial").value("Bodega Prueba A S.A.C."));

        ver(empresaA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("987654321"))
                .andExpect(jsonPath("$.direccion").value("Av. Lima 100, Surco"));
    }

    @Test
    void elTokenDeLaEmpresaAGuardaEnAYLaEmpresaBQuedaIgual() throws Exception {
        guardar(empresaA, DATOS_A).andExpect(status().isOk());

        Usuario b = usuarios.findById(empresaB.getId()).orElseThrow();
        assertThat(b.getRuc()).isNull();
        assertThat(b.getRazonSocial()).isNull();
        ver(empresaB).andExpect(jsonPath("$.correo").value("empresa.b@adbrand.pe"))
                .andExpect(jsonPath("$.ruc").isEmpty());
    }

    @Test
    void rucQueYaTieneOtraCuentaDa409() throws Exception {
        guardar(empresaA, DATOS_A).andExpect(status().isOk());

        guardar(empresaB, DATOS_A.replace("Bodega Prueba A S.A.C.", "Café B S.R.L."))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("RUC_REPETIDO"))
                .andExpect(jsonPath("$.mensaje").value("Ese RUC ya está registrado en otra cuenta."));
    }

    @Test
    void guardarOtraVezElMismoRucDeLaPropiaCuentaNoEsRepetido() throws Exception {
        guardar(empresaA, DATOS_A).andExpect(status().isOk());

        guardar(empresaA, DATOS_A.replace("Av. Lima 100", "Av. Arequipa 200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccion").value("Av. Arequipa 200, Surco"));
    }

    @Test
    void rucMalEscritoSeRechaza() throws Exception {
        for (String ruc : new String[] {"2061111222", "30611112222", "2061111222A"}) {
            guardar(empresaA, DATOS_A.replace("20611112222", ruc))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.ruc").value("El RUC tiene 11 dígitos y empieza en 10 o 20."));
        }
    }

    @Test
    void telefonoMalEscritoSeRechazaYElFijoDeSieteDigitosSirve() throws Exception {
        for (String telefono : new String[] {"887654321", "98765432", "98765 432"}) {
            guardar(empresaA, DATOS_A.replace("987654321", telefono))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.telefono").exists());
        }

        guardar(empresaA, DATOS_A.replace("987654321", "4567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("4567890"));
    }

    @Test
    void telefonoVacioSeGuardaSinTelefono() throws Exception {
        guardar(empresaA, DATOS_A.replace("987654321", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").isEmpty());
    }

    @Test
    void razonSocialYDireccionSonObligatorias() throws Exception {
        guardar(empresaA, """
                {"ruc": "20611112222", "razonSocial": "  ", "telefono": "", "direccion": ""}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.razonSocial").value("Ingresa la razón social."))
                .andExpect(jsonPath("$.campos.direccion").value("Ingresa la dirección."));
    }

    @Test
    void sinTokenDa401() throws Exception {
        mvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));

        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content(DATOS_A))
                .andExpect(status().isUnauthorized());

        assertThat(usuarios.findById(empresaA.getId()).orElseThrow().getRuc()).isNull();
    }
}
