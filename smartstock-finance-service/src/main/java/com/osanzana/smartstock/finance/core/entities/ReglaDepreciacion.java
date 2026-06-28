package com.osanzana.smartstock.finance.core.entities;

import com.osanzana.smartstock.finance.core.entities.Usuario;
import com.osanzana.smartstock.finance.core.entities.Comercio;
import com.osanzana.smartstock.finance.core.entities.Categoria;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "reglas_depreciacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReglaDepreciacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @Column(name = "dias_criticos_min", nullable = false)
    private Integer diasCriticosMin;

    @Column(name = "porcentaje_descuento", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeDescuento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_gerente", nullable = false)
    private Usuario gerente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_comercio", nullable = false)
    private Comercio comercio;

    @Column(nullable = false)
    private Integer activa = 1;
}
