package com.osanzana.smartstock.finance.core.repositories;

import com.osanzana.smartstock.finance.core.entities.Comercio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComercioRepository extends JpaRepository<Comercio, Long> {
    Optional<Comercio> findByRutEmpresa(String rutEmpresa);
}
