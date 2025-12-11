package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.AdmissionResultDTO;
import com.unsis.admunsisbackend.dto.ExcelUploadResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

// Servicio para gestionar los resultados de admisión.
public interface AdmissionResultService {
    // Procesa un archivo Excel con los resultados de admisión para un año específico.
    ExcelUploadResponse processResultsExcel(MultipartFile file, Integer admissionYear);
    
    // Obtiene todos los resultados de admisión.
    List<AdmissionResultDTO> getAllResults();
}
