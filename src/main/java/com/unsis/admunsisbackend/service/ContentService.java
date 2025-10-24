package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.ContentDTO;
import com.unsis.admunsisbackend.dto.ContentPartDTO;
import java.util.List;

public interface ContentService {
    ContentDTO getByKey(String keyName);
    List<ContentDTO> listAll();
    ContentPartDTO upsertPart(String keyName, String partKey, ContentPartDTO dto);
    ContentDTO upsertParts(String keyName, List<ContentPartDTO> parts);
}


