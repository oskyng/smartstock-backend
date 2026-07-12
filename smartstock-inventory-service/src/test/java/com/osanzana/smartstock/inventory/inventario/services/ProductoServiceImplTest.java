package com.osanzana.smartstock.inventory.inventario.services;

import com.osanzana.smartstock.inventory.core.entities.Categoria;
import com.osanzana.smartstock.inventory.core.entities.Comercio;
import com.osanzana.smartstock.inventory.core.entities.Producto;
import com.osanzana.smartstock.inventory.core.repositories.CategoriaRepository;
import com.osanzana.smartstock.inventory.core.repositories.ComercioRepository;
import com.osanzana.smartstock.inventory.core.repositories.ProductoRepository;
import com.osanzana.smartstock.inventory.shared.dto.request.ProductoRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.ProductoResponseDTO;
import com.osanzana.smartstock.inventory.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ComercioRepository comercioRepository;

    @InjectMocks
    private ProductoService productoService;

    private Producto producto;
    private Categoria categoria;
    private Comercio comercio;
    private ProductoRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        categoria = Categoria.builder().id(1L).nombre("Lácteos").build();
        comercio = Comercio.builder().id(1L).build();
        producto = Producto.builder()
                .id(1L)
                .nombre("Leche")
                .codigoBarra("123456")
                .precioBase(new BigDecimal("1000"))
                .categoria(categoria)
                .comercio(comercio)
                .build();

        requestDTO = ProductoRequestDTO.builder()
                .nombre("Leche")
                .codigoBarra("123456")
                .precioBase(new BigDecimal("1000"))
                .idCategoria(1L)
                .build();
    }

    @Test
    void listarPorComercio_Success() {
        when(productoRepository.findByComercioId(1L)).thenReturn(List.of(producto));
        List<ProductoResponseDTO> result = productoService.listarPorComercio(1L);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void guardar_Success() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));
        when(productoRepository.save(any(Producto.class))).thenReturn(producto);
        
        ProductoResponseDTO result = productoService.guardar(requestDTO, 1L);
        
        assertNotNull(result);
        assertEquals("Leche", result.getNombre());
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    void obtenerPorId_Success() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        ProductoResponseDTO result = productoService.obtenerPorId(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void obtenerPorId_NotFound() {
        when(productoRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productoService.obtenerPorId(1L));
    }
    @Test
    void listarTodos_Success() {
        when(productoRepository.findAll()).thenReturn(List.of(producto));
        List<ProductoResponseDTO> result = productoService.listarTodos();
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Leche", result.get(0).getNombre());
    }

    @Test
    void guardar_CategoriaNotFound() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productoService.guardar(requestDTO, 1L));
    }

    @Test
    void guardar_ComercioNotFound() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(comercioRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> productoService.guardar(requestDTO, 1L));
    }
}
