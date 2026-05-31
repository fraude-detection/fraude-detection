package com.fraude.usuario.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserRequest {
    private String nombre;
    private String apellido;
    private String email;
    private String passwordActual;
    private String passwordNuevo;
}
