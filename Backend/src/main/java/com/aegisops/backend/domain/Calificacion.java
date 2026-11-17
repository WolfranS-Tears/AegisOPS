package com.aegisops.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "calificacion")
public class Calificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "respuesta_id", nullable = false, unique = true)
    private RespuestaIA respuesta;

    @Column(nullable = false)
    private Short puntuacion;

    @Column(length = 500)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    @PreUpdate
    void alGuardar() {
        fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public RespuestaIA getRespuesta() { return respuesta; }
    public void setRespuesta(RespuestaIA respuesta) { this.respuesta = respuesta; }
    public Short getPuntuacion() { return puntuacion; }
    public void setPuntuacion(Short puntuacion) { this.puntuacion = puntuacion; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public LocalDateTime getFecha() { return fecha; }
}
