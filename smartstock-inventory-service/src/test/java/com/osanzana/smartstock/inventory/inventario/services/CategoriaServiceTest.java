package com.osanzana.smartstock.inventory.inventario.services;

import com.osanzana.smartstock.inventory.core.entities.Categoria;
import com.osanzana.smartstock.inventory.core.entities.Comercio;
import com.osanzana.smartstock.inventory.core.repositories.CategoriaRepository;
import com.osanzana.smartstock.inventory.core.repositories.ComercioRepository;
import com.osanzana.smartstock.inventory.shared.dto.request.CategoriaRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.CategoriaResponseDTO;
import com.osanzana.smartstock.inventory.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ComercioRepository comercioRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    private Categoria categoria;
    private Comercio comercio;
    private CategoriaRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        comercio = Comercio.builder().id(1L).build();
        categoria = Categoria.builder().id(1L).nombre("Bebidas").comercio(comercio).build();
        requestDTO = CategoriaRequestDTO.builder().nombre("Bebidas").build();
    }

    @Test
    void listarPorComercio_Success() {
        when(categoriaRepository.findByComercioId(1L)).thenReturn(List.of(categoria));
        List<CategoriaResponseDTO> result = categoriaService.listarPorComercio(1L);
        assertEquals(1, result.size());
        assertEquals("Bebidas", result.get(0).getNombre());
    }

    @Test
    void crear_Success() {
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(categoria);

        CategoriaResponseDTO result = categoriaService.crear(requestDTO, 1L);

        assertNotNull(result);
        assertEquals("Bebidas", result.getNombre());
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void crear_ComercioNotFound() {
        when(comercioRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> categoriaService.crear(requestDTO, 1L));
    }
}
