package com.osanzana.smartstock.inventory.inventario.services;

import com.osanzana.smartstock.inventory.shared.dto.request.LoteRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.LoteResponseDTO;
import java.util.List;

public interface LoteService {
    LoteResponseDTO guardarLote(LoteRequestDTO loteDto, Long comercioId);
    List<LoteResponseDTO> listarTodos();
    List<LoteResponseDTO> listarPorComercio(Long comercioId);
}
