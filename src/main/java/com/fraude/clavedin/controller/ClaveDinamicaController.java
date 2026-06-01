package com.fraude.clavedin.controller;

import com.fraude.clavedin.service.ClaveDinamicaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/clave-dinamica")
@CrossOrigin(origins = { "http://localhost:8081", "http://localhost:5173", "http://127.0.0.1:5173",
        "http://localhost:5174", "http://127.0.0.1:5174" })
public class ClaveDinamicaController {

    private final ClaveDinamicaService service;

    public ClaveDinamicaController(ClaveDinamicaService service) {
        this.service = service;
    }

    /**
     * POST /api/clave-dinamica/generar
     * Header: X-Num-Documento
     * Genera una clave dinámica de 6 dígitos vigente por 5 minutos.
     */
    @PostMapping("/generar")
    public ResponseEntity<Map<String, Object>> generar(
            @RequestHeader("X-Num-Documento") String numDocumento) {
        return ResponseEntity.ok(service.generar(numDocumento));
    }

    /**
     * POST /api/clave-dinamica/validar
     * Body: { "codigo": "123456" }
     * Header: X-Num-Documento
     */
    @PostMapping("/validar")
    public ResponseEntity<Map<String, Object>> validar(
            @RequestHeader("X-Num-Documento") String numDocumento,
            @RequestBody Map<String, String> body) {
        String codigo = body.getOrDefault("codigo", "").trim();
        if (codigo.length() != 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("valida", false, "mensaje", "La clave debe tener 6 dígitos"));
        }
        return ResponseEntity.ok(service.validar(numDocumento, codigo));
    }
}
