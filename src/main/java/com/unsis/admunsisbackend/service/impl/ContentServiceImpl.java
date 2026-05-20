package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.ContentDTO;
import com.unsis.admunsisbackend.dto.ContentPartDTO;
import com.unsis.admunsisbackend.model.Content;
import com.unsis.admunsisbackend.model.ContentPart;
import com.unsis.admunsisbackend.repository.ContentPartRepository;
import com.unsis.admunsisbackend.repository.ContentRepository;
import com.unsis.admunsisbackend.service.ContentService;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContentServiceImpl implements ContentService {

    private final ContentRepository contentRepo;
    private final ContentPartRepository partRepo;

    // 1. LLAVES EN MINÚSCULAS para coincidir con la normalización
    private static final Map<String, List<String>> ALLOWED_PART_KEYS_BY_CONTENT = Map.of(
            "mensaje_aceptado", List.of("greeting", "welcome_note", "inscription_dates", "survey", "documents_list", "note", "start_date", "contact"),
            "mensaje_reprobado", List.of("header", "body", "suggested_programs", "deadline_note", "contact")
    );

    private static final Safelist HTML_SAFELIST = Safelist.relaxed()
            .addTags("span")
            .addAttributes(":all", "style", "class")
            .preserveRelativeLinks(true);

    public ContentServiceImpl(ContentRepository contentRepo, ContentPartRepository partRepo) {
        this.contentRepo = contentRepo;
        this.partRepo = partRepo;
    }

    private ContentPartDTO partToDto(ContentPart p) {
        ContentPartDTO d = new ContentPartDTO();
        d.setId(p.getId());
        d.setPartKey(p.getPartKey());
        d.setTitle(p.getTitle());
        d.setHtmlContent(p.getHtmlContent());
        d.setOrderIndex(p.getOrderIndex());
        return d;
    }

    private ContentDTO toDto(Content c, List<ContentPart> parts) {
        ContentDTO d = new ContentDTO();
        d.setId(c.getId());
        d.setKeyName(c.getKeyName());
        d.setTitle(c.getTitle());
        d.setLanguage(c.getLanguage());
        d.setActive(c.isActive());
        d.setParts(parts.stream().map(this::partToDto).collect(Collectors.toList()));
        return d;
    }

    @Override
    @Transactional
    public ContentDTO getByKey(String keyName) {
        // 2. Normalizamos la entrada a minúsculas
        String normalizedKey = keyName.toLowerCase(); 
        
        List<ContentPart> parts = ensureAndGetPartsForContentKey(normalizedKey);
        
        Content c = contentRepo.findByKeyName(normalizedKey)
                .orElseThrow(() -> new RuntimeException("Content no encontrado: " + normalizedKey));
        return toDto(c, parts);
    }

    private List<ContentPart> ensureAndGetPartsForContentKey(String keyName) {
        // Ahora keyName ya viene en minúsculas desde getByKey
        List<String> allowedKeys = ALLOWED_PART_KEYS_BY_CONTENT.get(keyName);
        
        if (allowedKeys == null) {
            throw new IllegalArgumentException("Content key no permitida: " + keyName);
        }

        Content content = contentRepo.findByKeyName(keyName).orElseGet(() -> {
            Content nc = new Content();
            nc.setKeyName(keyName);
            nc.setLanguage("es");
            nc.setActive(true);
            return contentRepo.save(nc);
        });

        List<ContentPart> existingParts = partRepo.findByContentIdOrderByOrderIndex(content.getId());
        Map<String, ContentPart> existingMap = existingParts.stream()
                .collect(Collectors.toMap(ContentPart::getPartKey, p -> p));

        List<ContentPart> toSave = new ArrayList<>();
        for (int i = 0; i < allowedKeys.size(); i++) {
            String key = allowedKeys.get(i);
            ContentPart p = existingMap.get(key);
            
            if (p == null) {
                p = new ContentPart();
                p.setContent(content);
                p.setPartKey(key);
                p.setHtmlContent("");
                p.setOrderIndex(i);
                toSave.add(p);
            } else if (p.getOrderIndex() == null || p.getOrderIndex() != i) {
                p.setOrderIndex(i);
                toSave.add(p);
            }
        }

        if (!toSave.isEmpty()) {
            partRepo.saveAll(toSave);
            return partRepo.findByContentIdOrderByOrderIndex(content.getId());
        }

        return existingParts;
    }

    @Override
    @Transactional
    public ContentPartDTO upsertPart(String keyName, String partKey, ContentPartDTO dto) {
        String normalizedKey = keyName.toLowerCase();
        List<ContentPart> parts = ensureAndGetPartsForContentKey(normalizedKey);
        
        ContentPart part = parts.stream()
                .filter(p -> p.getPartKey().equals(partKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("PartKey no permitida: " + partKey));

        updatePartData(part, dto);
        return partToDto(partRepo.save(part));
    }

    @Override
    @Transactional
    public ContentDTO upsertParts(String keyName, List<ContentPartDTO> partsDto) {
        String normalizedKey = keyName.toLowerCase();
        List<ContentPart> existingParts = ensureAndGetPartsForContentKey(normalizedKey);
        
        Map<String, ContentPart> byKey = existingParts.stream()
                .collect(Collectors.toMap(ContentPart::getPartKey, p -> p));

        List<ContentPart> toUpdate = new ArrayList<>();
        for (ContentPartDTO dto : partsDto) {
            ContentPart p = byKey.get(dto.getPartKey());
            if (p != null) {
                updatePartData(p, dto);
                toUpdate.add(p);
            }
        }

        partRepo.saveAll(toUpdate);
        return getByKey(normalizedKey);
    }

    private void updatePartData(ContentPart entity, ContentPartDTO dto) {
        if (dto.getHtmlContent() != null) {
            entity.setHtmlContent(Jsoup.clean(dto.getHtmlContent(), HTML_SAFELIST));
        }
        if (dto.getTitle() != null) {
            entity.setTitle(dto.getTitle());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentDTO> listAll() {
        return contentRepo.findAll().stream()
                .map(c -> toDto(c, partRepo.findByContentIdOrderByOrderIndex(c.getId())))
                .collect(Collectors.toList());
    }
}