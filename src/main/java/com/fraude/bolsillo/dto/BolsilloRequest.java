package com.fraude.bolsillo.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BolsilloRequest {
    private String nombre;
    private String descripcion;
    private String tipo; // BOLSILLO | META | COLCHON
    private BigDecimal montoObjetivo;
    private LocalDate fechaLimite;
}
