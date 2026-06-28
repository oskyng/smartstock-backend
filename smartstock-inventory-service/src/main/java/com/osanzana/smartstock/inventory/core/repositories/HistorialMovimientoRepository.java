package com.osanzana.smartstock.inventory.core.repositories;

import com.osanzana.smartstock.inventory.core.entities.HistorialMovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialMovimientoRepository extends JpaRepository<HistorialMovimientoStock, Long> {
    List<HistorialMovimientoStock> findByLoteId(Long loteId);
    List<HistorialMovimientoStock> findByComercioId(Long comercioId);
}
