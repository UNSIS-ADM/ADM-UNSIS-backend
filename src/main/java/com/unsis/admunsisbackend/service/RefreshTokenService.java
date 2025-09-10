package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.model.RefreshToken;
import com.unsis.admunsisbackend.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // Crear un refresh token nuevo
    public RefreshToken createRefreshToken(Long userId, long durationMs) {
        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setToken(UUID.randomUUID().toString());
        rt.setExpiryDate(new Date(System.currentTimeMillis() + durationMs));
        return refreshTokenRepository.save(rt);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean isValid(RefreshToken rt) {
        return rt.getExpiryDate().after(new Date());
    }

    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
}
