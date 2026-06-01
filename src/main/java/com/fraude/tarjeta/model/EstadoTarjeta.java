package com.fraude.tarjeta.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_estado_tarjeta")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoTarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_tarjeta")
    private Integer id;

    @Column(name = "nombre_estado_tarjeta", nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "descripcion_estado_tarjeta", length = 200)
    private String descripcion;
}
