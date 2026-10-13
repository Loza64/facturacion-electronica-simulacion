package com.server.app.dte.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.server.app.dte.model.DteAlmacenado;

public interface DteAlmacenadoRepository extends JpaRepository<DteAlmacenado, Long> {

  boolean existsByCodigoGeneracion(String codigoGeneracion);

  boolean existsByNitEmisorAndNumeroControl(String nitEmisor, String numeroControl);

  Optional<DteAlmacenado> findByCodigoGeneracion(String codigoGeneracion);
}
