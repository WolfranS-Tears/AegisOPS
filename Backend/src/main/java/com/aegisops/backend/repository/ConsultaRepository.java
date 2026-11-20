package com.aegisops.backend.repository;

import com.aegisops.backend.domain.ConsultaIA;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultaRepository extends JpaRepository<ConsultaIA, Long> {

    @EntityGraph(attributePaths = {"incidente", "respuesta", "respuesta.calificacion"})
    @Query("""
            select c from ConsultaIA c
            where (:incidenteId is null or c.incidente.id = :incidenteId)
            order by c.fecha desc, c.id desc
            """)
    List<ConsultaIA> historial(@Param("incidenteId") Long incidenteId);

    @EntityGraph(attributePaths = {"incidente", "respuesta", "respuesta.calificacion"})
    Optional<ConsultaIA> findDetalleById(Long id);
}
