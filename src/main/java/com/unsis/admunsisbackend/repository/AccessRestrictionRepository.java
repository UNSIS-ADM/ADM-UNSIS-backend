package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.AccessRestriction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AccessRestrictionRepository extends JpaRepository<AccessRestriction, Long> {
    // devolvemos la primera regla para el roleName (solo tienes una regla ROLE_APPLICANT)
    Optional<AccessRestriction> findFirstByRoleName(String roleName);
}
