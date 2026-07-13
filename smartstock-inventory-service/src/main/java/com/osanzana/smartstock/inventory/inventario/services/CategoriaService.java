package com.osanzana.smartstock.inventory.inventario.services;

import com.osanzana.smartstock.inventory.core.entities.Categoria;
import com.osanzana.smartstock.inventory.core.entities.Comercio;
import com.osanzana.smartstock.inventory.core.repositories.CategoriaRepository;
import com.osanzana.smartstock.inventory.core.repositories.ComercioRepository;
import com.osanzana.smartstock.inventory.shared.dto.request.CategoriaRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.CategoriaResponseDTO;
import com.osanzana.smartstock.inventory.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ComercioRepository comercioRepository;

    public List<CategoriaResponseDTO> listarPorComercio(Long comercioId) {
        return categoriaRepository.findByComercioId(comercioId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoriaResponseDTO crear(CategoriaRequestDTO dto, Long comercioId) {
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado"));

        Categoria categoria = Categoria.builder()
                .nombre(dto.getNombre())
                .comercio(comercio)
                .build();

        return mapToResponseDTO(categoriaRepository.save(categoria));
    }

    private CategoriaResponseDTO mapToResponseDTO(Categoria categoria) {
        return CategoriaResponseDTO.builder()
                .id(categoria.getId())
                .nombre(categoria.getNombre())
                .build();
    }
}
