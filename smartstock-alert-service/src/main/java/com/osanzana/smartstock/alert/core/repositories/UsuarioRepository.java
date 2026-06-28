package com.osanzana.smartstock.alert.core.repositories;

import com.osanzana.smartstock.alert.core.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByRut(String rut);
    List<Usuario> findByRolNombreAndActivoAndComercioId(String rolNombre, Integer activo, Long comercioId);
}
