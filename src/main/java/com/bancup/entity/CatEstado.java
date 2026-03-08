package com.bancup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CAT_ESTADOS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class CatEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ENTIDAD_ID")
    @EqualsAndHashCode.Include
    private Long entidadId;

    @Column(name = "CIUDAD", length = 200)
    private String ciudad;

    @Column(name = "ESTADO", length = 200)
    private String estado;

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
