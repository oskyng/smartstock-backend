package com.osanzana.smartstock.finance.core.repositories;

import com.osanzana.smartstock.finance.core.entities.LoteInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<LoteInventario, Long> {
    List<LoteInventario> findByComercioId(Long comercioId);

    /**
     * Sobrescribe findAll() con fetch join: MotorPreciosScheduler lee todos los lotes fuera de
     * una transacción (para no poner en riesgo transacciones compartidas entre lotes) y luego
     * pasa cada uno a calcularPrecioDinamico(), que abre su propia transacción por lote. Sin este
     * fetch, producto/categoria/comercio (LAZY) quedan huérfanos de sesión y lanzan
     * LazyInitializationException al intentar cargarlos dentro de esa transacción distinta.
     */
    @Override
    @Query("SELECT l FROM LoteInventario l JOIN FETCH l.producto p JOIN FETCH p.categoria JOIN FETCH l.comercio")
    List<LoteInventario> findAll();
}
