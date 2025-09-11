package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.model.RefreshToken;
import com.unsis.admunsisbackend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // << importar

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // Crear un refresh token nuevo (elimina previos para el user)
    @Transactional // << asegura que delete + save estén en una transacción
    public RefreshToken createRefreshToken(Long userId, long durationMs) {
        // eliminar refresh tokens anteriores del usuario si quieres un sólo token
        // activo
        deleteByUserId(userId);

        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiryDate(new Date(System.currentTimeMillis() + durationMs));
        return refreshTokenRepository.save(rt);
    }

    // Buscar por token
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // Validar si sigue vigente
    public boolean isValid(RefreshToken rt) {
        return rt != null && rt.getExpiryDate() != null && rt.getExpiryDate().after(new Date());
    }

    // Borrar refresh tokens por userId — también transaccional
    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
