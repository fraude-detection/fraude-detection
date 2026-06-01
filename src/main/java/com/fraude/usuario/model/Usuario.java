package com.fraude.usuario.model;

import jakarta.persistence.*;
import lombok.*;
import com.fraude.rol.model.Rol;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @EmbeddedId
    private UsuarioId id;

    @Column(name = "id_rol")
    private Integer rolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_rol", referencedColumnName = "id_rol", insertable = false, updatable = false)
    private Rol rol;

    @Column(name = "nombre_usuario")
    private String nombre;

    @Column(name = "apellido_usuario")
    private String apellido;

    @Column(name = "email_usuario")
    private String email;

    @Column(name = "password_hash_usuario")
    private String passwordHash;

    @Column(name = "estado_usuario")
    private Integer estadoId;

    @Column(name = "fecha_creacion_usuario")
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion_usuario")
    private LocalDateTime fechaActualizacion;
}