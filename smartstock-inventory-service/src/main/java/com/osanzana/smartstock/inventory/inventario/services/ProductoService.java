package com.osanzana.smartstock.inventory.inventario.services;

import com.osanzana.smartstock.inventory.core.entities.Categoria;
import com.osanzana.smartstock.inventory.core.entities.Producto;
import com.osanzana.smartstock.inventory.core.entities.Comercio;
import com.osanzana.smartstock.inventory.core.repositories.ComercioRepository;
import com.osanzana.smartstock.inventory.core.repositories.CategoriaRepository;
import com.osanzana.smartstock.inventory.core.repositories.ProductoRepository;
import com.osanzana.smartstock.inventory.shared.dto.request.ProductoRequestDTO;
import com.osanzana.smartstock.inventory.shared.dto.response.ProductoResponseDTO;
import com.osanzana.smartstock.inventory.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ComercioRepository comercioRepository;

    public List<ProductoResponseDTO> listarTodos() {
        return productoRepository.findAll().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ProductoResponseDTO> listarPorComercio(Long comercioId) {
        return productoRepository.findByComercioId(comercioId).stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    public ProductoResponseDTO obtenerPorId(Long id, Long comercioId) {
        Producto producto = productoRepository.findById(id)
                .filter(p -> p.getComercio().getId().equals(comercioId))
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
        return mapToResponseDTO(producto);
    }

    @Transactional
    public ProductoResponseDTO guardar(ProductoRequestDTO dto, Long comercioId) {
        Categoria categoria = categoriaRepository.findById(dto.getIdCategoria())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada"));
        
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio no encontrado"));

        Producto producto = Producto.builder()
                .codigoBarra(dto.getCodigoBarra())
                .nombre(dto.getNombre())
                .categoria(categoria)
                .precioBase(dto.getPrecioBase())
                .comercio(comercio)
                .build();

        return mapToResponseDTO(productoRepository.save(producto));
    }

    private ProductoResponseDTO mapToResponseDTO(Producto producto) {
        return ProductoResponseDTO.builder()
                .id(producto.getId())
                .codigoBarra(producto.getCodigoBarra())
                .nombre(producto.getNombre())
                .nombreCategoria(producto.getCategoria().getNombre())
                .precioBase(producto.getPrecioBase())
                .build();
    }
}
