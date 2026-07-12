package com.osanzana.smartstock.commerce.gestion.services;

import com.osanzana.smartstock.commerce.core.entities.Comercio;
import com.osanzana.smartstock.commerce.core.repositories.ComercioRepository;
import com.osanzana.smartstock.commerce.shared.dto.request.ComercioRequestDTO;
import com.osanzana.smartstock.commerce.shared.dto.response.ComercioResponseDTO;
import com.osanzana.smartstock.commerce.shared.exception.ConflictException;
import com.osanzana.smartstock.commerce.shared.exception.ResourceNotFoundException;
import com.osanzana.smartstock.commerce.shared.exception.UnauthorizedActionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComercioServiceTest {

    @Mock
    private ComercioRepository comercioRepository;

    @InjectMocks
    private ComercioService comercioService;

    private Comercio comercio;
    private ComercioRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        comercio = Comercio.builder()
                .id(1L)
                .rutEmpresa("12.345.678-9")
                .razonSocial("Tienda Test")
                .rubro("Retail")
                .fechaRegistro(LocalDate.now())
                .estado("ACTIVO")
                .build();

        requestDTO = ComercioRequestDTO.builder()
                .rutEmpresa("12.345.678-9")
                .razonSocial("Tienda Test")
                .rubro("Retail")
                .build();
    }

    @Test
    void crearComercio_Success() {
        when(comercioRepository.findByRutEmpresa(anyString())).thenReturn(Optional.empty());
        when(comercioRepository.save(any(Comercio.class))).thenReturn(comercio);

        ComercioResponseDTO response = comercioService.crearComercio(requestDTO);

        assertNotNull(response);
        assertEquals(comercio.getRutEmpresa(), response.getRutEmpresa());
        verify(comercioRepository).save(any(Comercio.class));
    }

    @Test
    void crearComercio_Conflict() {
        when(comercioRepository.findByRutEmpresa(anyString())).thenReturn(Optional.of(comercio));

        assertThrows(ConflictException.class, () -> comercioService.crearComercio(requestDTO));
    }

    @Test
    void listarComercios_Success() {
        when(comercioRepository.findAll()).thenReturn(Collections.singletonList(comercio));

        List<ComercioResponseDTO> response = comercioService.listarComercios();

        assertFalse(response.isEmpty());
        assertEquals(1, response.size());
    }

    @Test
    void updateComercio_Admin_Success() {
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));
        when(comercioRepository.save(any(Comercio.class))).thenReturn(comercio);

        ComercioResponseDTO response = comercioService.updateComercio(1L, requestDTO, "ROLE_ADMIN_SISTEMA", null);

        assertNotNull(response);
        verify(comercioRepository).save(any(Comercio.class));
    }

    @Test
    void updateComercio_Gerente_Success() {
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));
        when(comercioRepository.save(any(Comercio.class))).thenReturn(comercio);

        ComercioResponseDTO response = comercioService.updateComercio(1L, requestDTO, "ROLE_GERENTE_TIENDA", 1L);

        assertNotNull(response);
    }

    @Test
    void updateComercio_Gerente_Unauthorized() {
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));

        assertThrows(UnauthorizedActionException.class, () -> 
            comercioService.updateComercio(1L, requestDTO, "ROLE_GERENTE_TIENDA", 2L));
    }

    @Test
    void updateComercio_AdminRole_Forbidden() {
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));

        assertThrows(UnauthorizedActionException.class, () -> 
            comercioService.updateComercio(1L, requestDTO, "ROLE_ADMIN", 2L));
    }

    @Test
    void deleteComercio_Success() {
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));

        comercioService.deleteComercio(1L);

        verify(comercioRepository).delete(comercio);
    }

    @Test
    void obtenerPorId_Success() {
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));

        ComercioResponseDTO response = comercioService.obtenerPorId(1L, "ROLE_ADMIN_SISTEMA", null);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void obtenerPorId_NotFound() {
        when(comercioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> comercioService.obtenerPorId(1L, "ROLE_ADMIN_SISTEMA", null));
    }
}
