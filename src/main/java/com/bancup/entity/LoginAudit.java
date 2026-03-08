package com.bancup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de auditoria de cada login exitoso.
 *
 * Captura: quien, desde donde y cuando.
 * Se crea en cada login exitoso independientemente de AuditoriaEvento.
 * Permite analisis de patrones de acceso y deteccion de anomalias.
 */
@Entity
@Table(name = "LOGIN_AUDIT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"usuario"})
public class LoginAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * FK a USUARIOS. El usuario que realizo el login.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private Usuario usuario;

    /**
     * Direccion IP desde donde se realizo el login.
     */
    @Column(name = "IP_ADDRESS", length = 50)
    private String ipAddress;

    /**
     * Informacion del dispositivo: User-Agent, tipo, etc.
     */
    @Column(name = "DEVICE_INFO", length = 255)
    private String deviceInfo;

    /**
     * Fecha y hora exacta del login exitoso (UTC).
     */
    @Column(name = "LOGIN_TIME", nullable = false)
    private LocalDateTime loginTime;
}
