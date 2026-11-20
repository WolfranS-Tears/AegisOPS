package com.aegisops.backend.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "respuesta_ia")
public class RespuestaIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consulta_id", nullable = false, unique = true)
    private ConsultaIA consulta;

    @Column(nullable = false, columnDefinition = "text")
    private String respuesta;

    /** Pasos recomendados, uno por línea. */
    @Column(columnDefinition = "text")
    private String pasos;

    @Column(length = 100)
    private String modelo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @OneToMany(mappedBy = "respuesta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("similitud DESC")
    private List<FuenteRespuesta> fuentes = new ArrayList<>();

    @OneToOne(mappedBy = "respuesta", fetch = FetchType.LAZY)
    private Calificacion calificacion;

    @PrePersist
    void alCrear() {
        fecha = LocalDateTime.now();
    }

    public void agregarFuente(FuenteRespuesta fuente) {
        fuente.setRespuesta(this);
        fuentes.add(fuente);
    }

    public Long getId() { return id; }
    public ConsultaIA getConsulta() { return consulta; }
    public void setConsulta(ConsultaIA consulta) { this.consulta = consulta; }
    public String getRespuesta() { return respuesta; }
    public void setRespuesta(String respuesta) { this.respuesta = respuesta; }
    public String getPasos() { return pasos; }
    public void setPasos(String pasos) { this.pasos = pasos; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public LocalDateTime getFecha() { return fecha; }
    public List<FuenteRespuesta> getFuentes() { return fuentes; }
    public Calificacion getCalificacion() { return calificacion; }
    public void setCalificacion(Calificacion calificacion) { this.calificacion = calificacion; }
}
