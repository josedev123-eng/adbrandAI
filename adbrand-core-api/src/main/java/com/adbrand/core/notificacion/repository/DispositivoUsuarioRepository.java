package com.adbrand.core.notificacion.repository;

import com.adbrand.core.notificacion.entity.DispositivoUsuario;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// HU 50: dispositivos que reciben los avisos. El servicio limpia los tokens que
// Firebase marca como inválidos y reasigna el token cuando cambia de cuenta.
public interface DispositivoUsuarioRepository extends JpaRepository<DispositivoUsuario, Long> {

    Optional<DispositivoUsuario> findByFcmToken(String fcmToken);

    List<DispositivoUsuario> findByUsuarioIdAndActivoTrueOrderByLastUsedAtDesc(Long usuarioId);

    List<DispositivoUsuario> findByFcmTokenIn(Collection<String> tokens);

    long deleteByFcmTokenIn(Collection<String> tokens);
}
