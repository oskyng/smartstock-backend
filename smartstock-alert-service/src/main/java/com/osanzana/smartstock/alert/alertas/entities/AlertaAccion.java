package com.osanzana.smartstock.alert.alertas.entities;

import com.osanzana.smartstock.alert.core.entities.Usuario;
import com.osanzana.smartstock.alert.core.entities.Comercio;
import com.osanzana.smartstock.alert.core.entities.ReglaDepreciacion;
import com.osanzana.smartstock.alert.core.entities.LoteInventario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "alertas_accion")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaAccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alerta")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_lote", nullable = false)
    private LoteInventario lote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_regla_aplicada")
    private ReglaDepreciacion reglaAplicada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_comercio", nullable = false)
    private Comercio comercio;

    @Column(name = "descripcion_alerta", nullable = false, length = 255)
    private String descripcionAlerta;

    @Column(name = "estado_alerta", nullable = false, length = 30)
    private String estadoAlerta = "PENDIENTE";

    @Column(name = "fecha_notificacion", nullable = false)
    private LocalDateTime fechaNotificacion;

    @Column(name = "fecha_limite_atencion", nullable = false)
    private LocalDateTime fechaLimiteAtencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_asignado", nullable = false)
    private Usuario usuarioAsignado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_supervisor", nullable = false)
    private Usuario usuarioSupervisor;

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;

    @PrePersist
    protected void onCreate() {
        if (fechaNotificacion == null) {
            fechaNotificacion = LocalDateTime.now();
        }
    }
}
