package com.osanzana.smartstock.auth.core.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "codigos_recuperacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodigoRecuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_codigo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "codigo_verificador", nullable = false, length = 6)
    private String codigoVerificador;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    /** 'N' = no utilizado, 'S' = utilizado (CHECK utilizado IN ('S','N') en la BD). */
    @Column(nullable = false, length = 1)
    private String utilizado;
}
