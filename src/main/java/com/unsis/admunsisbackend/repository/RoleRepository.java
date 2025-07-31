package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;


public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    //Cuenta el número de roles por carrera y año de admisión
    long countByCareerAndAdmissionYear(String career, Integer year);

}
    