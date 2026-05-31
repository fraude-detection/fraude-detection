package com.fraude.bolsillo.service;

import com.fraude.bolsillo.dto.BolsilloRequest;
import com.fraude.bolsillo.model.Bolsillo;
import com.fraude.bolsillo.repository.BolsilloRepository;
import com.fraude.cuenta.model.Cuenta;
import com.fraude.cuenta.repository.CuentaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class BolsilloService {

    private final BolsilloRepository bolsilloRepository;
    private final CuentaRepository cuentaRepository;

    public BolsilloService(BolsilloRepository bolsilloRepository, CuentaRepository cuentaRepository) {
        this.bolsilloRepository = bolsilloRepository;
        this.cuentaRepository = cuentaRepository;
    }

    public List<Bolsillo> listar(String numDocumento) {
        return bolsilloRepository.findByNumDocumentoOrderByFechaCreacionDesc(numDocumento);
    }

    @Transactional
    public Bolsillo crear(String numDocumento, BolsilloRequest request) {
        String tipo = request.getTipo() != null ? request.getTipo().toUpperCase() : "BOLSILLO";

        // Solo puede existir un COLCHON por usuario
        if ("COLCHON".equals(tipo) && bolsilloRepository.existsByNumDocumentoAndTipo(numDocumento, "COLCHON")) {
            throw new RuntimeException("Ya tienes un Colchón creado");
        }

        Bolsillo bolsillo = Bolsillo.builder()
                .numDocumento(numDocumento)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .tipo(tipo)
                .saldo(BigDecimal.ZERO)
                .montoObjetivo(request.getMontoObjetivo())
                .fechaLimite(request.getFechaLimite())
                .completado(false)
                .fechaCreacion(LocalDateTime.now())
                .build();

        return bolsilloRepository.save(bolsillo);
    }

    @Transactional
    public Bolsillo consignar(Integer bolsilloId, String numDocumento, BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto debe ser mayor a cero");
        }

        Bolsillo bolsillo = bolsilloRepository.findById(bolsilloId)
                .orElseThrow(() -> new RuntimeException("Bolsillo no encontrado"));

        if (!bolsillo.getNumDocumento().equals(numDocumento)) {
            throw new RuntimeException("No tienes permiso sobre este bolsillo");
        }

        Cuenta cuenta = cuentaRepository.findByNumDocumento(numDocumento)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new RuntimeException("Saldo insuficiente en tu cuenta principal");
        }

        cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        cuentaRepository.save(cuenta);

        bolsillo.setSaldo(bolsillo.getSaldo().add(monto));

        // Marcar META como completada si alcanzó el objetivo
        if ("META".equals(bolsillo.getTipo()) && bolsillo.getMontoObjetivo() != null
                && bolsillo.getSaldo().compareTo(bolsillo.getMontoObjetivo()) >= 0) {
            bolsillo.setCompletado(true);
        }

        return bolsilloRepository.save(bolsillo);
    }

    @Transactional
    public Bolsillo retirar(Integer bolsilloId, String numDocumento, BigDecimal monto) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto debe ser mayor a cero");
        }

        Bolsillo bolsillo = bolsilloRepository.findById(bolsilloId)
                .orElseThrow(() -> new RuntimeException("Bolsillo no encontrado"));

        if (!bolsillo.getNumDocumento().equals(numDocumento)) {
            throw new RuntimeException("No tienes permiso sobre este bolsillo");
        }

        if (bolsillo.getSaldo().compareTo(monto) < 0) {
            throw new RuntimeException("Saldo insuficiente en el bolsillo");
        }

        Cuenta cuenta = cuentaRepository.findByNumDocumento(numDocumento)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

        bolsillo.setSaldo(bolsillo.getSaldo().subtract(monto));
        bolsillo.setCompletado(false);
        bolsilloRepository.save(bolsillo);

        cuenta.setSaldo(cuenta.getSaldo().add(monto));
        cuentaRepository.save(cuenta);

        return bolsillo;
    }

    @Transactional
    public void eliminar(Integer bolsilloId, String numDocumento) {
        Bolsillo bolsillo = bolsilloRepository.findById(bolsilloId)
                .orElseThrow(() -> new RuntimeException("Bolsillo no encontrado"));

        if (!bolsillo.getNumDocumento().equals(numDocumento)) {
            throw new RuntimeException("No tienes permiso sobre este bolsillo");
        }

        // Devolver saldo a cuenta principal si tiene saldo
        if (bolsillo.getSaldo().compareTo(BigDecimal.ZERO) > 0) {
            Cuenta cuenta = cuentaRepository.findByNumDocumento(numDocumento)
                    .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));
            cuenta.setSaldo(cuenta.getSaldo().add(bolsillo.getSaldo()));
            cuentaRepository.save(cuenta);
            log.info("Saldo {} devuelto al eliminar bolsillo {} de {}", bolsillo.getSaldo(), bolsilloId, numDocumento);
        }

        bolsilloRepository.delete(bolsillo);
    }
}
