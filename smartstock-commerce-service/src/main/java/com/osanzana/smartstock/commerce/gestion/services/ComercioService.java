package com.osanzana.smartstock.commerce.gestion.services;

import com.osanzana.smartstock.commerce.core.entities.Comercio;
import com.osanzana.smartstock.commerce.core.repositories.ComercioRepository;
import com.osanzana.smartstock.commerce.shared.dto.request.ComercioRequestDTO;
import com.osanzana.smartstock.commerce.shared.dto.response.ComercioResponseDTO;
import com.osanzana.smartstock.commerce.shared.exception.ConflictException;
import com.osanzana.smartstock.commerce.shared.exception.ResourceNotFoundException;
import com.osanzana.smartstock.commerce.shared.exception.UnauthorizedActionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComercioService {

    private final ComercioRepository comercioRepository;

    @Transactional
    public ComercioResponseDTO crearComercio(ComercioRequestDTO request) {
        if (comercioRepository.findByRutEmpresa(request.getRutEmpresa()).isPresent()) {
            throw new ConflictException("El RUT de empresa ya está registrado");
        }

        Comercio comercio = Comercio.builder()
                .rutEmpresa(request.getRutEmpresa())
                .razonSocial(request.getRazonSocial())
                .rubro(request.getRubro())
                .estado("ACTIVO")
                .build();

        Comercio guardado = comercioRepository.save(comercio);
        log.info("Comercio creado: {} (ID: {})", guardado.getRazonSocial(), guardado.getId());
        return mapToResponse(guardado);
    }

    @Transactional(readOnly = true)
    public List<ComercioResponseDTO> listarComercios() {
        return comercioRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ComercioResponseDTO updateComercio(Long id, ComercioRequestDTO request, String rolSolicitante, Long idComercioContexto) {
        Comercio comercio = comercioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado"));

        if (!"ROLE_ADMIN_SISTEMA".equals(rolSolicitante)) {
            if (!comercio.getId().equals(idComercioContexto)) {
                throw new UnauthorizedActionException("No tienes permiso para modificar este comercio");
            }
        }

        if (!comercio.getRutEmpresa().equals(request.getRutEmpresa()) &&
                comercioRepository.findByRutEmpresa(request.getRutEmpresa()).isPresent()) {
            throw new ConflictException("El nuevo RUT de empresa ya está registrado");
        }

        comercio.setRutEmpresa(request.getRutEmpresa());
        comercio.setRazonSocial(request.getRazonSocial());
        comercio.setRubro(request.getRubro());
        
        Comercio actualizado = comercioRepository.save(comercio);
        return mapToResponse(actualizado);
    }

    @Transactional
    public void deleteComercio(Long id) {
        Comercio comercio = comercioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado"));
        comercioRepository.delete(comercio);
    }

    @Transactional(readOnly = true)
    public ComercioResponseDTO obtenerPorId(Long id, String rolSolicitante, Long idComercioContexto) {
        Comercio comercio = comercioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado"));

        if (!"ROLE_ADMIN_SISTEMA".equals(rolSolicitante)) {
            if (!comercio.getId().equals(idComercioContexto)) {
                throw new UnauthorizedActionException("No tienes permiso para ver este comercio");
            }
        }

        return mapToResponse(comercio);
    }

    private ComercioResponseDTO mapToResponse(Comercio comercio) {
        return ComercioResponseDTO.builder()
                .id(comercio.getId())
                .rutEmpresa(comercio.getRutEmpresa())
                .razonSocial(comercio.getRazonSocial())
                .rubro(comercio.getRubro())
                .fechaRegistro(comercio.getFechaRegistro())
                .estado(comercio.getEstado())
                .build();
    }
}
