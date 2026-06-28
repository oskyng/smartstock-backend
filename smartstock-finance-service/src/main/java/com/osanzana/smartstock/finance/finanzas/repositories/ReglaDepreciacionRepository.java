package com.osanzana.smartstock.finance.finanzas.repositories;

import com.osanzana.smartstock.finance.core.entities.ReglaDepreciacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReglaDepreciacionRepository extends JpaRepository<ReglaDepreciacion, Long> {
    List<ReglaDepreciacion> findByCategoriaIdAndActiva(Long categoriaId, Integer activa);
    List<ReglaDepreciacion> findByCategoriaIdAndComercioIdAndActiva(Long categoriaId, Long comercioId, Integer activa);
    List<ReglaDepreciacion> findByComercioId(Long comercioId);
}
