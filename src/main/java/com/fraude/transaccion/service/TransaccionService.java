package com.fraude.transaccion.service;

import com.fraude.cuenta.model.Cuenta;
import com.fraude.cuenta.repository.CuentaRepository;
import com.fraude.transaccion.model.EstadoTransaccion;
import com.fraude.transaccion.model.TipoTransaccion;
import com.fraude.transaccion.model.Transaccion;
import com.fraude.transaccion.repository.EstadoTransaccionRepository;
import com.fraude.transaccion.repository.TipoTransaccionRepository;
import com.fraude.transaccion.repository.TransaccionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final FraudeService fraudeService;
    private final CuentaRepository cuentaRepository;
    private final TipoTransaccionRepository tipoTransaccionRepository;
    private final EstadoTransaccionRepository estadoTransaccionRepository;

    public TransaccionService(TransaccionRepository transaccionRepository, FraudeService fraudeService,
            CuentaRepository cuentaRepository, TipoTransaccionRepository tipoTransaccionRepository,
            EstadoTransaccionRepository estadoTransaccionRepository) {
        this.transaccionRepository = transaccionRepository;
        this.fraudeService = fraudeService;
        this.cuentaRepository = cuentaRepository;
        this.tipoTransaccionRepository = tipoTransaccionRepository;
        this.estadoTransaccionRepository = estadoTransaccionRepository;
    }

    private TipoTransaccion getTipo(String nombre) {
        String key = (nombre != null && !nombre.isBlank()) ? nombre.toUpperCase() : "TRANSFERENCIA";
        return tipoTransaccionRepository.findByNombre(key)
                .orElseGet(() -> tipoTransaccionRepository.findByNombre("TRANSFERENCIA")
                        .orElseThrow(() -> new RuntimeException("Tipo de transacción no encontrado: " + key)));
    }

    private EstadoTransaccion getEstado(String nombre) {
        return estadoTransaccionRepository.findByNombre(nombre)
                .orElseThrow(() -> new RuntimeException("Estado de transacción no encontrado: " + nombre));
    }

    @Transactional
    public Transaccion procesarTransaccion(Transaccion transaccion) {
        try {
            log.info("Procesando transacción: origen={}, destino={}, monto={}",
                    transaccion.getCuentaOrigenId(),
                    transaccion.getCuentaDestinoId(),
                    transaccion.getMonto());

            if (transaccion.getMonto() == null || transaccion.getMonto() <= 0) {
                throw new IllegalArgumentException("Monto debe ser mayor a 0");
            }
            if (transaccion.getCuentaOrigenId() == null || transaccion.getCuentaOrigenId().isEmpty()) {
                throw new IllegalArgumentException("Cuenta origen es requerida");
            }
            if (transaccion.getCuentaDestinoId() == null || transaccion.getCuentaDestinoId().isEmpty()) {
                throw new IllegalArgumentException("Cuenta destino es requerida");
            }

            Cuenta origen = cuentaRepository.findById(transaccion.getCuentaOrigenId())
                    .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no existe"));
            Cuenta destino = cuentaRepository.findById(transaccion.getCuentaDestinoId())
                    .orElseThrow(() -> new IllegalArgumentException("Cuenta destino no existe"));

            // Evaluar fraude (devuelve nombre: APROBADA, PENDIENTE, RECHAZADA)
            String estadoNombre = fraudeService.evaluarFraude(transaccion);
            log.info("Estado de fraude evaluado: {}", estadoNombre);
            transaccion.setEstadoTransaccion(getEstado(estadoNombre));

            // Si es aprobada, actualizar saldos
            if ("APROBADA".equals(estadoNombre)) {
                BigDecimal montoTransferencia = BigDecimal.valueOf(transaccion.getMonto());
                if (origen.getSaldo().compareTo(montoTransferencia) < 0) {
                    log.error("Saldo insuficiente: disponible={}, requerido={}",
                            origen.getSaldo(), montoTransferencia);
                    transaccion.setEstadoTransaccion(getEstado("RECHAZADA"));
                } else {
                    origen.setSaldo(origen.getSaldo().subtract(montoTransferencia));
                    destino.setSaldo(destino.getSaldo().add(montoTransferencia));
                    cuentaRepository.save(origen);
                    cuentaRepository.save(destino);
                    log.info("Saldos actualizados. Origen: {}, Destino: {}",
                            origen.getSaldo(), destino.getSaldo());
                }
            } else {
                log.info("ℹTransacción no aprobada (estado={}). Saldos no se actualizan.", estadoNombre);
            }

            // Asignar tipo y fecha
            if (transaccion.getTipoTransaccion() == null) {
                transaccion.setTipoTransaccion(getTipo(null));
            } else {
                transaccion.setTipoTransaccion(getTipo(transaccion.getTipoTransaccion().getNombre()));
            }
            transaccion.setFechaCreacion(LocalDateTime.now());

            Transaccion resultado = transaccionRepository.save(transaccion);
            log.info("Transacción guardada con ID: {}, Estado: {}", resultado.getId(), resultado.getEstadoNombre());
            return resultado;

        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al procesar transacción: {}", e.getMessage(), e);
            throw new RuntimeException("Error al procesar transacción: " + e.getMessage());
        }
    }

    public List<Transaccion> obtenerHistorial(String cuentaId) {
        try {
            List<Transaccion> enviadas = transaccionRepository.findByCuentaOrigenId(cuentaId);
            List<Transaccion> recibidas = transaccionRepository.findByCuentaDestinoId(cuentaId);
            List<Transaccion> historial = new ArrayList<>();
            historial.addAll(enviadas);
            historial.addAll(recibidas);
            historial.sort(Comparator.comparing(Transaccion::getFechaCreacion,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return historial;
        } catch (Exception e) {
            log.error("Error al obtener historial: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Transaccion> obtenerTodasTransacciones() {
        try {
            List<Transaccion> transacciones = transaccionRepository.findAll();
            transacciones.sort(Comparator.comparing(Transaccion::getFechaCreacion,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return transacciones;
        } catch (Exception e) {
            log.error("Error al obtener todas las transacciones: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Transaccion> obtenerTransaccionesPendientes() {
        try {
            // Excluir retiros-con-código: esos están pendientes de uso en cajero, no de
            // aprobación admin
            List<Transaccion> pendientes = transaccionRepository.findByEstadoTransaccionNombre("PENDIENTE")
                    .stream()
                    .filter(t -> t.getCodigoRetiro() == null)
                    .collect(Collectors.toList());
            pendientes.sort(Comparator.comparing(Transaccion::getFechaCreacion,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return pendientes;
        } catch (Exception e) {
            log.error("Error al obtener transacciones pendientes: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Transactional
    public Transaccion actualizarEstadoTransaccion(Integer id, String nuevoEstadoNombre) {
        try {
            Transaccion transaccion = transaccionRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada"));

            String estadoActual = transaccion.getEstadoNombre();

            if (!"PENDIENTE".equals(estadoActual)) {
                throw new IllegalArgumentException("Solo se pueden validar transacciones en estado PENDIENTE");
            }

            if (!"APROBADA".equals(nuevoEstadoNombre) && !"RECHAZADA".equals(nuevoEstadoNombre)) {
                throw new IllegalArgumentException("Estado inválido. Debe ser APROBADA o RECHAZADA");
            }

            // Si cambia de PENDIENTE a APROBADA, actualizar saldos
            if ("APROBADA".equals(nuevoEstadoNombre)) {
                String tipoNombre = transaccion.getTipoTransaccion() != null
                        ? transaccion.getTipoTransaccion().getNombre()
                        : "TRANSFERENCIA";
                BigDecimal montoTransferencia = BigDecimal.valueOf(transaccion.getMonto());

                if ("DEPOSITO".equals(tipoNombre)) {
                    // Origen es "CAJA" (no existe en BD); solo acreditar a la cuenta destino
                    Cuenta destino = cuentaRepository.findById(transaccion.getCuentaDestinoId())
                            .orElseThrow(() -> new IllegalArgumentException("Cuenta destino no existe"));
                    destino.setSaldo(destino.getSaldo().add(montoTransferencia));
                    cuentaRepository.save(destino);
                } else if ("RETIRO".equals(tipoNombre)) {
                    // Destino es "CAJA" (no existe en BD); solo debitar la cuenta origen
                    Cuenta origen = cuentaRepository.findById(transaccion.getCuentaOrigenId())
                            .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no existe"));
                    if (origen.getSaldo().compareTo(montoTransferencia) < 0) {
                        throw new IllegalArgumentException("Saldo insuficiente en la cuenta origen");
                    }
                    origen.setSaldo(origen.getSaldo().subtract(montoTransferencia));
                    cuentaRepository.save(origen);
                } else {
                    // TRANSFERENCIA: actualizar ambas cuentas
                    Cuenta origen = cuentaRepository.findById(transaccion.getCuentaOrigenId())
                            .orElseThrow(() -> new IllegalArgumentException("Cuenta origen no existe"));
                    Cuenta destino = cuentaRepository.findById(transaccion.getCuentaDestinoId())
                            .orElseThrow(() -> new IllegalArgumentException("Cuenta destino no existe"));
                    if (origen.getSaldo().compareTo(montoTransferencia) < 0) {
                        throw new IllegalArgumentException("Saldo insuficiente en la cuenta origen");
                    }
                    origen.setSaldo(origen.getSaldo().subtract(montoTransferencia));
                    destino.setSaldo(destino.getSaldo().add(montoTransferencia));
                    cuentaRepository.save(origen);
                    cuentaRepository.save(destino);
                }
            }

            transaccion.setEstadoTransaccion(getEstado(nuevoEstadoNombre));
            Transaccion actualizada = transaccionRepository.save(transaccion);
            log.info("Transacción actualizada: id={}, estado={}", id, nuevoEstadoNombre);
            return actualizada;

        } catch (IllegalArgumentException e) {
            log.error("Error de validación: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error inesperado al actualizar estado: {}", e.getMessage(), e);
            throw new RuntimeException("Error al actualizar estado: " + e.getMessage());
        }
    }

    @Transactional
    public Transaccion realizarDeposito(String numeroCuenta, Double monto) {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }
        Cuenta cuenta = cuentaRepository.findById(numeroCuenta)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + numeroCuenta));

        Transaccion transaccion = new Transaccion();
        transaccion.setCuentaOrigenId("CAJA");
        transaccion.setCuentaDestinoId(numeroCuenta);
        transaccion.setMonto(monto);
        transaccion.setTipoTransaccion(getTipo("DEPOSITO"));
        transaccion.setFechaCreacion(LocalDateTime.now());

        String estadoNombre = fraudeService.evaluarFraude(transaccion);
        transaccion.setEstadoTransaccion(getEstado(estadoNombre));

        if ("APROBADA".equals(estadoNombre)) {
            cuenta.setSaldo(cuenta.getSaldo().add(BigDecimal.valueOf(monto)));
            cuentaRepository.save(cuenta);
            log.info("Depósito aprobado. Cuenta: {}, Nuevo saldo: {}", numeroCuenta, cuenta.getSaldo());
        } else {
            log.info("Depósito no aprobado (estado={}). Saldo sin cambios.", estadoNombre);
        }

        return transaccionRepository.save(transaccion);
    }

    @Transactional
    public Transaccion realizarRetiro(String numeroCuenta, Double monto) {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }
        if (monto > 5_000_000) {
            throw new IllegalArgumentException("El monto máximo para retiro en cajero es $5,000,000");
        }
        cuentaRepository.findById(numeroCuenta)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada: " + numeroCuenta));

        // Generar código de 6 dígitos y establecer expiración a 10 minutos
        String codigo = String.format("%06d", new Random().nextInt(1_000_000));
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(10);

        Transaccion transaccion = new Transaccion();
        transaccion.setCuentaOrigenId(numeroCuenta);
        transaccion.setCuentaDestinoId("CAJA");
        transaccion.setMonto(monto);
        transaccion.setTipoTransaccion(getTipo("RETIRO"));
        transaccion.setFechaCreacion(LocalDateTime.now());
        transaccion.setCodigoRetiro(codigo);
        transaccion.setFechaExpiracionCodigo(expiracion);
        // El dinero NO se descuenta aún; queda pendiente hasta que se use en cajero
        transaccion.setEstadoTransaccion(getEstado("PENDIENTE"));

        log.info("Código de retiro generado: cuenta={}, monto={}, codigo={}, expira={}",
                numeroCuenta, monto, codigo, expiracion);
        return transaccionRepository.save(transaccion);
    }

    @Transactional
    public Transaccion usarCodigoRetiro(String codigo) {
        Transaccion transaccion = transaccionRepository.findByCodigoRetiro(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Código de retiro no válido"));

        if (!"PENDIENTE".equals(transaccion.getEstadoNombre())) {
            throw new IllegalArgumentException("Este código ya fue utilizado o fue cancelado");
        }
        if (LocalDateTime.now().isAfter(transaccion.getFechaExpiracionCodigo())) {
            transaccion.setEstadoTransaccion(getEstado("RECHAZADA"));
            transaccionRepository.save(transaccion);
            throw new IllegalArgumentException("El código ha expirado. Solicite uno nuevo.");
        }

        Cuenta cuenta = cuentaRepository.findById(transaccion.getCuentaOrigenId())
                .orElseThrow(() -> new IllegalArgumentException("Cuenta no encontrada"));
        BigDecimal montoRetiro = BigDecimal.valueOf(transaccion.getMonto());
        if (cuenta.getSaldo().compareTo(montoRetiro) < 0) {
            transaccion.setEstadoTransaccion(getEstado("RECHAZADA"));
            transaccionRepository.save(transaccion);
            throw new IllegalArgumentException("Saldo insuficiente para completar el retiro");
        }

        cuenta.setSaldo(cuenta.getSaldo().subtract(montoRetiro));
        cuentaRepository.save(cuenta);
        transaccion.setEstadoTransaccion(getEstado("APROBADA"));
        log.info("Retiro completado en cajero: cuenta={}, monto={}",
                transaccion.getCuentaOrigenId(), transaccion.getMonto());
        return transaccionRepository.save(transaccion);
    }

    @Transactional
    public Transaccion solicitarPrestamo(String cuentaOrigen, String cuentaDestino, Double monto) {
        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0");
        }
        cuentaRepository.findById(cuentaOrigen)
                .orElseThrow(() -> new IllegalArgumentException("La cuenta prestamista no existe: " + cuentaOrigen));
        cuentaRepository.findById(cuentaDestino)
                .orElseThrow(() -> new IllegalArgumentException("Cuenta destino no encontrada: " + cuentaDestino));
        if (cuentaOrigen.equals(cuentaDestino)) {
            throw new IllegalArgumentException("No puedes solicitar préstamo a tu propia cuenta");
        }

        Transaccion transaccion = new Transaccion();
        transaccion.setCuentaOrigenId(cuentaOrigen);
        transaccion.setCuentaDestinoId(cuentaDestino);
        transaccion.setMonto(monto);
        transaccion.setTipoTransaccion(getTipo("PRESTAMO"));
        transaccion.setFechaCreacion(LocalDateTime.now());
        transaccion.setEstadoTransaccion(getEstado("PENDIENTE"));

        log.info("Solicitud de préstamo: de={}, a={}, monto={}", cuentaOrigen, cuentaDestino, monto);
        return transaccionRepository.save(transaccion);
    }

    public List<Transaccion> obtenerPrestamosPendientesDePrestamista(String numeroCuenta) {
        return transaccionRepository.findPrestamosPendientesPorPrestamista(numeroCuenta);
    }

    @Transactional
    public Transaccion responderPrestamo(Integer id, String numeroCuentaPrestamista, String estado) {
        Transaccion transaccion = transaccionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transacción no encontrada"));

        String tipoNombre = transaccion.getTipoTransaccion() != null
                ? transaccion.getTipoTransaccion().getNombre()
                : "";
        if (!"PRESTAMO".equals(tipoNombre)) {
            throw new IllegalArgumentException("La transacción no es un préstamo");
        }
        if (!numeroCuentaPrestamista.equals(transaccion.getCuentaOrigenId())) {
            throw new IllegalArgumentException("Solo el titular de la cuenta prestamista puede responder");
        }
        if (!"PENDIENTE".equals(transaccion.getEstadoNombre())) {
            throw new IllegalArgumentException("Este préstamo ya fue respondido");
        }
        if (!"APROBADA".equals(estado) && !"RECHAZADA".equals(estado)) {
            throw new IllegalArgumentException("Estado inválido. Debe ser APROBADA o RECHAZADA");
        }

        if ("APROBADA".equals(estado)) {
            BigDecimal monto = BigDecimal.valueOf(transaccion.getMonto());
            Cuenta prestamista = cuentaRepository.findById(transaccion.getCuentaOrigenId())
                    .orElseThrow(() -> new IllegalArgumentException("Cuenta prestamista no encontrada"));
            Cuenta solicitante = cuentaRepository.findById(transaccion.getCuentaDestinoId())
                    .orElseThrow(() -> new IllegalArgumentException("Cuenta solicitante no encontrada"));
            if (prestamista.getSaldo().compareTo(monto) < 0) {
                throw new IllegalArgumentException("Saldo insuficiente para otorgar el préstamo");
            }
            prestamista.setSaldo(prestamista.getSaldo().subtract(monto));
            solicitante.setSaldo(solicitante.getSaldo().add(monto));
            cuentaRepository.save(prestamista);
            cuentaRepository.save(solicitante);
        }

        transaccion.setEstadoTransaccion(getEstado(estado));
        log.info("Préstamo respondido: id={}, prestamista={}, estado={}", id, numeroCuentaPrestamista, estado);
        return transaccionRepository.save(transaccion);
    }
}
