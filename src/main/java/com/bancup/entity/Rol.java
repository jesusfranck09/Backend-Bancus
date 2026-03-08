package com.bancup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ROLES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_ROL")
    @EqualsAndHashCode.Include
    private Long idRol;

    @Column(name = "NOMBRE_PERFIL", nullable = false, length = 100)
    private String nombrePerfil;

    @Column(name = "DESC_PERFIL", length = 255)
    private String descPerfil;

    @Column(name = "ESTATUS", length = 20)
    private String estatus;

    @Column(name = "FECHA_CREA")
    private LocalDateTime fechaCrea;

    @Column(name = "USUARIO_CREA", length = 100)
    private String usuarioCrea;

    @Column(name = "FECHA_MODIFICA")
    private LocalDateTime fechaModifica;

    @Column(name = "USUARIO_MODIFICA", length = 100)
    private String usuarioModifica;
}
