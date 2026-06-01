package com.fraude.factura.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_servicio")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Integer id;

    @Column(name = "nombre_servicio", unique = true, nullable = false, length = 50)
    private String nombre;

    @Column(name = "descripcion_servicio", length = 255)
    private String descripcion;
}
