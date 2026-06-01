package com.fraude.clavedin.service;

import com.fraude.clavedin.model.ClaveDinamica;
import com.fraude.clavedin.repository.ClaveDinamicaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class ClaveDinamicaService {

    private static final int MINUTOS_VIGENCIA = 5;
    private final ClaveDinamicaRepository repository;
    private final SecureRandom random = new SecureRandom();

    public ClaveDinamicaService(ClaveDinamicaRepository repository) {
        this.repository = repository;
    }

    /**
     * Genera una nueva clave dinámica de 6 dígitos para el usuario.
     * Invalida claves anteriores pendientes del mismo usuario.
     */
    @Transactional
    public Map<String, Object> generar(String numDocumento) {
        // Invalidar claves anteriores vigentes del mismo usuario
        repository.findByNumDocumentoAndUsadoFalseAndFechaExpiracionAfter(numDocumento, LocalDateTime.now())
                .forEach(c -> {
                    c.setUsado(true);
                    repository.save(c);
                });

        String codigo = String.format("%06d", random.nextInt(1_000_000));
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(MINUTOS_VIGENCIA);

        ClaveDinamica clave = ClaveDinamica.builder()
                .numDocumento(numDocumento)
                .codigo(codigo)
                .fechaExpiracion(expiracion)
                .build();
        repository.save(clave);

        return Map.of(
                "codigo", codigo,
                "expiracion", expiracion.toString(),
                "minutosVigencia", MINUTOS_VIGENCIA);
    }

    /**
     * Valida una clave dinámica y la marca como usada si es válida.
     */
    @Transactional
    public Map<String, Object> validar(String numDocumento, String codigo) {
        Optional<ClaveDinamica> opt = repository
                .findByNumDocumentoAndCodigoAndUsadoFalseAndFechaExpiracionAfter(
                        numDocumento, codigo, LocalDateTime.now());

        if (opt.isEmpty()) {
            return Map.of("valida", false, "mensaje", "Clave inválida o expirada");
        }

        ClaveDinamica clave = opt.get();
        clave.setUsado(true);
        repository.save(clave);

        return Map.of("valida", true, "mensaje", "Clave válida");
    }
}
