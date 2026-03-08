package com.bancup.repository;

import com.bancup.entity.RefreshToken;
import com.bancup.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Busca un refresh token por su hash SHA-256.
     * Se usa al validar el token enviado por el cliente en /auth/refresh.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Elimina todos los refresh tokens de un usuario.
     * Se usa al bloquear cuenta o en logout global (todas las sesiones).
     */
    void deleteByUsuario(Usuario usuario);
}
