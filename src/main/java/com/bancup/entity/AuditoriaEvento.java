package com.bancup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUDITORIA_EVENTOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"usuario"})
public class AuditoriaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_EVENTO")
    @EqualsAndHashCode.Include
    private Long idEvento;

    /**
     * FK a USUARIOS. Puede ser null en eventos pre-autenticacion (ej. intentos fallidos).
     * En signup se asigna el usuario recien creado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO")
    private Usuario usuario;

    /**
     * Accion registrada. Valores usados: SIGNUP, LOGIN, LOGOUT, etc.
     */
    @Column(name = "ACCION", nullable = false, length = 100)
    private String accion;

    @Column(name = "IP_ORIGEN", length = 50)
    private String ipOrigen;

    @Column(name = "FECHA_EVENTO")
    private LocalDateTime fechaEvento;
}
