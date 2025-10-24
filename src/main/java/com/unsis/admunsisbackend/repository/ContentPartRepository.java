package com.unsis.admunsisbackend.repository;

import com.unsis.admunsisbackend.model.ContentPart;
import com.unsis.admunsisbackend.model.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface ContentPartRepository extends JpaRepository<ContentPart, Long> {
    // Buscar por contentId (usado en service)
    List<ContentPart> findByContentIdOrderByOrderIndex(Long contentId);

    // Buscar por content (entidad)
    List<ContentPart> findByContent(Content content);
    
    //Optional<ContentPart> findByContentKeyNameAndPartKey(String keyName, String partKey);

    // Buscar por content.keyName y partKey (por si lo necesitas)
    @Query("SELECT cp FROM ContentPart cp JOIN cp.content c WHERE c.keyName = :keyName AND cp.partKey = :partKey")
    Optional<ContentPart> findByContentKeyNameAndPartKey(@Param("keyName") String keyName, @Param("partKey") String partKey);
}

