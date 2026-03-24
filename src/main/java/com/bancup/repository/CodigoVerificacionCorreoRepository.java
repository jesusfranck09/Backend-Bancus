package com.bancup.repository;

import com.bancup.entity.CodigoVerificacionCorreo;
import com.bancup.entity.EstatusCodigoVerificacion;
import com.bancup.entity.TipoCodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodigoVerificacionCorreoRepository extends JpaRepository<CodigoVerificacionCorreo, Long> {

    Optional<CodigoVerificacionCorreo> findTopByCorreoAndTipoOrderByFechaCreaDesc(
            String correo,
            TipoCodigoVerificacion tipo
    );

    Optional<CodigoVerificacionCorreo> findTopByCorreoAndTipoAndEstatusOrderByFechaCreaDesc(
            String correo,
            TipoCodigoVerificacion tipo,
            EstatusCodigoVerificacion estatus
    );

    List<CodigoVerificacionCorreo> findByCorreoAndTipoAndEstatusIn(
            String correo,
            TipoCodigoVerificacion tipo,
            Collection<EstatusCodigoVerificacion> estatuses
    );
}
