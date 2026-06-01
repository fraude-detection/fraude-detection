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
    @Column(name = "id_bolsillo")
    private Integer id;

    @Column(name = "num_documento_bolsillo", nullable = false)
    private String numDocumento;

    @Column(name = "nombre_bolsillo", nullable = false)
    private String nombre;

    @Column(name = "descripcion_bolsillo")
    private String descripcion;

    /**
     * BOLSILLO | META | COLCHON
     */
    @Column(name = "tipo_bolsillo", nullable = false)
    private String tipo;

    @Column(name = "saldo_bolsillo", precision = 20, scale = 2, nullable = false)
    private BigDecimal saldo;

    /** Solo aplica para tipo META */
    @Column(name = "monto_objetivo_bolsillo", precision = 20, scale = 2)
    private BigDecimal montoObjetivo;

    /** Solo aplica para tipo META */
    @Column(name = "fecha_limite_bolsillo")
    private LocalDate fechaLimite;

    @Builder.Default
    @Column(name = "completado_bolsillo")
    private boolean completado = false;

    @Column(name = "fecha_creacion_bolsillo")
    private LocalDateTime fechaCreacion;
}
