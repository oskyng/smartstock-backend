package com.osanzana.smartstock.inventory.inventario.services;

import com.osanzana.smartstock.inventory.core.entities.Comercio;
import com.osanzana.smartstock.inventory.core.entities.Proveedor;
import com.osanzana.smartstock.inventory.core.repositories.ComercioRepository;
import com.osanzana.smartstock.inventory.core.repositories.ProveedorRepository;
import com.osanzana.smartstock.inventory.shared.dto.request.ProveedorRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.ProveedorResponseDTO;
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
class ProveedorServiceTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ComercioRepository comercioRepository;

    @InjectMocks
    private ProveedorService proveedorService;

    private Proveedor proveedor;
    private Comercio comercio;
    private ProveedorRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        comercio = Comercio.builder().id(1L).build();
        proveedor = Proveedor.builder()
                .id(1L)
                .rutEmpresa("76.111.111-1")
                .razonSocial("Proveedor Uno")
                .contactoEmail("contacto@proveedor.cl")
                .comercio(comercio)
                .build();
        requestDTO = ProveedorRequestDTO.builder()
                .rutEmpresa("76.111.111-1")
                .razonSocial("Proveedor Uno")
                .contactoEmail("contacto@proveedor.cl")
                .build();
    }

    @Test
    void listarPorComercio_Success() {
        when(proveedorRepository.findByComercioId(1L)).thenReturn(List.of(proveedor));
        List<ProveedorResponseDTO> result = proveedorService.listarPorComercio(1L);
        assertEquals(1, result.size());
        assertEquals("Proveedor Uno", result.get(0).getRazonSocial());
    }

    @Test
    void crear_Success() {
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));
        when(proveedorRepository.save(any(Proveedor.class))).thenReturn(proveedor);

        ProveedorResponseDTO result = proveedorService.crear(requestDTO, 1L);

        assertNotNull(result);
        assertEquals("Proveedor Uno", result.getRazonSocial());
        assertEquals("76.111.111-1", result.getRutEmpresa());
        verify(proveedorRepository).save(any(Proveedor.class));
    }

    @Test
    void crear_ComercioNotFound() {
        when(comercioRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> proveedorService.crear(requestDTO, 1L));
    }
}
