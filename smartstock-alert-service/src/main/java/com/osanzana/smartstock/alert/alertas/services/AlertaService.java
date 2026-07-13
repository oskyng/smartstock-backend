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
    void atenderAlerta(Long alertaId);
    void procesarEscalamientoSLA();
    void procesarNuevoLote(com.osanzana.smartstock.alert.shared.dto.events.LoteEventDTO loteEvent);
}
