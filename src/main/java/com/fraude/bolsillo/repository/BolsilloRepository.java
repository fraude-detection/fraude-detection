package com.fraude.bolsillo.repository;

import com.fraude.bolsillo.model.Bolsillo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BolsilloRepository extends JpaRepository<Bolsillo, Integer> {
    List<Bolsillo> findByNumDocumentoOrderByFechaCreacionDesc(String numDocumento);

    Optional<Bolsillo> findByNumDocumentoAndTipo(String numDocumento, String tipo);

    boolean existsByNumDocumentoAndTipo(String numDocumento, String tipo);
}
