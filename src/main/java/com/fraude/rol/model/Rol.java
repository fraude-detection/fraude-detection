package com.fraude.rol.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tbl_rol")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer id;

    @Column(name = "nombre_rol", unique = true, nullable = false)
    private String nombre;

}
