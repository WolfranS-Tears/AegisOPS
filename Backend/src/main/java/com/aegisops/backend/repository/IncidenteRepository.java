package com.aegisops.backend.repository;

import com.aegisops.backend.domain.Incidente;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IncidenteRepository extends JpaRepository<Incidente, Long>, JpaSpecificationExecutor<Incidente> {

    @Override
    @EntityGraph(attributePaths = {"categoria", "usuario"})
    Page<Incidente> findAll(Specification<Incidente> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"categoria", "usuario"})
    Optional<Incidente> findWithRelacionesById(Long id);
}
