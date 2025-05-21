package com.admunisis.service;

import com.admunisis.model.Aspirante;
import com.admunisis.repository.AspiranteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AspiranteService {
    @Autowired
    private AspiranteRepository aspiranteRepository;
    
    public Aspirante guardarAspirante(Aspirante aspirante) {
        return aspiranteRepository.save(aspirante);
    }
    
    // Otros métodos de servicio
}
