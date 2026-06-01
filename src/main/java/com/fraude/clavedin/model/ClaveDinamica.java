package com.fraude.clavedin.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_clave_dinamica")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClaveDinamica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_clave_dinamica")
    private Long id;

    @Column(name = "num_documento_clave_dinamica", nullable = false)
    private String numDocumento;

    @Column(name = "codigo_clave_dinamica", nullable = false, length = 6)
    private String codigo;

    @Column(name = "fecha_expiracion_clave_dinamica", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "usado_clave_dinamica", nullable = false)
    @Builder.Default
    private boolean usado = false;
}
