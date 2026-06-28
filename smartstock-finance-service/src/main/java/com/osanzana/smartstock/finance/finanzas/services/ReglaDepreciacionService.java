package com.osanzana.smartstock.finance.finanzas.services;

import com.osanzana.smartstock.finance.core.entities.Usuario;
import com.osanzana.smartstock.finance.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.finance.core.entities.Comercio;
import com.osanzana.smartstock.finance.core.repositories.ComercioRepository;
import com.osanzana.smartstock.finance.core.entities.ReglaDepreciacion;
import com.osanzana.smartstock.finance.finanzas.repositories.ReglaDepreciacionRepository;
import com.osanzana.smartstock.finance.core.entities.Categoria;
import com.osanzana.smartstock.finance.core.repositories.CategoriaRepository;
import com.osanzana.smartstock.finance.shared.dto.request.ReglaDepreciacionRequestDTO;
import com.osanzana.smartstock.finance.shared.dto.response.ReglaDepreciacionResponseDTO;
import com.osanzana.smartstock.finance.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReglaDepreciacionService {

    private final ReglaDepreciacionRepository reglaRepository;
    private final CategoriaRepository categoriaRepository;
    private final ComercioRepository comercioRepository;
    private final UsuarioRepository usuarioRepository;

    public List<ReglaDepreciacionResponseDTO> listarPorComercio(Long comercioId) {
        return reglaRepository.findByComercioId(comercioId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReglaDepreciacionResponseDTO guardar(ReglaDepreciacionRequestDTO dto, Long comercioId, String emailGerente) {
        Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado"));

        Usuario gerente = usuarioRepository.findByEmail(emailGerente)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario gerente no encontrado"));

        ReglaDepreciacion regla = ReglaDepreciacion.builder()
                .categoria(categoria)
                .diasCriticosMin(dto.getDiasCriticosMin())
                .porcentajeDescuento(dto.getPorcentajeDescuento())
                .comercio(comercio)
                .gerente(gerente)
                .activa(1)
                .build();

        return mapToResponseDTO(reglaRepository.save(regla));
    }

    @Transactional
    public void eliminar(Long id) {
        ReglaDepreciacion regla = reglaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Regla no encontrada"));
        reglaRepository.delete(regla);
    }

    private ReglaDepreciacionResponseDTO mapToResponseDTO(ReglaDepreciacion regla) {
        return ReglaDepreciacionResponseDTO.builder()
                .id(regla.getId())
                .nombreCategoria(regla.getCategoria().getNombre())
                .diasCriticosMin(regla.getDiasCriticosMin())
                .porcentajeDescuento(regla.getPorcentajeDescuento())
                .nombreGerente(regla.getGerente().getNombre() + " " + regla.getGerente().getApellido())
                .activa(regla.getActiva())
                .build();
    }
}
