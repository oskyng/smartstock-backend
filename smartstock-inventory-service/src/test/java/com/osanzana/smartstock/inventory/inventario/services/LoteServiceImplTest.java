package com.osanzana.smartstock.inventory.inventario.services;

import com.osanzana.smartstock.inventory.core.entities.*;
import com.osanzana.smartstock.inventory.core.repositories.*;
import com.osanzana.smartstock.inventory.inventario.events.LoteEventProducer;
import com.osanzana.smartstock.inventory.shared.dto.request.LoteRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.LoteResponseDTO;
import com.osanzana.smartstock.inventory.shared.exception.BusinessException;
import com.osanzana.smartstock.inventory.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoteServiceImplTest {

    @Mock private LoteRepository loteRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private ComercioRepository comercioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private LoteEventProducer loteEventProducer;

    @InjectMocks
    private LoteServiceImpl loteService;

    private LoteRequestDTO requestDTO;
    private Producto producto;
    private Proveedor proveedor;
    private Comercio comercio;
    private Usuario operador;

    @BeforeEach
    void setUp() {
        requestDTO = LoteRequestDTO.builder()
                .idProducto(1L)
                .idProveedor(1L)
                .cantidadInicial(100)
                .costoUnitario(new BigDecimal("10.0"))
                .precioDinamico(new BigDecimal("15.0"))
                .fechaVencimiento(LocalDate.now().plusMonths(6))
                .build();

        producto = Producto.builder().id(1L).nombre("Producto Test").build();
        proveedor = Proveedor.builder().id(1L).razonSocial("Proveedor Test").build();
        comercio = Comercio.builder().id(1L).razonSocial("Comercio Test").build();
        operador = Usuario.builder().id(1L).email("op@test.com").build();
    }

    @Test
    void guardarLote_Success() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));
        when(usuarioRepository.findByRolNombreAndActivoAndComercioId("OPERADOR_INVENTARIO", 1, 1L))
                .thenReturn(Collections.singletonList(operador));
        
        LoteInventario loteGuardado = LoteInventario.builder()
                .id(1L)
                .producto(producto)
                .cantidadActual(100)
                .precioDinamico(new BigDecimal("15.0"))
                .estadoLote("DISPONIBLE")
                .build();
        
        when(loteRepository.save(any(LoteInventario.class))).thenReturn(loteGuardado);

        LoteResponseDTO response = loteService.guardarLote(requestDTO, 1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Producto Test", response.getNombreProducto());
        verify(loteRepository).save(any(LoteInventario.class));
        verify(loteEventProducer).publishLoteCreado(any(LoteInventario.class));
    }

    @Test
    void guardarLote_ProductoNotFound() {
        when(productoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> loteService.guardarLote(requestDTO, 1L));
    }

    @Test
    void guardarLote_OperadorNotFound() {
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));
        when(usuarioRepository.findByRolNombreAndActivoAndComercioId(anyString(), anyInt(), anyLong()))
                .thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> loteService.guardarLote(requestDTO, 1L));
    }

    @Test
    void listarTodos_Success() {
        LoteInventario lote = LoteInventario.builder()
                .id(1L)
                .producto(producto)
                .build();
        when(loteRepository.findAll()).thenReturn(Collections.singletonList(lote));

        List<LoteResponseDTO> result = loteService.listarTodos();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void listarPorComercio_Success() {
        LoteInventario lote = LoteInventario.builder()
                .id(1L)
                .producto(producto)
                .build();
        when(loteRepository.findByComercioId(1L)).thenReturn(Collections.singletonList(lote));

        List<LoteResponseDTO> result = loteService.listarPorComercio(1L);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }
}
