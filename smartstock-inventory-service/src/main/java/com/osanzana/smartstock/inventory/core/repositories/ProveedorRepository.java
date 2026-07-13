package com.osanzana.smartstock.inventory.core.repositories;

import com.osanzana.smartstock.inventory.core.entities.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    Optional<Proveedor> findByRutEmpresa(String rutEmpresa);
    List<Proveedor> findByComercioId(Long comercioId);
}
