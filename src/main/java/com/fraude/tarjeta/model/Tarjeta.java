package com.fraude.tarjeta.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_tarjeta")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarjeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarjeta")
    private Integer id;

    @Column(name = "num_documento_tarjeta", nullable = false)
    private String numDocumento;

    /** CREDITO o DEBITO */
    @Column(name = "tipo_tarjeta", nullable = false)
    private String tipoTarjeta;

    /** Últimos 4 dígitos de la tarjeta */
    @Column(name = "ultimos_cuatro_tarjeta")
    private String ultimosCuatro;

    @Column(name = "nombre_titular_tarjeta", nullable = false)
    private String nombreTitular;

    /** MM/YY */
    @Column(name = "fecha_expiracion_tarjeta")
    private String fechaExpiracion;

    /** FK normalizada a tbl_marca_tarjeta */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_marca_tarjeta")
    private MarcaTarjeta marcaTarjeta;

    /** Compatibilidad hacia atrás: devuelve el nombre de la marca como string */
    @JsonProperty("marca")
    public String getMarca() {
        return marcaTarjeta != null ? marcaTarjeta.getNombre() : null;
    }

    /** ID del customer en Stripe */
    @Column(name = "stripe_customer_id_tarjeta")
    private String stripeCustomerId;

    /** ID del PaymentMethod en Stripe */
    @Column(name = "stripe_payment_method_id_tarjeta")
    private String stripePaymentMethodId;

    /**
     * FK normalizada a tbl_estado_tarjeta
     * (ACTIVA, PENDIENTE, ELIMINADA, RECHAZADA, BLOQUEADA)
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado_tarjeta")
    private EstadoTarjeta estadoTarjeta;

    /** Compatibilidad hacia atrás: devuelve el ID del estado */
    @JsonProperty("estadoId")
    public Integer getEstadoId() {
        return estadoTarjeta != null ? estadoTarjeta.getId() : null;
    }

    /** Nombre legible del estado (ACTIVA, PENDIENTE, etc.) */
    @JsonProperty("estadoNombre")
    public String getEstadoNombre() {
        return estadoTarjeta != null ? estadoTarjeta.getNombre() : null;
    }

    /** Límite de crédito asignado por el admin (solo tarjetas CREDITO) */
    @Column(name = "limite_credito_tarjeta")
    private Double limiteCredito;

    /** Crédito disponible actual (se reduce al pagar con tarjeta de crédito) */
    @Column(name = "credito_disponible_tarjeta")
    private Double creditoDisponible;

    /** Saldo cargado en la tarjeta (solo tarjetas DEBITO) */
    @Column(name = "saldo_tarjeta")
    private Double saldoTarjeta;

    /** Motivo del rechazo (opcional) */
    @Column(name = "motivo_rechazo_tarjeta")
    private String motivoRechazo;

    @Column(name = "fecha_creacion_tarjeta")
    private LocalDateTime fechaCreacion;
}
