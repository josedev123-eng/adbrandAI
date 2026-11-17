package com.adbrand.core.previsualizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.adbrand.core.brandkit.entity.KitMarca;
import com.adbrand.core.brandkit.repository.KitMarcaRepository;
import com.adbrand.core.contenido.dto.RedSocial;
import com.adbrand.core.contenido.entity.Contenido;
import com.adbrand.core.contenido.entity.EstadoContenido;
import com.adbrand.core.contenido.entity.OpcionAnuncio;
import com.adbrand.core.contenido.entity.VersionAnuncio;
import com.adbrand.core.contenido.repository.ContenidoRepository;
import com.adbrand.core.contenido.repository.OpcionAnuncioRepository;
import com.adbrand.core.contenido.repository.VersionAnuncioRepository;
import com.adbrand.core.imagen.dto.ImagenGenerada;
import com.adbrand.core.previsualizacion.dto.FormatoAnuncio;
import com.adbrand.core.previsualizacion.dto.ImagenAnuncioResponse;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private OpcionAnuncioRepository opciones;
    private VersionAnuncioRepository versiones;
    private PrevisualizacionService servicio;

    @BeforeEach
    void preparar() {
        kits = mock(KitMarcaRepository.class);
        contenidos = mock(ContenidoRepository.class);
        opciones = mock(OpcionAnuncioRepository.class);
        versiones = mock(VersionAnuncioRepository.class);
        servicio = new PrevisualizacionService(kits, contenidos, opciones, versiones);
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

    // HU 46: imagen del anuncio para la vista previa como publicación.
    private Contenido propio(Long id, Long usuarioId) {
        Contenido contenido = anuncio("2x1 en café");
        contenido.setUsuarioId(usuarioId);
        when(contenidos.findById(id)).thenReturn(Optional.of(contenido));
        return contenido;
    }

    @Test
    void laImagenEsLaDeLaOpcionElegida() {
        propio(9L, 1L);
        OpcionAnuncio opcion = mock(OpcionAnuncio.class);
        when(opcion.imagen()).thenReturn(new ImagenGenerada("image/jpeg", "QUJD", "cloudflare", false));
        when(opciones.findFirstByContenidoIdAndElegidaTrue(9L)).thenReturn(Optional.of(opcion));

        ImagenAnuncioResponse respuesta = servicio.imagen(1L, 9L);

        assertThat(respuesta.imagen()).isEqualTo("data:image/jpeg;base64,QUJD");
    }

    @Test
    void siSeRetocoUsaLaVersionActual() {
        propio(9L, 1L);
        OpcionAnuncio opcion = mock(OpcionAnuncio.class);
        when(opcion.getId()).thenReturn(5L);
        when(opcion.getVersionActual()).thenReturn(2);
        when(opcion.imagen()).thenReturn(new ImagenGenerada("image/jpeg", "VIEJA", "cloudflare", false));
        VersionAnuncio version = mock(VersionAnuncio.class);
        when(version.imagen()).thenReturn(new ImagenGenerada("image/png", "NUEVA", "openrouter", false));
        when(opciones.findFirstByContenidoIdAndElegidaTrue(9L)).thenReturn(Optional.of(opcion));
        when(versiones.findByOpcionIdAndNumero(5L, 2)).thenReturn(Optional.of(version));

        assertThat(servicio.imagen(1L, 9L).imagen()).isEqualTo("data:image/png;base64,NUEVA");
    }

    @Test
    void unAnuncioSinImagenDevuelveNull() {
        propio(9L, 1L);
        when(opciones.findFirstByContenidoIdAndElegidaTrue(9L)).thenReturn(Optional.empty());

        assertThat(servicio.imagen(1L, 9L).imagen()).isNull();
    }

    @Test
    void elAnuncioDeOtraEmpresaNoSeMuestra() {
        propio(9L, 2L);

        assertThatThrownBy(() -> servicio.imagen(1L, 9L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    // HU 46: el formato elegido viaja en la respuesta y el texto largo se acorta con "…".
    @Test
    void enFormatoVerticalElTextoLargoSeAcortaYSeMarcaComoRecortado() {
        String largo = "Este viernes el pan de yema viene doble: trae a quien quieras y llévate dos panes por el precio "
                + "de uno, recién salidos del horno.";
        when(kits.findFirstByUsuarioIdOrderByFechaActualizacionDescIdDesc(1L)).thenReturn(Optional.empty());
        when(contenidos.findByUsuarioIdOrderByFechaCreacionDescIdDesc(1L)).thenReturn(List.of(anuncio(largo), anuncio("2x1 hoy")));

        PrevisualizacionResponse respuesta = servicio.obtener(1L, FormatoAnuncio.VERTICAL);

        assertThat(respuesta.formato().codigo()).isEqualTo("VERTICAL");
        assertThat(respuesta.formato().uso()).isEqualTo("Historia");
        assertThat(respuesta.anuncios().get(0).recortado()).isTrue();
        assertThat(respuesta.anuncios().get(0).textoAjustado()).endsWith("…");
        assertThat(respuesta.anuncios().get(0).texto()).isEqualTo(largo);
        assertThat(respuesta.anuncios().get(1).recortado()).isFalse();
        assertThat(respuesta.anuncios().get(1).textoAjustado()).isEqualTo("2x1 hoy");
    }
}
