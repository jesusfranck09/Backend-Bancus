package com.bancup.repository;

import com.bancup.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Valida unicidad de email antes del registro.
     */
    boolean existsByEmail(String email);

    /**
     * Valida unicidad de CURP antes del registro.
     */
    boolean existsByCurp(String curp);

    /**
     * Busqueda por email para autenticacion.
     * Preparado para LOGIN (fase 2) y para UserDetailsService de Spring Security.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Busqueda por publicId (identificador expuesto al exterior).
     */
    Optional<Usuario> findByPublicId(String publicId);
}
