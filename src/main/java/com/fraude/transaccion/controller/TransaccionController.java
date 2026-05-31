package com.fraude.transaccion.controller;

import com.fraude.transaccion.model.Transaccion;
import com.fraude.transaccion.service.TransaccionService;
import com.fraude.usuario.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transacciones")
@CrossOrigin(origins = { "http://localhost:8081", "http://localhost:5173", "http://127.0.0.1:5173",
        "http://localhost:5174", "http://127.0.0.1:5174" })
public class TransaccionController {

    private final TransaccionService service;
    private final UsuarioService usuarioService;

    public TransaccionController(TransaccionService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<?> procesarTransaccion(@RequestBody Transaccion transaccion) {
        try {
            log.info("📥 Nueva solicitud de transacción recibida");
            Transaccion resultado = service.procesarTransaccion(transaccion);
            log.info("✅ Transacción procesada exitosamente");
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Validación fallida: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error al procesar transacción: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al procesar la transacción: " + e.getMessage());
            error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<?> obtenerHistorial(@PathVariable String numeroCuenta) {
        try {
            log.info("📋 Solicitud de historial para cuenta: {}", numeroCuenta);
            List<Transaccion> historial = service.obtenerHistorial(numeroCuenta);
            log.info("✅ Historial obtenido: {} transacciones", historial.size());
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            log.error("Error al obtener historial: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al obtener el historial");
            error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // Endpoints para administrador
    @GetMapping
    public ResponseEntity<?> obtenerTodasTransacciones(
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDocumento) {
        ResponseEntity<Map<String, Object>> authError = validarAdmin(adminDocumento);
        if (authError != null) {
            return authError;
        }

        try {
            log.info("Solicitud admin de todas las transacciones");
            return ResponseEntity.ok(service.obtenerTodasTransacciones());
        } catch (Exception e) {
            log.error("Error al obtener todas las transacciones: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al obtener transacciones");
            error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/pendientes")
    public ResponseEntity<?> obtenerTransaccionesPendientes(
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDocumento) {
        ResponseEntity<Map<String, Object>> authError = validarAdmin(adminDocumento);
        if (authError != null) {
            return authError;
        }

        try {
            log.info("👨‍💼 Solicitud de transacciones pendientes (Admin)");
            List<Transaccion> pendientes = service.obtenerTransaccionesPendientes();
            log.info("✅ Transacciones pendientes obtenidas: {}", pendientes.size());
            return ResponseEntity.ok(pendientes);
        } catch (Exception e) {
            log.error("Error al obtener transacciones pendientes: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al obtener transacciones pendientes");
            error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstadoTransaccion(
            @PathVariable Integer id,
            @RequestHeader(name = "X-Admin-Documento", required = false) String adminDocumento,
            @RequestBody Map<String, Object> body) {
        ResponseEntity<Map<String, Object>> authError = validarAdmin(adminDocumento);
        if (authError != null) {
            return authError;
        }

        try {
            // Acepta tanto "estadoNombre" (nuevo) como "estadoId" para compatibilidad
            String nuevoEstadoNombre = null;
            if (body.containsKey("estadoNombre")) {
                nuevoEstadoNombre = (String) body.get("estadoNombre");
            } else if (body.containsKey("estadoId")) {
                // Compatibilidad: 5=APROBADA, 6=RECHAZADA (legacy)
                Object raw = body.get("estadoId");
                int legacyId = raw instanceof Number ? ((Number) raw).intValue() : Integer.parseInt(raw.toString());
                if (legacyId == 5)
                    nuevoEstadoNombre = "APROBADA";
                else if (legacyId == 6)
                    nuevoEstadoNombre = "RECHAZADA";
            }

            if (!"APROBADA".equals(nuevoEstadoNombre) && !"RECHAZADA".equals(nuevoEstadoNombre)) {
                throw new IllegalArgumentException("Estado inválido. Debe ser APROBADA o RECHAZADA");
            }

            log.info("Solicitud de actualización de estado para transacción: id={}, estado={}", id, nuevoEstadoNombre);
            Transaccion actualizada = service.actualizarEstadoTransaccion(id, nuevoEstadoNombre);
            log.info("Estado de transacción actualizado: id={}, estado={}", id, nuevoEstadoNombre);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Validación fallida: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            error.put("status", HttpStatus.BAD_REQUEST.value());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error al actualizar estado: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al actualizar el estado");
            error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/deposito")
    public ResponseEntity<?> realizarDeposito(@RequestBody Map<String, Object> body) {
        try {
            String numeroCuenta = (String) body.get("numeroCuenta");
            Double monto = ((Number) body.get("monto")).doubleValue();
            log.info("Solicitud de depósito: cuenta={}, monto={}", numeroCuenta, monto);
            Transaccion resultado = service.realizarDeposito(numeroCuenta, monto);
            log.info("Depósito procesado: id={}, estado={}", resultado.getId(), resultado.getEstadoNombre());
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            log.warn("Depósito rechazado: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error al procesar depósito: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al procesar el depósito");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/retiro")
    public ResponseEntity<?> realizarRetiro(@RequestBody Map<String, Object> body) {
        try {
            String numeroCuenta = (String) body.get("numeroCuenta");
            Double monto = ((Number) body.get("monto")).doubleValue();
            log.info("Solicitud de retiro: cuenta={}, monto={}", numeroCuenta, monto);
            Transaccion resultado = service.realizarRetiro(numeroCuenta, monto);
            log.info("Retiro procesado: id={}, estado={}", resultado.getId(), resultado.getEstadoNombre());
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            log.warn("Retiro rechazado: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error al procesar retiro: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al procesar el retiro");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/retiro/usar-codigo")
    public ResponseEntity<?> usarCodigoRetiro(@RequestBody Map<String, Object> body) {
        try {
            String codigo = (String) body.get("codigo");
            if (codigo == null || codigo.isBlank()) {
                throw new IllegalArgumentException("El código es requerido");
            }
            log.info("Uso de código de retiro en cajero: codigo={}", codigo);
            Transaccion resultado = service.usarCodigoRetiro(codigo);
            log.info("Retiro en cajero completado: id={}, cuenta={}", resultado.getId(), resultado.getCuentaOrigenId());
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            log.warn("Código de retiro inválido: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error al usar código de retiro: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al procesar el retiro en cajero");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/prestamo")
    public ResponseEntity<?> solicitarPrestamo(@RequestBody Map<String, Object> body) {
        try {
            String cuentaOrigen = (String) body.get("cuentaOrigen");
            String cuentaDestino = (String) body.get("cuentaDestino");
            Double monto = ((Number) body.get("monto")).doubleValue();
            log.info("Solicitud de préstamo: de={}, a={}, monto={}", cuentaOrigen, cuentaDestino, monto);
            Transaccion resultado = service.solicitarPrestamo(cuentaOrigen, cuentaDestino, monto);
            log.info("Préstamo solicitado: id={}, estado={}", resultado.getId(), resultado.getEstadoNombre());
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            log.warn("Solicitud de préstamo rechazada: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error al solicitar préstamo: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al procesar la solicitud de préstamo");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/prestamos-pendientes/{numeroCuenta}")
    public ResponseEntity<?> obtenerPrestamosPendientes(@PathVariable String numeroCuenta) {
        try {
            List<Transaccion> pendientes = service.obtenerPrestamosPendientesDePrestamista(numeroCuenta);
            return ResponseEntity.ok(pendientes);
        } catch (Exception e) {
            log.error("Error al obtener préstamos pendientes: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al obtener préstamos pendientes");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/prestamo/{id}/responder")
    public ResponseEntity<?> responderPrestamo(
            @PathVariable Integer id,
            @RequestHeader("X-Numero-Cuenta") String numeroCuenta,
            @RequestBody Map<String, Object> body) {
        try {
            String estado = (String) body.get("estado");
            log.info("Respuesta a préstamo: id={}, prestamista={}, estado={}", id, numeroCuenta, estado);
            Transaccion resultado = service.responderPrestamo(id, numeroCuenta, estado);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            log.warn("Error al responder préstamo: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error al responder préstamo: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Error al procesar la respuesta");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private ResponseEntity<Map<String, Object>> validarAdmin(String adminDocumento) {
        if (adminDocumento == null || adminDocumento.isBlank()) {
            return construirError(HttpStatus.BAD_REQUEST, "Header X-Admin-Documento es requerido");
        }

        if (!usuarioService.esAdministrador(adminDocumento)) {
            return construirError(HttpStatus.FORBIDDEN, "Solo un administrador puede ejecutar esta acción");
        }

        return null;
    }

    private ResponseEntity<Map<String, Object>> construirError(HttpStatus status, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        error.put("status", status.value());
        return ResponseEntity.status(status).body(error);
    }
}