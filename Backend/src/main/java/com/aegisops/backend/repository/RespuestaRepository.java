package com.aegisops.backend.repository;

import com.aegisops.backend.domain.RespuestaIA;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespuestaRepository extends JpaRepository<RespuestaIA, Long> {
}
