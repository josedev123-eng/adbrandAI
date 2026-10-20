package com.adbrand.core.auth.config;

import com.adbrand.core.auth.service.ServicioJwt;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

// HU 22: revisa el token de cada petición a /api/*. Si es válido, deja el id del usuario en el request
// ("usuarioId") y los controllers lo leen con @RequestAttribute("usuarioId"). Si no, responde 401.
// No lleva @Component a propósito: se registra en SeguridadConfig, así las pruebas @WebMvcTest no lo cargan.
public class FiltroJwt extends OncePerRequestFilter {

    public static final String ATRIBUTO_USUARIO = "usuarioId";
    private static final String PREFIJO = "Bearer ";
    private static final String RESPUESTA_401 =
            "{\"codigo\":\"NO_AUTENTICADO\",\"mensaje\":\"Inicia sesión para continuar.\",\"campos\":{}}";

    private final ServicioJwt jwt;

    public FiltroJwt(ServicioJwt jwt) {
        this.jwt = jwt;
    }

    // Rutas que no piden token: registro, inicio de sesión y recuperar contraseña (/api/auth/**, salvo la sesión),
    // la lista de planes (HU 25 de Edu) y las peticiones OPTIONS que hace el navegador antes de cada llamada.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        String metodo = request.getMethod();

        if ("OPTIONS".equals(metodo)) {
            return true;
        }
        if (ruta.startsWith("/api/auth/") && !ruta.equals("/api/auth/sesion")) {
            return true;
        }
        return "GET".equals(metodo) && ruta.equals("/api/planes");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            rechazar(response);
            return;
        }

        try {
            Long usuarioId = jwt.leerUsuarioId(cabecera.substring(PREFIJO.length()).trim());
            request.setAttribute(ATRIBUTO_USUARIO, usuarioId);
        } catch (ServicioJwt.TokenInvalidoException ex) {
            // Token alterado, vencido o mal escrito.
            rechazar(response);
            return;
        }

        cadena.doFilter(request, response);
    }

    private void rechazar(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(RESPUESTA_401);
    }
}
