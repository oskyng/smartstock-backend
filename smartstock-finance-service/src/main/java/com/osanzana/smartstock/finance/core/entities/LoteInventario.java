package com.osanzana.smartstock.finance.core.entities;

import com.osanzana.smartstock.finance.core.entities.Usuario;
import com.osanzana.smartstock.finance.core.entities.Comercio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "lotes_inventario")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lote")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "cantidad_inicial", nullable = false)
    private Integer cantidadInicial;

    @Column(name = "cantidad_actual", nullable = false)
    private Integer cantidadActual;

    @Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    @Column(name = "precio_dinamico", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioDinamico;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDate fechaRecepcion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_operador", nullable = false)
    private Usuario operador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_comercio", nullable = false)
    private Comercio comercio;

    @Column(name = "estado_lote", nullable = false, length = 25)
    private String estadoLote = "DISPONIBLE";

    @PrePersist
    protected void onCreate() {
        if (fechaRecepcion == null) {
            fechaRecepcion = LocalDate.now();
        }
    }
}
