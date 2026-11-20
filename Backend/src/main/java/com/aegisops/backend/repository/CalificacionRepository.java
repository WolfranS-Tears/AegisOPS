package com.aegisops.backend.repository;

import com.aegisops.backend.domain.Calificacion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalificacionRepository extends JpaRepository<Calificacion, Long> {

    Optional<Calificacion> findByRespuestaId(Long respuestaId);
}
