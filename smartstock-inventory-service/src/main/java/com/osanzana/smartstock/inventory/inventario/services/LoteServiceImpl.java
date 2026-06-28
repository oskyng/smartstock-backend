package com.osanzana.smartstock.inventory.inventario.services;

import com.osanzana.smartstock.inventory.core.entities.Usuario;
import com.osanzana.smartstock.inventory.core.repositories.UsuarioRepository;
import com.osanzana.smartstock.inventory.core.entities.Comercio;
import com.osanzana.smartstock.inventory.core.repositories.ComercioRepository;
import com.osanzana.smartstock.inventory.core.entities.LoteInventario;
import com.osanzana.smartstock.inventory.core.entities.Producto;
import com.osanzana.smartstock.inventory.core.entities.Proveedor;
import com.osanzana.smartstock.inventory.inventario.events.LoteEventProducer;
import com.osanzana.smartstock.inventory.core.repositories.LoteRepository;
import com.osanzana.smartstock.inventory.core.repositories.ProductoRepository;
import com.osanzana.smartstock.inventory.core.repositories.ProveedorRepository;
import com.osanzana.smartstock.inventory.shared.dto.request.LoteRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.LoteResponseDTO;
import com.osanzana.smartstock.inventory.shared.exception.BusinessException;
import com.osanzana.smartstock.inventory.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoteServiceImpl implements LoteService {

    private final LoteRepository loteRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final ComercioRepository comercioRepository;
    private final UsuarioRepository usuarioRepository;
    private final LoteEventProducer loteEventProducer;

    @Override
    @Transactional
    public LoteResponseDTO guardarLote(LoteRequestDTO dto, Long comercioId) {
        log.info("Guardando lote para producto {} y comercio {}", dto.getIdProducto(), comercioId);

        Producto producto = productoRepository.findById(dto.getIdProducto())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        Proveedor proveedor = proveedorRepository.findById(dto.getIdProveedor())
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));

        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado"));

        // Buscar un usuario operador por defecto para el comercio (para simplificar)
        Usuario operador = usuarioRepository.findByRolNombreAndActivoAndComercioId("OPERADOR_INVENTARIO", 1, comercioId)
                .stream().findFirst()
                .orElseThrow(() -> new BusinessException("No se encontró un OPERADOR_INVENTARIO activo para el comercio ID: " + comercioId));

        LoteInventario lote = LoteInventario.builder()
                .producto(producto)
                .proveedor(proveedor)
                .comercio(comercio)
                .operador(operador)
                .cantidadInicial(dto.getCantidadInicial())
                .cantidadActual(dto.getCantidadInicial())
                .costoUnitario(dto.getCostoUnitario())
                .precioDinamico(dto.getPrecioDinamico())
                .fechaVencimiento(dto.getFechaVencimiento())
                .estadoLote("DISPONIBLE")
                .build();

        LoteInventario nuevoLote = loteRepository.save(lote);
        
        loteEventProducer.publishLoteCreado(nuevoLote);
        
        return mapToResponseDTO(nuevoLote);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteResponseDTO> listarTodos() {
        log.info("Listando todos los lotes");
        return loteRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteResponseDTO> listarPorComercio(Long comercioId) {
        log.info("Listando lotes para el comercio {}", comercioId);
        return loteRepository.findByComercioId(comercioId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private LoteResponseDTO mapToResponseDTO(LoteInventario lote) {
        return LoteResponseDTO.builder()
                .id(lote.getId())
                .nombreProducto(lote.getProducto().getNombre())
                .cantidadActual(lote.getCantidadActual())
                .precioDinamico(lote.getPrecioDinamico())
                .fechaVencimiento(lote.getFechaVencimiento())
                .estadoLote(lote.getEstadoLote())
                .fechaRecepcion(lote.getFechaRecepcion())
                .build();
    }
}
