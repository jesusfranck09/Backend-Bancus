package com.bancup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CODIGOS_VERIFICACION_CORREO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CodigoVerificacionCorreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CODIGO")
    @EqualsAndHashCode.Include
    private Long idCodigo;

    @Column(name = "CORREO", nullable = false, length = 255)
    private String correo;

    @Column(name = "CODIGO_HASH", nullable = false, length = 255)
    private String codigoHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 30)
    private TipoCodigoVerificacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTATUS", nullable = false, length = 20)
    private EstatusCodigoVerificacion estatus;

    @Column(name = "INTENTOS_FALLIDOS", nullable = false)
    private Integer intentosFallidos;

    @Column(name = "REENVIOS", nullable = false)
    private Integer reenvios;

    @Column(name = "FECHA_EXPIRACION", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "FECHA_VERIFICADO")
    private LocalDateTime fechaVerificado;

    @Column(name = "IP_ORIGEN", length = 45)
    private String ipOrigen;

    @Column(name = "FECHA_CREA", nullable = false)
    private LocalDateTime fechaCrea;

    @Column(name = "USUARIO_CREA", nullable = false, length = 64)
    private String usuarioCrea;

    @Column(name = "FECHA_MODIFICA")
    private LocalDateTime fechaModifica;

    @Column(name = "USUARIO_MODIFICA", length = 64)
    private String usuarioModifica;
}
