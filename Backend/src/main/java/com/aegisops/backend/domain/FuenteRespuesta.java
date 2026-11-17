package com.aegisops.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "fuente_respuesta")
public class FuenteRespuesta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "respuesta_id", nullable = false)
    private RespuestaIA respuesta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "articulo_id", nullable = false)
    private ArticuloConocimiento articulo;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal similitud;

    protected FuenteRespuesta() {
    }

    public FuenteRespuesta(ArticuloConocimiento articulo, BigDecimal similitud) {
        this.articulo = articulo;
        this.similitud = similitud;
    }

    public Long getId() { return id; }
    public RespuestaIA getRespuesta() { return respuesta; }
    void setRespuesta(RespuestaIA respuesta) { this.respuesta = respuesta; }
    public ArticuloConocimiento getArticulo() { return articulo; }
    public BigDecimal getSimilitud() { return similitud; }
}
