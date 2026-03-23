package com.bancup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "USUARIOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"rol", "genero"})
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_USUARIO")
    @EqualsAndHashCode.Include
    private Long idUsuario;

    /**
     * FK a ROLES. Siempre requerido.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_ROL", nullable = false)
    private Rol rol;

    /**
     * FK a CAT_GENERO. Requerido en el esquema actual.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GENERO_ID", nullable = false)
    private CatGenero genero;

    @Column(name = "NOMBRE_USUARIO", nullable = false, length = 150)
    private String nombre;

    @Column(name = "CORREO", unique = true, nullable = false, length = 255)
    private String email;

    /**
     * Hash BCrypt de la contrasena. Nunca se almacena en texto plano.
     */
    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Ultimo login del usuario. Se actualiza en el flujo de LOGIN (fase 2).
     * Se inicializa en null.
     */
    @Column(name = "ULTIMO_LOGIN")
    private LocalDateTime ultimoLogin;

    @Column(name = "FECHA_REGISTRO")
    private LocalDateTime fechaRegistro;

    /**
     * Contador de intentos de login fallidos consecutivos.
     * Se resetea a 0 en cada login exitoso.
     * Cuando llega a 5, se activa accountLocked.
     */
    @Column(name = "INTENTOS_FALLIDOS", nullable = false)
    private Integer failedLoginAttempts;

    /**
     * Indica si la cuenta esta bloqueada por exceso de intentos fallidos.
     * Se desbloquea manualmente por soporte o administrador.
     * Oracle mapea Boolean a NUMBER(1): 0=false, 1=true.
     */
    @Column(name = "CUENTA_BLOQUEADA", nullable = false)
    private Boolean accountLocked;

    @Column(name = "ESTATUS", nullable = false)
    private Integer estatus;

    @Column(name = "FECHA_CREA", nullable = false)
    private LocalDateTime fechaCrea;

    @Column(name = "USUARIO_CREA", nullable = false, length = 64)
    private String usuarioCrea;

    @Column(name = "FECHA_MODIFICA")
    private LocalDateTime fechaModifica;

    @Column(name = "USUARIO_MODIFICA", length = 64)
    private String usuarioModifica;
}
