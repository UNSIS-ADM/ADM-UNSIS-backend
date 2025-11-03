package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/* Repositorio para los roles */
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

}
    