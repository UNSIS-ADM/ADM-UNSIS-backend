package com.unsis.admunsisbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.unsis.admunsisbackend.model.AccessRestriction;

import java.util.List;

public interface AccessRestrictionRepository extends JpaRepository<AccessRestriction, Long> {
    List<AccessRestriction > findByRoleNameAndEnabledTrue(String roleName);
}
