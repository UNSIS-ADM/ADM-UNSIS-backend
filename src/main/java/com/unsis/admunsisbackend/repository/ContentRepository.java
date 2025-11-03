package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/* Repositorio para los contenidos */
public interface ContentRepository extends JpaRepository<Content, Long> {
    Optional<Content> findByKeyName(String keyName);
}

