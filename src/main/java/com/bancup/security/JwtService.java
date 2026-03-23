package com.bancup.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * Servicio para generar y validar JSON Web Tokens (JWT).
 *
 * Algoritmo: HS256.
 * Claims incluidos: subject (email), userId (id interno), email, role.
 * Expiracion: configurable en application.properties (por defecto 24h).
 */
@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret:${bancup.jwt.secret:}}")
    private String secret;

    @Value("${jwt.expiration:${bancup.jwt.expiration-ms:86400000}}")
    private long expirationMs;

    /**
     * Genera un JWT firmado con HS256.
     *
     * @param userId    Identificador del usuario
     * @param email     Email del usuario (subject del token)
     * @param role      Nombre del perfil/rol asignado
     * @return          Token JWT compacto listo para enviar al cliente
     */
    public String generateToken(String userId, String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("email", email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Valida que el token sea autentico, no manipulado y no expirado.
     *
     * @param token JWT a validar
     * @return true si es valido, false en cualquier otro caso
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT invalido o expirado: {}", e.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractUserId(String token) {
        return extractAllClaims(token).get("userId", String.class);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No fue posible inicializar la clave JWT", e);
        }
    }
}
