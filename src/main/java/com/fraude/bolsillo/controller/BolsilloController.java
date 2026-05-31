package com.fraude.bolsillo.controller;

import com.fraude.bolsillo.dto.BolsilloRequest;
import com.fraude.bolsillo.dto.MovimientoRequest;
import com.fraude.bolsillo.model.Bolsillo;
import com.fraude.bolsillo.service.BolsilloService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bolsillos")
@CrossOrigin(origins = { "http://localhost:8081", "http://localhost:5173", "http://127.0.0.1:5173",
        "http://localhost:5174", "http://127.0.0.1:5174" })
public class BolsilloController {

    private final BolsilloService service;

    public BolsilloController(BolsilloService service) {
        this.service = service;
    }

    /** Listar todos los bolsillos del usuario */
    @GetMapping
    public List<Bolsillo> listar(@RequestHeader("X-Num-Documento") String numDocumento) {
        return service.listar(numDocumento);
    }

    /** Crear bolsillo / meta / colchón */
    @PostMapping
    public ResponseEntity<?> crear(@RequestHeader("X-Num-Documento") String numDocumento,
            @RequestBody BolsilloRequest request) {
        try {
            Bolsillo b = service.crear(numDocumento, request);
            return ResponseEntity.ok(b);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Consignar dinero desde cuenta principal al bolsillo */
    @PostMapping("/{id}/consignar")
    public ResponseEntity<?> consignar(@PathVariable Integer id,
            @RequestHeader("X-Num-Documento") String numDocumento,
            @RequestBody MovimientoRequest request) {
        try {
            Bolsillo b = service.consignar(id, numDocumento, request.getMonto());
            return ResponseEntity.ok(b);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Retirar dinero del bolsillo a cuenta principal */
    @PostMapping("/{id}/retirar")
    public ResponseEntity<?> retirar(@PathVariable Integer id,
            @RequestHeader("X-Num-Documento") String numDocumento,
            @RequestBody MovimientoRequest request) {
        try {
            Bolsillo b = service.retirar(id, numDocumento, request.getMonto());
            return ResponseEntity.ok(b);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Eliminar bolsillo (devuelve saldo a cuenta principal) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id,
            @RequestHeader("X-Num-Documento") String numDocumento) {
        try {
            service.eliminar(id, numDocumento);
            return ResponseEntity.ok(Map.of("mensaje", "Bolsillo eliminado correctamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
