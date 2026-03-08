package com.bancup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "USUARIOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"rol", "genero", "entidad"})
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
     * Identificador publico del usuario (UUID). No expone el PK interno.
     */
    @Column(name = "PUBLIC_ID", unique = true, nullable = false, length = 36)
    private String publicId;

    @Column(name = "EMAIL", unique = true, nullable = false, length = 255)
    private String email;

    /**
     * Hash BCrypt de la contrasena. Nunca se almacena en texto plano.
     */
    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Hash secundario. Reservado para uso futuro (ej. PIN, token interno).
     * Se inicializa en null en esta fase.
     */
    @Column(name = "PASSWORD_2HASH", length = 255)
    private String password2Hash;

    @Column(name = "TELEFONO", length = 20)
    private String telefono;

    @Column(name = "NOMBRE", nullable = false, length = 100)
    private String nombre;

    @Column(name = "APELLIDO_PATERNO", nullable = false, length = 100)
    private String apellidoPaterno;

    @Column(name = "APELLIDO_MATERNO", length = 100)
    private String apellidoMaterno;

    /**
     * CURP: unico cuando se proporciona. Oracle permite multiples NULLs en columna UNIQUE.
     */
    @Column(name = "CURP", unique = true, length = 18)
    private String curp;

    /**
     * Oracle DATE almacena fecha + hora, pero fecha_nacimiento es solo fecha.
     * Se mapea a LocalDate para preservar semantica de negocio.
     */
    @Column(name = "FECHA_NACIMIENTO")
    private LocalDate fechaNacimiento;

    /**
     * FK a CAT_GENERO. Opcional.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "GENERO_ID")
    private CatGenero genero;

    /**
     * FK a CAT_ESTADOS. Opcional.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ENTIDAD_ID")
    private CatEstado entidad;

    @Column(name = "FOTO_SELFIE_URL", length = 500)
    private String fotoSelfieUrl;

    @Column(name = "FOTO_INE_FRENTE_URL", length = 500)
    private String fotoIneFrenteUrl;

    @Column(name = "FOTO_INE_VUELTA_URL", length = 500)
    private String fotoIneVueltaUrl;

    /**
     * Estado KYC del usuario. Valor inicial: PENDIENTE.
     * Valores esperados: PENDIENTE, EN_REVISION, APROBADO, RECHAZADO.
     */
    @Column(name = "KYC_STATUS", nullable = false, length = 50)
    private String kycStatus;

    /**
     * Indicador biometrico. Valor inicial: 0 (no validado).
     * Mapeado como NUMBER en Oracle (0 = false, 1 = true).
     */
    @Column(name = "ES_BIOMETRIA_VALIDA", nullable = false)
    private Integer esBiometriaValida;

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
    @Column(name = "FAILED_LOGIN_ATTEMPTS", nullable = false)
    private Integer failedLoginAttempts;

    /**
     * Indica si la cuenta esta bloqueada por exceso de intentos fallidos.
     * Se desbloquea manualmente por soporte o administrador.
     * Oracle mapea Boolean a NUMBER(1): 0=false, 1=true.
     */
    @Column(name = "ACCOUNT_LOCKED", nullable = false)
    private Boolean accountLocked;
}
