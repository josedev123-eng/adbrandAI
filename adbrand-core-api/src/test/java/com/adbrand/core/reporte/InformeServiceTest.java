package com.adbrand.core.reporte;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.reporte.dto.CampanaInformeResponse;
import com.adbrand.core.reporte.dto.InformeCampanasResponse;
import com.adbrand.core.reporte.service.InformeService;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// HU 37: el informe consolidado con campañas de distintos estados, sin base de datos (Mockito).
class InformeServiceTest {

    private ContenidoRepository contenidos;
    private UsuarioRepository usuarios;
    private InformeService servicio;

    @BeforeEach
    void preparar() {
        contenidos = mock(ContenidoRepository.class);
        usuarios = mock(UsuarioRepository.class);
        servicio = new InformeService(contenidos, usuarios);

        Usuario usuario = new Usuario();
        usuario.setNombreEmpresa("Panadería Doña Rosa");
        when(usuarios.findById(1L)).thenReturn(Optional.of(usuario));
    }

    private static Contenido anuncio(String oferta, RedSocial red, EstadoContenido estado) {
        Contenido contenido = new Contenido();
        contenido.setUsuarioId(1L);
        contenido.setRedSocial(red);
        contenido.setOferta(oferta);
        contenido.setEstado(estado);
        return contenido;
    }

    @Test
    void cuentaCadaEstadoPorSeparado() {
        when(contenidos.findByUsuarioIdOrderByFechaCreacionDescIdDesc(1L)).thenReturn(List.of(
                anuncio("Promo 1", RedSocial.INSTAGRAM, EstadoContenido.APROBADO),
                anuncio("Promo 2", RedSocial.INSTAGRAM, EstadoContenido.APROBADO),
                anuncio("Promo 3", RedSocial.FACEBOOK, EstadoContenido.DUDOSO),
                anuncio("Promo 4", RedSocial.FACEBOOK, EstadoContenido.RECHAZADO),
                anuncio("Promo 5", RedSocial.INSTAGRAM, EstadoContenido.RECHAZADO),
                anuncio("Promo 6", RedSocial.FACEBOOK, EstadoContenido.RECHAZADO)));

        InformeCampanasResponse informe = servicio.campanas(1L);

        assertThat(informe.empresa()).isEqualTo("Panadería Doña Rosa");
        assertThat(informe.total()).isEqualTo(6);
        assertThat(informe.aprobados()).isEqualTo(2);
        assertThat(informe.dudosos()).isEqualTo(1);
        assertThat(informe.rechazados()).isEqualTo(3);
        assertThat(informe.aprobados() + informe.dudosos() + informe.rechazados()).isEqualTo(informe.total());
    }

    @Test
    void conservaElOrdenDelMasRecienteAlMasAntiguo() {
        when(contenidos.findByUsuarioIdOrderByFechaCreacionDescIdDesc(1L)).thenReturn(List.of(
                anuncio("La más reciente", RedSocial.INSTAGRAM, EstadoContenido.APROBADO),
                anuncio("La más antigua", RedSocial.FACEBOOK, EstadoContenido.APROBADO)));

        List<CampanaInformeResponse> campanas = servicio.campanas(1L).campanas();

        assertThat(campanas).extracting(CampanaInformeResponse::oferta)
                .containsExactly("La más reciente", "La más antigua");
    }

    @Test
    void conAnunciosDeUnaSolaRedLosTotalesSiguenCuadrando() {
        when(contenidos.findByUsuarioIdOrderByFechaCreacionDescIdDesc(1L)).thenReturn(List.of(
                anuncio("Promo 1", RedSocial.FACEBOOK, EstadoContenido.APROBADO),
                anuncio("Promo 2", RedSocial.FACEBOOK, EstadoContenido.DUDOSO)));

        InformeCampanasResponse informe = servicio.campanas(1L);

        assertThat(informe.campanas()).extracting(CampanaInformeResponse::redSocial).containsOnly(RedSocial.FACEBOOK);
        assertThat(informe.total()).isEqualTo(2);
        assertThat(informe.rechazados()).isZero();
    }

    @Test
    void sinCampanasTodosLosTotalesSonCero() {
        // Criterio 2: con esto la web avisa en lugar de descargar un archivo vacío.
        when(contenidos.findByUsuarioIdOrderByFechaCreacionDescIdDesc(1L)).thenReturn(List.of());

        InformeCampanasResponse informe = servicio.campanas(1L);

        assertThat(informe.campanas()).isEmpty();
        assertThat(informe.total()).isZero();
        assertThat(informe.aprobados()).isZero();
        assertThat(informe.dudosos()).isZero();
        assertThat(informe.rechazados()).isZero();
    }

    @Test
    void siLaCuentaNoExisteResponde404YNoBuscaAnuncios() {
        when(usuarios.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.campanas(99L))
                .isInstanceOf(RecursoNoEncontradoException.class)
                .hasMessage("La cuenta no existe.");
        verify(contenidos, never()).findByUsuarioIdOrderByFechaCreacionDescIdDesc(99L);
    }
}
