package com.fraude.transaccion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_transaccion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion")
    private Integer id;

    @Column(name = "monto_transaccion")
    private Double monto;

    @Column(name = "cuenta_origen_transaccion")
    private String cuentaOrigenId;

    @Column(name = "cuenta_destino_transaccion")
    private String cuentaDestinoId;

    /** FK normalizada a tbl_estado_transaccion */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado_transaccion")
    private EstadoTransaccion estadoTransaccion;

    /** Compatibilidad hacia atrás: expone el ID del estado */
    @JsonProperty("estadoId")
    public Integer getEstadoId() {
        return estadoTransaccion != null ? estadoTransaccion.getId() : null;
    }

    /** Nombre del estado (PENDIENTE, APROBADA, RECHAZADA) */
    @JsonProperty("estadoNombre")
    public String getEstadoNombre() {
        return estadoTransaccion != null ? estadoTransaccion.getNombre() : null;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_transaccion")
    private TipoTransaccion tipoTransaccion;

    @Column(name = "fecha_transaccion")
    private LocalDateTime fechaCreacion;

    /** Código numérico de 6 dígitos para retiro en cajero */
    @Column(name = "codigo_retiro_transaccion", length = 10)
    private String codigoRetiro;

    /** Fecha/hora en que expira el código de retiro */
    @Column(name = "fecha_expiracion_codigo_transaccion")
    private LocalDateTime fechaExpiracionCodigo;

    /** Retrocompatibilidad: expone el nombre del tipo como string en el JSON */
    @JsonProperty("tipoTransaccionNombre")
    public String getTipoTransaccionNombre() {
        return tipoTransaccion != null ? tipoTransaccion.getNombre() : null;
    }
}