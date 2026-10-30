package com.adbrand.core.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.adbrand.core.auth.entity.EstadoUsuario;
import com.adbrand.core.auth.entity.TokenRenovacion;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.TokenRenovacionRepository;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.ServicioJwt;
import com.adbrand.core.auth.service.TokenRenovacionService;
import com.adbrand.core.shared.error.AccesoDenegadoException;
import com.adbrand.core.shared.error.NoAutenticadoException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// HU 43: tokens válidos, vencidos, revocados y cuentas desactivadas, sin base de datos.
class TokenRenovacionServiceTest {

    private static final String MENSAJE_INVALIDO = "Tu sesión venció. Vuelve a iniciar sesión.";

    private TokenRenovacionRepository tokens;
    private UsuarioRepository usuarios;
    private ServicioJwt jwt;
    private TokenRenovacionService servicio;

    @BeforeEach
    void preparar() {
        tokens = mock(TokenRenovacionRepository.class);
        usuarios = mock(UsuarioRepository.class);
        jwt = mock(ServicioJwt.class);
        servicio = new TokenRenovacionService(tokens, usuarios, jwt, 30);
        when(tokens.save(any(TokenRenovacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(jwt.generar(any(Usuario.class))).thenReturn("jwt-nuevo");
    }

    private Usuario usuario(EstadoUsuario estado) {
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(1L);
        when(usuario.getEstado()).thenReturn(estado);
        return usuario;
    }

    private TokenRenovacion fila(String token, LocalDateTime vence, boolean revocado) {
        TokenRenovacion fila = new TokenRenovacion();
        fila.setUsuarioId(1L);
        fila.setTokenHash(TokenRenovacionService.hash(token));
        fila.setFechaVencimiento(vence);
        fila.setRevocado(revocado);
        return fila;
    }

    @Test
    void crearGuardaSoloElHashYUnaFechaFutura() {
        String token = servicio.crear(1L);

        ArgumentCaptor<TokenRenovacion> captor = ArgumentCaptor.forClass(TokenRenovacion.class);
        verify(tokens).save(captor.capture());
        TokenRenovacion guardado = captor.getValue();

        assertThat(token).isNotBlank();
        assertThat(guardado.getTokenHash()).hasSize(64);
        assertThat(guardado.getTokenHash()).isEqualTo(TokenRenovacionService.hash(token));
        assertThat(guardado.getTokenHash()).isNotEqualTo(token);
        assertThat(guardado.getFechaVencimiento()).isAfter(LocalDateTime.now());
        assertThat(guardado.isRevocado()).isFalse();
    }

    @Test
    void unTokenVigenteDevuelveUnParNuevoYRevocaElAnterior() {
        String tokenViejo = "token-viejo";
        TokenRenovacion fila = fila(tokenViejo, LocalDateTime.now().plusDays(1), false);
        Usuario activo = usuario(EstadoUsuario.ACTIVO);
        when(tokens.findByTokenHash(TokenRenovacionService.hash(tokenViejo))).thenReturn(Optional.of(fila));
        when(usuarios.findById(1L)).thenReturn(Optional.of(activo));

        TokenRenovacionService.Renovacion resultado = servicio.renovar(tokenViejo);

        assertThat(resultado.token()).isEqualTo("jwt-nuevo");
        assertThat(resultado.tokenRenovacion()).isNotBlank();
        assertThat(fila.isRevocado()).isTrue();
        verify(tokens, times(2)).save(any(TokenRenovacion.class));
    }

    @Test
    void unTokenVencidoResponde401() {
        String tokenViejo = "token-vencido";
        TokenRenovacion fila = fila(tokenViejo, LocalDateTime.now().minusMinutes(1), false);
        when(tokens.findByTokenHash(TokenRenovacionService.hash(tokenViejo))).thenReturn(Optional.of(fila));

        assertThatThrownBy(() -> servicio.renovar(tokenViejo))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessage(MENSAJE_INVALIDO);
    }

    @Test
    void unTokenRevocadoResponde401() {
        String tokenViejo = "token-revocado";
        TokenRenovacion fila = fila(tokenViejo, LocalDateTime.now().plusDays(1), true);
        when(tokens.findByTokenHash(TokenRenovacionService.hash(tokenViejo))).thenReturn(Optional.of(fila));

        assertThatThrownBy(() -> servicio.renovar(tokenViejo))
                .isInstanceOf(NoAutenticadoException.class)
                .hasMessage(MENSAJE_INVALIDO);
    }

    @Test
    void unaCuentaInactivaNoPuedeRenovar() {
        String tokenViejo = "token-cuenta-inactiva";
        TokenRenovacion fila = fila(tokenViejo, LocalDateTime.now().plusDays(1), false);
        Usuario inactivo = usuario(EstadoUsuario.INACTIVO);
        when(tokens.findByTokenHash(TokenRenovacionService.hash(tokenViejo))).thenReturn(Optional.of(fila));
        when(usuarios.findById(1L)).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> servicio.renovar(tokenViejo))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void cerrarSesionRevocaElToken() {
        String token = "token-de-cierre";
        TokenRenovacion fila = fila(token, LocalDateTime.now().plusDays(1), false);
        when(tokens.findByTokenHash(TokenRenovacionService.hash(token))).thenReturn(Optional.of(fila));

        servicio.revocar(token);

        assertThat(fila.isRevocado()).isTrue();
        verify(tokens).save(fila);
    }
}
