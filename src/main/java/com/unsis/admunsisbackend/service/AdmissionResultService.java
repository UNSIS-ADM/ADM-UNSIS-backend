package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.AdmissionResultDTO;
import com.unsis.admunsisbackend.dto.ExcelUploadResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface AdmissionResultService {
    ExcelUploadResponse processResultsExcel(MultipartFile file, Integer admissionYear);
    List<AdmissionResultDTO> getAllResults();   
}
