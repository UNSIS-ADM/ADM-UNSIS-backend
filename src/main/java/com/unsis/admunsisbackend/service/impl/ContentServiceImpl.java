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

    // define las keys permitidas y sus partes (inmutables)
    private static final Map<String, List<String>> ALLOWED_PART_KEYS_BY_CONTENT = Map.of(
            "mensaje_aceptado_2025",
            List.of("greeting", "welcome_note", "inscription_dates", "survey", "documents_list", "note", "start_date",
                    "contact"),
            "mensaje_reprobado_2025", List.of("header", "body", "suggested_programs", "deadline_note", "contact"));

    // Crear una constante para el Safelist configurado
    private static final Safelist HTML_SAFELIST = Safelist.relaxed()
            .addTags("span")
            .addAttributes(":all", "style")  // Permite style en todos los elementos
            .addAttributes(":all", "class")  // Permite class en todos los elementos
            .preserveRelativeLinks(true);    // Mantiene links relativos

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
    @Transactional(readOnly = true)
    public ContentDTO getByKey(String keyName) {
        // ensure content + parts ready
        List<ContentPart> parts = ensureAndGetPartsForContentKey(keyName);
        Content c = contentRepo.findByKeyName(keyName)
                .orElseThrow(() -> new RuntimeException("Content no encontrado: " + keyName));
        return toDto(c, parts);
    }

    @Transactional
    private List<ContentPart> ensureAndGetPartsForContentKey(String keyName) {
        List<String> allowed = ALLOWED_PART_KEYS_BY_CONTENT.get(keyName);
        if (allowed == null)
            throw new IllegalArgumentException("Content key no permitida: " + keyName);

        Content content = contentRepo.findByKeyName(keyName).orElseGet(() -> {
            Content nc = new Content();
            nc.setKeyName(keyName);
            nc.setLanguage("es");
            nc.setActive(true);
            return contentRepo.save(nc);
        });

        List<ContentPart> existing = partRepo.findByContentIdOrderByOrderIndex(content.getId());
        Map<String, ContentPart> byKey = existing.stream().collect(Collectors.toMap(ContentPart::getPartKey, p -> p));

        List<ContentPart> result = new ArrayList<>();
        int idx = 0;
        for (String pk : allowed) {
            ContentPart p = byKey.get(pk);
            if (p == null) {
                p = new ContentPart();
                p.setContent(content);
                p.setPartKey(pk);
                p.setTitle(null);
                p.setHtmlContent("");
                p.setOrderIndex(idx);
                p = partRepo.save(p);
            } else {
                p.setOrderIndex(idx);
                p = partRepo.save(p);
            }
            result.add(p);
            idx++;
        }
        return result;
    }

    @Override
    @Transactional
    public ContentPartDTO upsertPart(String keyName, String partKey, ContentPartDTO dto) {
        List<String> allowed = ALLOWED_PART_KEYS_BY_CONTENT.get(keyName);
        if (allowed == null || !allowed.contains(partKey)) {
            throw new IllegalArgumentException("PartKey no permitida: " + partKey);
        }
        List<ContentPart> parts = ensureAndGetPartsForContentKey(keyName);
        ContentPart part = parts.stream().filter(p -> p.getPartKey().equals(partKey)).findFirst()
                .orElseThrow(() -> new RuntimeException("Parte no encontrada: " + partKey));

        String cleaned = Jsoup.clean(dto.getHtmlContent(), HTML_SAFELIST);
        part.setTitle(dto.getTitle());
        part.setHtmlContent(cleaned);
        part.setOrderIndex(dto.getOrderIndex() != null ? dto.getOrderIndex() : part.getOrderIndex());
        ContentPart saved = partRepo.save(part);
        return partToDto(saved);
    }

    @Override
    @Transactional
    public ContentDTO upsertParts(String keyName, List<ContentPartDTO> partsDto) {
        List<String> allowed = ALLOWED_PART_KEYS_BY_CONTENT.get(keyName);
        if (allowed == null)
            throw new IllegalArgumentException("Content key no permitida: " + keyName);

        List<ContentPart> parts = ensureAndGetPartsForContentKey(keyName);
        Map<String, ContentPart> byKey = parts.stream().collect(Collectors.toMap(ContentPart::getPartKey, p -> p));

        // validate client didn't send disallowed keys
        for (ContentPartDTO pd : partsDto) {
            if (!allowed.contains(pd.getPartKey())) {
                throw new IllegalArgumentException("PartKey no permitida: " + pd.getPartKey());
            }
        }

        for (ContentPartDTO pd : partsDto) {
            ContentPart p = byKey.get(pd.getPartKey());
            if (p == null)
                continue;
            String cleaned = Jsoup.clean(pd.getHtmlContent(), HTML_SAFELIST);
            p.setTitle(pd.getTitle());
            p.setHtmlContent(cleaned);
            p.setOrderIndex(pd.getOrderIndex() != null ? pd.getOrderIndex() : p.getOrderIndex());
            partRepo.save(p);
        }
        return getByKey(keyName);
    }

    @Override
    public List<ContentDTO> listAll() {
        return contentRepo.findAll().stream().map(c -> {
            List<ContentPart> parts = partRepo.findByContentIdOrderByOrderIndex(c.getId());
            return toDto(c, parts);
        }).collect(Collectors.toList());
    }
}
