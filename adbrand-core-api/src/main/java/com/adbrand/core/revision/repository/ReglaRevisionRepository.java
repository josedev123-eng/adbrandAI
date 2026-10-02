package com.adbrand.core.revision.repository;

import com.adbrand.core.revision.entity.ReglaRevision;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Repository: solo necesitamos las reglas que están activas.
public interface ReglaRevisionRepository extends JpaRepository<ReglaRevision, Long> {

    List<ReglaRevision> findByActivaTrue();
}