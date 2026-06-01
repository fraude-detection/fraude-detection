package com.fraude.factura.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tbl_factura")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_factura")
    private Integer id;

    @Column(name = "num_documento_factura", nullable = false)
    private String numDocumento;

    /** FK normalizada a tbl_servicio */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_servicio")
    private Servicio servicio;

    @Column(name = "descripcion_factura")
    private String descripcion;

    @Column(name = "referencia_factura", nullable = false)
    private String referencia;

    @Column(name = "monto_factura", nullable = false)
    private Double monto;

    /** FK normalizada a tbl_estado_factura */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_estado_factura")
    private EstadoFactura estadoFactura;

    @Column(name = "fecha_vencimiento_factura")
    private LocalDateTime fechaVencimiento;

    @Column(name = "fecha_pago_factura")
    private LocalDateTime fechaPago;

    @Column(name = "id_tarjeta")
    private Integer tarjetaId;

    /** ID del PaymentIntent en Stripe */
    @Column(name = "stripe_payment_intent_id_factura")
    private String stripePaymentIntentId;

    @Column(name = "fecha_creacion_factura")
    private LocalDateTime fechaCreacion;

    /** Compatibilidad hacia atrás: devuelve el nombre del servicio como string */
    @JsonProperty("tipoServicio")
    public String getTipoServicio() {
        return servicio != null ? servicio.getNombre() : null;
    }

    /** Compatibilidad hacia atrás: devuelve el nombre del estado como string */
    @JsonProperty("estado")
    public String getEstado() {
        return estadoFactura != null ? estadoFactura.getNombre() : null;
    }
}
