package com.osanzana.smartstock.inventory.inventario.services;

import com.osanzana.smartstock.inventory.core.entities.Comercio;
import com.osanzana.smartstock.inventory.core.entities.Proveedor;
import com.osanzana.smartstock.inventory.core.repositories.ComercioRepository;
import com.osanzana.smartstock.inventory.core.repositories.ProveedorRepository;
import com.osanzana.smartstock.inventory.shared.dto.request.ProveedorRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.ProveedorResponseDTO;
import com.osanzana.smartstock.inventory.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ComercioRepository comercioRepository;

    public List<ProveedorResponseDTO> listarPorComercio(Long comercioId) {
        return proveedorRepository.findByComercioId(comercioId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProveedorResponseDTO crear(ProveedorRequestDTO dto, Long comercioId) {
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado"));

        Proveedor proveedor = Proveedor.builder()
                .rutEmpresa(dto.getRutEmpresa())
                .razonSocial(dto.getRazonSocial())
                .contactoEmail(dto.getContactoEmail())
                .comercio(comercio)
                .build();

        return mapToResponseDTO(proveedorRepository.save(proveedor));
    }

    private ProveedorResponseDTO mapToResponseDTO(Proveedor proveedor) {
        return ProveedorResponseDTO.builder()
                .id(proveedor.getId())
                .rutEmpresa(proveedor.getRutEmpresa())
                .razonSocial(proveedor.getRazonSocial())
                .contactoEmail(proveedor.getContactoEmail())
                .build();
    }
}
