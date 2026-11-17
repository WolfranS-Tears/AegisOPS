package com.aegisops.backend.repository;

import com.aegisops.backend.domain.FuenteRespuesta;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FuenteRepository extends JpaRepository<FuenteRespuesta, Long> {

    @EntityGraph(attributePaths = {"articulo", "articulo.categoria"})
    List<FuenteRespuesta> findByRespuestaIdOrderBySimilitudDesc(Long respuestaId);
}
