package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.AccessRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/* Repositorio para las restricciones de acceso */
public interface AccessRestrictionRepository extends JpaRepository<AccessRestriction, Long> {
    // Devolvemos la primera regla para el roleName dado
    Optional<AccessRestriction> findFirstByRoleName(String roleName);
}
