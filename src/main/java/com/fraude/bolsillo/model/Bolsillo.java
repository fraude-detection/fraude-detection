package com.fraude.bolsillo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_bolsillo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bolsillo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "num_documento", nullable = false)
    private String numDocumento;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    /**
     * BOLSILLO | META | COLCHON
     */
    @Column(nullable = false)
    private String tipo;

    @Column(precision = 20, scale = 2, nullable = false)
    private BigDecimal saldo;

    /** Solo aplica para tipo META */
    @Column(name = "monto_objetivo", precision = 20, scale = 2)
    private BigDecimal montoObjetivo;

    /** Solo aplica para tipo META */
    @Column(name = "fecha_limite")
    private LocalDate fechaLimite;

    @Builder.Default
    private boolean completado = false;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;
}
