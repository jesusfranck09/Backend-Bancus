package com.bancup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "CUENTAS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"usuario"})
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_CUENTA")
    @EqualsAndHashCode.Include
    private Long idCuenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO", nullable = false)
    private Usuario usuario;

    /**
     * Saldo de la cuenta. Se usa BigDecimal para precision financiera.
     * Inicializado en 0 durante el signup.
     */
    @Column(name = "SALDO", nullable = false, precision = 19, scale = 4)
    private BigDecimal saldo;

    /**
     * Tipo de cuenta. Valor inicial configurado en application.properties: AHORRO.
     */
    @Column(name = "TIPO_CUENTA", nullable = false, length = 50)
    private String tipoCuenta;

    @Column(name = "FECHA_CREACION")
    private LocalDateTime fechaCreacion;
}
