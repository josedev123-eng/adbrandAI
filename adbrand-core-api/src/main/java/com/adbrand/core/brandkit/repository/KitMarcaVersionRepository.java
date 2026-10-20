package com.adbrand.core.brandkit.repository;

import com.adbrand.core.brandkit.entity.KitMarcaVersion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository: ediciones guardadas de un kit (HU 32).
public interface KitMarcaVersionRepository extends JpaRepository<KitMarcaVersion, Long> {

    // La más reciente primero; el id desempata dos ediciones del mismo instante.
    List<KitMarcaVersion> findByKitMarcaIdOrderByFechaCreacionDescIdDesc(Long kitMarcaId);
}
