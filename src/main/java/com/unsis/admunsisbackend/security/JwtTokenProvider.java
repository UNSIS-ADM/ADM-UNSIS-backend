package com.unsis.admunsisbackend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Proveedor de tokens JWT que maneja la generación, validación y extracción de
 * información de los tokens JWT.
 */
@Component
public class JwtTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private int jwtExpirationInMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Genera un token JWT para el usuario especificado.
     *
     * @param username El nombre de usuario del usuario.
     * @return El token JWT generado.
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        String token = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        logger.info("Token JWT generado para el usuario: " + username);
        return token;
    }

    /**
     * Extrae el nombre de usuario del token JWT.
     *
     * @param token El token JWT.
     * @return El nombre de usuario extraído del token.
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        String username = claims.getSubject();
        logger.info("Usuario extraído del token: " + username);
        return username;
    }

    /**
     * Valida el token JWT.
     *
     * @param token El token JWT a validar.
     * @return true si el token es válido, false en caso contrario.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            logger.info("Token JWT validado exitosamente");
            return true;
        } catch (SecurityException ex) {
            logger.error("Firma JWT inválida: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Token JWT malformado: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Token JWT expirado: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Token JWT no soportado: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string está vacío: {}", ex.getMessage());
        }
        return false;
    }
    
    /**
     * Extrae todos los claims (información contenida dentro de JWT) del token JWT.
     *
     * @param token El token JWT.
     * @return Los claims extraídos del token.
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody(); // esto lanzará ExpiredJwtException si expiró
    }

    /**
     * Obtiene el tiempo de expiración del token JWT en milisegundos.
     * 
     * @return El tiempo de expiración en milisegundos.
     */
    public int getJwtExpirationInMs() {
        return jwtExpirationInMs;
    }

}
