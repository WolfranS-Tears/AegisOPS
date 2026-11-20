package com.aegisops.backend.domain;

import com.aegisops.backend.domain.Enums.EstadoConsulta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "consulta_ia")
public class ConsultaIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Opcional: una consulta puede hacerse sin incidente asociado. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incidente_id")
    private Incidente incidente;

    @Column(nullable = false, columnDefinition = "text")
    private String pregunta;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoConsulta estado = EstadoConsulta.PENDIENTE;

    /** Umbral de similitud aplicado en esta consulta (trazabilidad). */
    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal umbral;

    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    @OneToOne(mappedBy = "consulta", fetch = FetchType.LAZY)
    private RespuestaIA respuesta;

    @PrePersist
    void alCrear() {
        fecha = LocalDateTime.now();
    }

    public void marcarError(String mensaje) {
        this.estado = EstadoConsulta.ERROR;
        this.mensajeError = mensaje == null ? "Error desconocido"
                : mensaje.substring(0, Math.min(mensaje.length(), 500));
    }

    public Long getId() { return id; }
    public Incidente getIncidente() { return incidente; }
    public void setIncidente(Incidente incidente) { this.incidente = incidente; }
    public String getPregunta() { return pregunta; }
    public void setPregunta(String pregunta) { this.pregunta = pregunta; }
    public LocalDateTime getFecha() { return fecha; }
    public EstadoConsulta getEstado() { return estado; }
    public void setEstado(EstadoConsulta estado) { this.estado = estado; }
    public BigDecimal getUmbral() { return umbral; }
    public void setUmbral(BigDecimal umbral) { this.umbral = umbral; }
    public String getMensajeError() { return mensajeError; }
    public RespuestaIA getRespuesta() { return respuesta; }
    public void setRespuesta(RespuestaIA respuesta) { this.respuesta = respuesta; }
}
