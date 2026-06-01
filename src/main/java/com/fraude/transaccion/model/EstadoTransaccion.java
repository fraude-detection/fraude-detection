package com.fraude.transaccion.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_estado_transaccion")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoTransaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_transaccion")
    private Integer id;

    @Column(name = "nombre_estado_transaccion", nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "descripcion_estado_transaccion", length = 200)
    private String descripcion;
}
