package com.osanzana.smartstock.finance.core.repositories;

import com.osanzana.smartstock.finance.core.entities.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Optional<Producto> findByCodigoBarra(String codigoBarra);
    List<Producto> findByComercioId(Long comercioId);
}
