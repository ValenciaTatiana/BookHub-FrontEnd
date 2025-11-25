package com.bookhub.ui.reportes.dto;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportes_sistema")
public class ReporteSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descripcion;
    private String tipo;
    private LocalDateTime fechaGeneracion;

    public ReporteSistema() {}

    public ReporteSistema(String descripcion, String tipo) {
        this.descripcion = descripcion;
        this.tipo = tipo;
        this.fechaGeneracion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public LocalDateTime getFechaGeneracion() { return fechaGeneracion; }
}
