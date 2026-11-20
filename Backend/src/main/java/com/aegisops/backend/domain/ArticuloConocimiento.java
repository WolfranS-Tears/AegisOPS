package com.aegisops.backend.domain;

import com.aegisops.backend.domain.Enums.EstadoArticulo;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "articulo_conocimiento")
public class ArticuloConocimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200, unique = true)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String problema;

    @Column(nullable = false, columnDefinition = "text")
    private String sintomas;

    @Column(nullable = false, columnDefinition = "text")
    private String causa;

    @Column(nullable = false, columnDefinition = "text")
    private String solucion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoArticulo estado = EstadoArticulo.BORRADOR;

    @Column(nullable = false)
    private boolean verificado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = fechaCreacion;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    /** Regla RF02: solo artículos activos y verificados pueden ser fuente del RAG. */
    public boolean esFuenteValida() {
        return estado == EstadoArticulo.ACTIVO && verificado;
    }

    /** Texto que se convierte en embedding (lo usa el flujo de indexación). */
    public String textoParaEmbedding() {
        return "Título: " + titulo
                + "\nCategoría: " + categoria.getNombre()
                + "\nProblema: " + problema
                + "\nSíntomas: " + sintomas
                + "\nCausa: " + causa
                + "\nSolución: " + solucion;
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getProblema() { return problema; }
    public void setProblema(String problema) { this.problema = problema; }
    public String getSintomas() { return sintomas; }
    public void setSintomas(String sintomas) { this.sintomas = sintomas; }
    public String getCausa() { return causa; }
    public void setCausa(String causa) { this.causa = causa; }
    public String getSolucion() { return solucion; }
    public void setSolucion(String solucion) { this.solucion = solucion; }
    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
    public EstadoArticulo getEstado() { return estado; }
    public void setEstado(EstadoArticulo estado) { this.estado = estado; }
    public boolean isVerificado() { return verificado; }
    public void setVerificado(boolean verificado) { this.verificado = verificado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
}
