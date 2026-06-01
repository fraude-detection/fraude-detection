package com.fraude.clavedin.repository;

import com.fraude.clavedin.model.ClaveDinamica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClaveDinamicaRepository extends JpaRepository<ClaveDinamica, Long> {

    Optional<ClaveDinamica> findByNumDocumentoAndCodigoAndUsadoFalseAndFechaExpiracionAfter(
            String numDocumento, String codigo, LocalDateTime ahora);

    List<ClaveDinamica> findByNumDocumentoAndUsadoFalseAndFechaExpiracionAfter(
            String numDocumento, LocalDateTime ahora);
}
