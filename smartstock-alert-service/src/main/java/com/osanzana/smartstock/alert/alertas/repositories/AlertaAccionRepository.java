package com.osanzana.smartstock.alert.alertas.repositories;

import com.osanzana.smartstock.alert.alertas.entities.AlertaAccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertaAccionRepository extends JpaRepository<AlertaAccion, Long> {
    List<AlertaAccion> findByComercioId(Long comercioId);
    List<AlertaAccion> findByComercioIdAndEstadoAlerta(Long comercioId, String estadoAlerta);
    List<AlertaAccion> findByEstadoAlertaAndFechaLimiteAtencionBefore(String estadoAlerta, LocalDateTime fecha);
}
