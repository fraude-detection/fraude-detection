package com.fraude.factura.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_estado_factura")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_factura")
    private Integer id;

    @Column(name = "nombre_estado_factura", unique = true, nullable = false, length = 50)
    private String nombre;

    @Column(name = "descripcion_estado_factura", length = 255)
    private String descripcion;
}
