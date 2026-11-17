package com.aegisops.backend.repository;

import com.aegisops.backend.domain.ArticuloConocimiento;
import com.aegisops.backend.domain.Enums.EstadoArticulo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArticuloRepository extends JpaRepository<ArticuloConocimiento, Long> {

    @EntityGraph(attributePaths = "categoria")
    @Query("""
            select a from ArticuloConocimiento a
            where (:estado is null or a.estado = :estado)
              and (:categoriaId is null or a.categoria.id = :categoriaId)
              and (:texto is null or lower(a.titulo) like lower(concat('%', cast(:texto as string), '%'))
                   or lower(a.problema) like lower(concat('%', cast(:texto as string), '%')))
            order by a.id
            """)
    List<ArticuloConocimiento> buscar(@Param("estado") EstadoArticulo estado,
                                      @Param("categoriaId") Long categoriaId,
                                      @Param("texto") String texto);

    @EntityGraph(attributePaths = "categoria")
    Optional<ArticuloConocimiento> findWithCategoriaById(Long id);

    @EntityGraph(attributePaths = "categoria")
    List<ArticuloConocimiento> findAllByOrderByIdAsc();

    /** Fuentes válidas para el RAG: existen, están activas y verificadas. */
    @EntityGraph(attributePaths = "categoria")
    @Query("select a from ArticuloConocimiento a where a.id in :ids and a.estado = 'ACTIVO' and a.verificado = true")
    List<ArticuloConocimiento> findFuentesValidas(@Param("ids") Collection<Long> ids);
}
