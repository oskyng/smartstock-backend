package com.osanzana.smartstock.finance.finanzas.services;

import com.osanzana.smartstock.finance.core.entities.Categoria;
import com.osanzana.smartstock.finance.core.entities.Comercio;
import com.osanzana.smartstock.finance.core.entities.ReglaDepreciacion;
import com.osanzana.smartstock.finance.core.entities.Usuario;
import com.osanzana.smartstock.finance.core.repositories.CategoriaRepository;
import com.osanzana.smartstock.finance.core.repositories.ComercioRepository;
import com.osanzana.smartstock.finance.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.finance.finanzas.repositories.ReglaDepreciacionRepository;
import com.osanzana.smartstock.finance.shared.dto.request.ReglaDepreciacionRequestDTO;
import com.osanzana.smartstock.finance.shared.dto.response.ReglaDepreciacionResponseDTO;
import com.osanzana.smartstock.finance.shared.exception.ResourceNotFoundException;
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
class ReglaDepreciacionServiceTest {

    @Mock
    private ReglaDepreciacionRepository repository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ComercioRepository comercioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ReglaDepreciacionService service;

    private ReglaDepreciacion regla;
    private ReglaDepreciacionRequestDTO requestDTO;
    private Categoria categoria;
    private Comercio comercio;
    private Usuario gerente;

    @BeforeEach
    void setUp() {
        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Test");

        comercio = Comercio.builder().id(1L).build();
        gerente = Usuario.builder().id(1L).nombre("Gerente").apellido("Admin").build();
        
        regla = ReglaDepreciacion.builder()
                .id(1L)
                .categoria(categoria)
                .diasCriticosMin(5)
                .porcentajeDescuento(new BigDecimal("20.00"))
                .activa(1)
                .comercio(comercio)
                .gerente(gerente)
                .build();

        requestDTO = ReglaDepreciacionRequestDTO.builder()
                .idCategoria(1L)
                .diasCriticosMin(5)
                .porcentajeDescuento(new BigDecimal("20.00"))
                .build();
    }

    @Test
    void listarPorComercio_Success() {
        when(repository.findByComercioId(1L)).thenReturn(List.of(regla));
        List<ReglaDepreciacionResponseDTO> result = service.listarPorComercio(1L);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
    }

    @Test
    void guardar_Success() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));
        when(usuarioRepository.findByEmail("gerente@test.cl")).thenReturn(Optional.of(gerente));
        when(repository.save(any(ReglaDepreciacion.class))).thenReturn(regla);
        
        ReglaDepreciacionResponseDTO result = service.guardar(requestDTO, 1L, "gerente@test.cl");
        assertNotNull(result);
    }

    @Test
    void eliminar_Success() {
        when(repository.findById(1L)).thenReturn(Optional.of(regla));
        service.eliminar(1L, 1L);
        verify(repository).delete(regla);
    }

    @Test
    void eliminar_NotFound() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(1L, 1L));
    }

    @Test
    void eliminar_DeOtroComercio_LanzaNotFound() {
        // La regla existe pero pertenece a otro comercio: debe comportarse como si no existiera,
        // para no confirmarle a un atacante que el ID pertenece a otro tenant (IDOR).
        when(repository.findById(1L)).thenReturn(Optional.of(regla));
        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(1L, 99L));
        verify(repository, never()).delete(any(ReglaDepreciacion.class));
    }

    @Test
    void guardar_CategoriaNotFound() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.guardar(requestDTO, 1L, "gerente@test.cl"));
    }

    @Test
    void guardar_ComercioNotFound() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(comercioRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.guardar(requestDTO, 1L, "gerente@test.cl"));
    }

    @Test
    void guardar_GerenteNotFound() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(comercioRepository.findById(1L)).thenReturn(Optional.of(comercio));
        when(usuarioRepository.findByEmail("gerente@test.cl")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.guardar(requestDTO, 1L, "gerente@test.cl"));
    }
}
