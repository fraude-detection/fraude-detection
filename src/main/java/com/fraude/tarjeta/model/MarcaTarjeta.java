package com.fraude.tarjeta.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_marca_tarjeta")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarcaTarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_marca_tarjeta")
    private Integer id;

    /** VISA, MASTERCARD, AMEX, UNKNOWN */
    @Column(name = "nombre_marca_tarjeta", nullable = false, unique = true)
    private String nombre;

    @Column(name = "descripcion_marca_tarjeta")
    private String descripcion;
}
