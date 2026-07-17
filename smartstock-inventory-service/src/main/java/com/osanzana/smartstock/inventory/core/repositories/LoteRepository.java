package com.osanzana.smartstock.inventory.core.repositories;

import com.osanzana.smartstock.inventory.core.entities.LoteInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<LoteInventario, Long> {

    @Query("SELECT l FROM LoteInventario l JOIN FETCH l.producto p JOIN FETCH p.categoria WHERE l.comercio.id = :comercioId")
    List<LoteInventario> findByComercioId(@Param("comercioId") Long comercioId);
}
