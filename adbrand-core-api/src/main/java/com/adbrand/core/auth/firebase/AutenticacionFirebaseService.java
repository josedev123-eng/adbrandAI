package com.adbrand.core.auth.firebase;

import com.adbrand.core.auth.dto.LoginResponse;
import com.adbrand.core.auth.dto.UsuarioResponse;
import com.adbrand.core.auth.entity.Usuario;
import com.adbrand.core.auth.repository.UsuarioRepository;
import com.adbrand.core.auth.service.LoginService;
import com.adbrand.core.shared.error.AccesoDenegadoException;
import com.adbrand.core.shared.error.ConflictoException;
import com.adbrand.core.shared.error.NoAutenticadoException;
import com.adbrand.core.shared.error.RecursoNoEncontradoException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// App móvil: iniciar sesión y registrarse con Firebase Authentication.
// Firebase confirma quién es el usuario; Spring busca su empresa (la misma que usa la web) y entrega el mismo JWT de siempre.
// Así todo lo demás (anuncios, kit, planes, suspensión de cuentas) sigue igual.
@Service
public class AutenticacionFirebaseService {

    static final String CORREO_NO_VERIFICADO = "Confirma tu correo con el enlace que te enviamos y vuelve a intentar.";

    private final VerificadorTokenFirebase verificador;
    private final UsuarioRepository usuarios;
    private final LoginService login;
    private final PasswordEncoder codificador;

    public AutenticacionFirebaseService(VerificadorTokenFirebase verificador, UsuarioRepository usuarios,
                                        LoginService login, PasswordEncoder codificador) {
        this.verificador = verificador;
        this.usuarios = usuarios;
        this.login = login;
        this.codificador = codificador;
    }

    // Iniciar sesión con Firebase:
    // 1. Si la cuenta ya está unida a ese usuario de Firebase (firebase_uid), entra.
    // 2. Si no, se busca la empresa por correo y se une, pero solo si Firebase confirmó que el correo es suyo
    //    (así nadie entra a la cuenta de otro creando en Firebase un usuario con su correo).
    @Transactional
    public LoginResponse iniciarSesion(String idToken) {
        UsuarioFirebase firebase = verificador.verificar(idToken);

        Optional<Usuario> unida = usuarios.findByFirebaseUid(firebase.uid());
        if (unida.isPresent()) {
            return entregar(unida.get(), firebase);
        }

        Usuario porCorreo = usuarios.findByCorreo(firebase.correo())
                .orElseThrow(() -> new RecursoNoEncontradoException("CUENTA_NO_ENCONTRADA",
                        "No encontramos una cuenta con ese correo. Regístrate primero."));
        if (porCorreo.getFirebaseUid() != null) {
            throw new AccesoDenegadoException("CUENTA_DE_OTRO_FIREBASE",
                    "Esta cuenta ya está unida a otro acceso de Firebase.");
        }
        if (!firebase.correoVerificado()) {
            throw new AccesoDenegadoException("CORREO_NO_VERIFICADO", CORREO_NO_VERIFICADO);
        }
        porCorreo.setFirebaseUid(firebase.uid());
        return entregar(porCorreo, firebase);
    }

    // Registro desde la app: crea la empresa con el correo del token y la deja unida a Firebase.
    // La contraseña la guarda Firebase; en Spring queda una al azar (para la web se usa "¿Olvidaste tu contraseña?").
    @Transactional
    public UsuarioResponse registrar(FirebaseDtos.RegistroFirebaseRequest datos) {
        UsuarioFirebase firebase = verificador.verificar(datos.idToken());
        if (usuarios.existsByCorreo(firebase.correo()) || usuarios.findByFirebaseUid(firebase.uid()).isPresent()) {
            throw new ConflictoException("CORREO_REPETIDO", "Ya existe una cuenta con ese correo. Inicia sesión.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(datos.nombre());
        usuario.setNombreEmpresa(datos.nombreEmpresa());
        usuario.setCorreo(firebase.correo());
        usuario.setFirebaseUid(firebase.uid());
        usuario.setContrasenaHash(codificador.encode(UUID.randomUUID().toString()));
        return UsuarioResponse.desde(usuarios.saveAndFlush(usuario));
    }

    // Migración de una cuenta creada en la web: con la sesión de siempre (JWT) se une el usuario de Firebase recién creado.
    // La app lo hace la primera vez que alguien entra al móvil con su correo y contraseña de la web.
    @Transactional
    public UsuarioResponse vincular(Long usuarioId, String idToken) {
        UsuarioFirebase firebase = verificador.verificar(idToken);
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoAutenticadoException("NO_AUTENTICADO", "Inicia sesión para continuar."));

        if (!usuario.getCorreo().equalsIgnoreCase(firebase.correo())) {
            throw new AccesoDenegadoException("CORREO_DISTINTO", "El acceso de Firebase es de otro correo.");
        }
        Optional<Usuario> otra = usuarios.findByFirebaseUid(firebase.uid());
        if (otra.isPresent() && !otra.get().getId().equals(usuarioId)) {
            throw new ConflictoException("FIREBASE_YA_UNIDO", "Ese acceso de Firebase ya está unido a otra cuenta.");
        }
        usuario.setFirebaseUid(firebase.uid());
        return UsuarioResponse.desde(usuario);
    }

    private LoginResponse entregar(Usuario usuario, UsuarioFirebase firebase) {
        // Una cuenta nueva del móvil entra recién cuando confirma su correo (Firebase manda el enlace).
        if (!firebase.correoVerificado()) {
            throw new AccesoDenegadoException("CORREO_NO_VERIFICADO", CORREO_NO_VERIFICADO);
        }
        return login.entregarSesion(usuario);
    }
}
