package com.adbrand.core.previsualizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.negocio.entity.Tono;
import com.adbrand.core.previsualizacion.dto.PrevisualizacionResponse;
import com.adbrand.core.previsualizacion.service.PrevisualizacionService;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// HU 45: previsualización con kit y sin kit, sin base de datos.
class PrevisualizacionServiceTest {

    private KitMarcaRepository kits;
    private ContenidoRepository contenidos;
    private PrevisualizacionService servicio;

    @BeforeEach
    void preparar() {
        kits = mock(KitMarcaRepository.class);
        contenidos = mock(ContenidoRepository.class);
        servicio = new PrevisualizacionService(kits, contenidos);
    }

    private Contenido anuncio(String texto) {
        Contenido contenido = new Contenido();
        contenido.setTexto(texto);
        contenido.setRedSocial(RedSocial.INSTAGRAM);
        contenido.setTono(Tono.CERCANO);
        contenido.setEstado(EstadoContenido.APROBADO);
        return contenido;
    }

    private KitMarca kit() {
        KitMarca kit = new KitMarca();
        kit.setUsuarioId(1L);
        kit.setNombreNegocio("Bodega San Martín");
        kit.setSector("Comercio");
        kit.setEstiloVisual("Cálido");
        kit.setLogoConcepto("Una bolsa de compras");
        kit.setTipografiaPrimaria("Poppins");
        kit.setPaletaColores("#4F46E5,#F97316");
        return kit;
    }

    @Test
    void sinKitDevuelveNullYLosAnunciosIgualSeListan() {
        when(kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(1L)).thenReturn(Optional.empty());
        when(contenidos.findByUsuarioIdOrderByFechaCreacionDescIdDesc(1L))
                .thenReturn(List.of(anuncio("Hoy hay ofertas")));

        PrevisualizacionResponse respuesta = servicio.obtener(1L);

        assertThat(respuesta.kit()).isNull();
        assertThat(respuesta.anuncios()).hasSize(1);
        assertThat(respuesta.anuncios().get(0).texto()).isEqualTo("Hoy hay ofertas");
    }

    @Test
    void conKitDevuelveElKitMasRecienteYLosAnuncios() {
        when(kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(1L)).thenReturn(Optional.of(kit()));
        when(contenidos.findByUsuarioIdOrderByFechaCreacionDescIdDesc(1L))
                .thenReturn(List.of(anuncio("Primero"), anuncio("Segundo")));

        PrevisualizacionResponse respuesta = servicio.obtener(1L);

        assertThat(respuesta.kit()).isNotNull();
        assertThat(respuesta.kit().nombreNegocio()).isEqualTo("Bodega San Martín");
        assertThat(respuesta.kit().paletaColores()).isEqualTo("#4F46E5,#F97316");
        assertThat(respuesta.anuncios()).hasSize(2);
        assertThat(respuesta.anuncios().get(1).texto()).isEqualTo("Segundo");
    }

    @Test
    void comoMaximoDevuelveVeinteAnuncios() {
        when(kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(1L)).thenReturn(Optional.empty());
        when(contenidos.findByUsuarioIdOrderByFechaCreacionDescIdDesc(1L))
                .thenReturn(IntStream.range(0, 25).mapToObj(i -> anuncio("Anuncio " + i)).toList());

        PrevisualizacionResponse respuesta = servicio.obtener(1L);

        assertThat(respuesta.anuncios()).hasSize(20);
    }
}
