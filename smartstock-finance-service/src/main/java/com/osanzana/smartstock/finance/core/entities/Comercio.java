package com.osanzana.smartstock.finance.core.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "comercios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comercio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comercio")
    private Long id;

    @Column(name = "rut_empresa", nullable = false, unique = true, length = 12)
    private String rutEmpresa;

    @Column(name = "razon_social", nullable = false, length = 100)
    private String razonSocial;

    @Column(length = 50)
    private String rubro;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Column(nullable = false, length = 15)
    private String estado = "ACTIVO";

    @PrePersist
    protected void onCreate() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDate.now();
        }
    }
}
