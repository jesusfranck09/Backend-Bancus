package com.bancup.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CAT_GENERO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
public class CatGenero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GENERO_ID")
    @EqualsAndHashCode.Include
    private Long generoId;

    @Column(name = "DESCRIPCION", length = 100)
    private String descripcion;

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
