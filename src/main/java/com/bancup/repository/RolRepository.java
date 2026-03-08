package com.bancup.repository;

import com.bancup.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Long> {

    /**
     * Busca un rol por su nombre de perfil exacto.
     * Usado para resolver el rol por defecto (ej. "CLIENTE").
     */
    Optional<Rol> findByNombrePerfil(String nombrePerfil);
}
