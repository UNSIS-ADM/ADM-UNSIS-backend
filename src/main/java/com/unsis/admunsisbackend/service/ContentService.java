package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.ContentDTO;
import java.util.List;

public interface ContentService {
    ContentDTO getByKey(String keyName);
    List<ContentDTO> listAll();
    ContentDTO saveOrUpdate(String keyName, ContentDTO dto); // admin
}
