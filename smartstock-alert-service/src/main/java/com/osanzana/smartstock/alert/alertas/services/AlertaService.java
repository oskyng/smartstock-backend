package com.osanzana.smartstock.alert.alertas.services;

import com.osanzana.smartstock.alert.core.entities.ReglaDepreciacion;
import com.osanzana.smartstock.alert.core.entities.LoteInventario;
import com.osanzana.smartstock.alert.shared.dto.response.AlertaAuditoriaResponseDTO;
import com.osanzana.smartstock.alert.shared.dto.response.AlertaResponseDTO;
import java.util.List;

public interface AlertaService {
    List<AlertaResponseDTO> listarPendientes();
    List<AlertaResponseDTO> listarPorComercio(Long comercioId);
    List<AlertaResponseDTO> listarPendientesPorComercio(Long comercioId);
    List<AlertaAuditoriaResponseDTO> listarAuditoriaPorComercio(Long comercioId);
    void generarAlertaDescuento(LoteInventario lote, ReglaDepreciacion regla);
    void atenderAlerta(Long alertaId, Long comercioId);
    void procesarEscalamientoSLA();
    void procesarNuevoLote(com.osanzana.smartstock.alert.shared.dto.events.LoteEventDTO loteEvent);

    /**
     * Revisa todos los lotes que aún no tienen una alerta generada y crea las que correspondan
     * según las reglas de depreciación vigentes. Cubre tanto los lotes cuyo evento lote-creado
     * nunca se procesó (p.ej. por una caída del servicio o de Kafka) como los que no calificaban
     * al momento de crearse pero luego cruzaron su umbral crítico con el paso del tiempo.
     */
    void reconciliarLotesSinAlerta();
}
