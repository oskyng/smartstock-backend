package com.osanzana.smartstock.auth.core.repositories;

import com.osanzana.smartstock.auth.core.entities.CodigoRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodigoRecuperacionRepository extends JpaRepository<CodigoRecuperacion, Long> {

    Optional<CodigoRecuperacion> findByUsuarioIdAndCodigoVerificadorAndUtilizado(
            Long usuarioId, String codigoVerificador, String utilizado);

    /**
     * Usada para invalidar códigos previos vía entidades gestionadas (ver RecuperacionService),
     * en vez de un @Modifying UPDATE en bloque: en Oracle Autonomous DB un UPDATE en bloque sobre
     * una tabla recién creada puede ejecutarse con paralelismo automático, y cualquier otra
     * sentencia sobre la misma tabla en la MISMA transacción (como el INSERT del nuevo código que
     * sigue inmediatamente) falla con ORA-12838 "cannot read/modify an object after modifying it
     * in parallel". Actualizar fila por fila a través del contexto de persistencia evita el DML
     * en bloque y por lo tanto ese problema.
     */
    List<CodigoRecuperacion> findByUsuarioIdAndUtilizado(Long usuarioId, String utilizado);
}
