package com.fraude.bolsillo.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoRequest {
    private BigDecimal monto;
}
