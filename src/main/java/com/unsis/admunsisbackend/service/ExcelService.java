package com.unsis.admunsisbackend.service;

import org.springframework.web.multipart.MultipartFile;
import com.unsis.admunsisbackend.dto.ExcelUploadResponse;

public interface ExcelService {
    ExcelUploadResponse processExcel(MultipartFile file);
}
