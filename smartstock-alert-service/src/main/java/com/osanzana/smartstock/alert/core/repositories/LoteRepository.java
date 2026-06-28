package com.osanzana.smartstock.alert.core.repositories;

import com.osanzana.smartstock.alert.core.entities.LoteInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<LoteInventario, Long> {
    List<LoteInventario> findByComercioId(Long comercioId);
}
