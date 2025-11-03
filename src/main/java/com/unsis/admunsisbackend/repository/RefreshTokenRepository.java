package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/* Repositorio para los tokens de refresco */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    // Buscar por token
    Optional<RefreshToken> findByToken(String token);
    // Eliminar por userId
    void deleteByUserId(Long userId);
}
