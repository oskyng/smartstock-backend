package com.osanzana.smartstock.auth.core.repositories;

import com.osanzana.smartstock.auth.core.entities.LoteInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<LoteInventario, Long> {
    List<LoteInventario> findByComercioId(Long comercioId);
}
