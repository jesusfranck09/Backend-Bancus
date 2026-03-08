package com.bancup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Refresh token persistido en base de datos.
 *
 * Solo se almacena el HASH SHA-256 del token raw (nunca el token en texto plano).
 * El token raw se entrega una sola vez al cliente en la respuesta del login.
 * Al hacer refresh, el cliente envia el raw token, se hashea y se busca en BD.
 *
 * Al hacer logout, el registro se elimina de BD invalidando el token.
 */
@Entity
@Table(name = "REFRESH_TOKENS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"usuario"})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    @EqualsAndHashCode.Include
    private Long id;

    /**
     * FK a USUARIOS. El usuario dueno del token.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private Usuario usuario;

    /**
     * Hash SHA-256 del token raw. Unico en la tabla.
     * 64 caracteres hexadecimales (256 bits).
     */
    @Column(name = "TOKEN_HASH", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * Fecha y hora de expiracion. Por defecto 7 dias desde creacion.
     */
    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Informacion del dispositivo o IP de origen. Utilizado para auditoria.
     */
    @Column(name = "DEVICE_INFO", length = 255)
    private String deviceInfo;
}
