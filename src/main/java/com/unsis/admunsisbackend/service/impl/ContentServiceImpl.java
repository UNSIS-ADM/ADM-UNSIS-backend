package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.ContentDTO;
import com.unsis.admunsisbackend.model.Content;
import com.unsis.admunsisbackend.repository.ContentRepository;
import com.unsis.admunsisbackend.service.ContentService;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContentServiceImpl implements ContentService {

    private final ContentRepository repo;

    public ContentServiceImpl(ContentRepository repo) {
        this.repo = repo;
    }

    @Override
    public ContentDTO getByKey(String keyName) {
        return repo.findByKeyName(keyName)
                .map(this::toDto)
                .orElse(null);
    }

    @Override
    public List<ContentDTO> listAll() {
        return repo.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ContentDTO saveOrUpdate(String keyName, ContentDTO dto) {
        // Sanitizar HTML antes de guardar
        // usa Safelist.relaxed() o construye la lista que necesites
        String cleaned = Jsoup.clean(dto.getHtmlContent(), Safelist.relaxed()
                         .addTags("span") // por ejemplo
                         .addAttributes("span", "class", "style")); // si necesitas clases/styles
        Content c = repo.findByKeyName(keyName).orElse(new Content());
        c.setKeyName(keyName);
        c.setTitle(dto.getTitle());
        c.setHtmlContent(cleaned);
        c.setLanguage(dto.getLanguage() != null ? dto.getLanguage() : "es");
        c.setActive(dto.isActive());
        Content saved = repo.save(c);
        return toDto(saved);
    }

    private ContentDTO toDto(Content c) {
        ContentDTO d = new ContentDTO();
        d.setId(c.getId());
        d.setKeyName(c.getKeyName());
        d.setTitle(c.getTitle());
        d.setHtmlContent(c.getHtmlContent()); // ya sanitizado
        d.setLanguage(c.getLanguage());
        d.setActive(c.isActive());
        return d;
    }
}
