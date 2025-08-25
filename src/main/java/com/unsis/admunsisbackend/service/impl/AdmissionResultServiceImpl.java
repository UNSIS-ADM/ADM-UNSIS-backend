package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.AdmissionResultDTO;
import com.unsis.admunsisbackend.dto.ExcelUploadResponse;
import com.unsis.admunsisbackend.model.AdmissionResult;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.repository.AdmissionResultRepository;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.service.AdmissionResultService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdmissionResultServiceImpl implements AdmissionResultService {

    private static final String[] HEADERS = {
            "CURP", "Nombre completo", "Carrera",
            "Resultado", "Comentario", "Puntaje"
    };

    @Autowired
    private AdmissionResultRepository resultRepo;
    @Autowired
    private ApplicantRepository applicantRepo;

    @Override
    @Transactional
    public ExcelUploadResponse processResultsExcel(MultipartFile file) {
        ExcelUploadResponse resp = new ExcelUploadResponse();
        List<ExcelUploadResponse.ExcelError> errors = new ArrayList<>();
        int total = 0, processed = 0;

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);

            // 1) Validar encabezados
            Row header = sheet.getRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = header.getCell(i, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                if (!HEADERS[i].equalsIgnoreCase(c.getStringCellValue().trim())) {
                    resp.setSuccess(false);
                    resp.setMessage("Estructura de Excel inválida, encabezado '"
                            + HEADERS[i] + "' no encontrado.");
                    return resp;
                }
            }

            // 2) Iterar filas de datos
            Iterator<Row> it = sheet.rowIterator();
            it.next(); // saltar header

            while (it.hasNext()) {
                Row row = it.next();
                total++;

                try {
                    // Leer CURP (columna 0)
                    String curp = row
                            .getCell(0, MissingCellPolicy.CREATE_NULL_AS_BLANK)
                            .getStringCellValue().trim();

                    // Leer Resultado (columna 3)
                    String resultado = row
                            .getCell(3, MissingCellPolicy.CREATE_NULL_AS_BLANK)
                            .getStringCellValue().trim();

                    // Leer Comentario (columna 4)
                    String comentario = row
                            .getCell(4, MissingCellPolicy.CREATE_NULL_AS_BLANK)
                            .getStringCellValue().trim();

                    // Leer Puntaje (columna 5), puede venir vacío
                    Cell scoreCell = row.getCell(5, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    Double rawScore = scoreCell.getCellType() == CellType.NUMERIC
                            ? scoreCell.getNumericCellValue()
                            : null;

                    // --- BUSCAR AL ASPIRANTE POR 'curp' ---
                    Applicant applicant = applicantRepo.findByCurp(curp)
                            .orElseThrow(() -> new RuntimeException(
                                    "No existe aspirante con CURP " + curp));

                    // --- ACTUALIZAR SU STATUS ---
                    applicant.setStatus(resultado);
                    applicantRepo.save(applicant);

                    // --- GUARDAR EL ADMISSION_RESULT ---
                    AdmissionResult ar = new AdmissionResult();
                    ar.setApplicant(applicant);
                    ar.setResult(resultado);
                    ar.setComment(comentario.isEmpty() ? null : comentario);

                    // Sólo asignar puntaje si la carrera es Medicina
                    if ("LICENCIATURA EN MEDICINA"
                            .equalsIgnoreCase(applicant.getCareer())
                            && rawScore != null) {
                        ar.setScore(BigDecimal.valueOf(rawScore));
                    }

                    resultRepo.save(ar);
                    processed++;

                } catch (Exception ex) {
                    errors.add(new ExcelUploadResponse.ExcelError(
                            row.getRowNum() + 1,
                            ex.getMessage()));
                }
            }

        } catch (Exception ex) {
            resp.setSuccess(false);
            resp.setMessage("Error leyendo Excel: " + ex.getMessage());
            return resp;
        }

        resp.setSuccess(true);
        resp.setMessage(String.format("%d/%d registros procesados", processed, total));
        resp.setErrors(errors);
        return resp;
    }

    @Override
    public List<AdmissionResultDTO> getAllResults() {
        return resultRepo.findAll().stream().map(ar -> {
            AdmissionResultDTO dto = new AdmissionResultDTO();
            dto.setId(ar.getId());
            dto.setApplicantId(ar.getApplicant().getId());
            dto.setFicha(ar.getApplicant().getFicha());
            dto.setFullName(ar.getApplicant().getUser().getFullName());
            dto.setCareer(ar.getApplicant().getCareer());
            dto.setResult(ar.getResult());
            dto.setComment(ar.getComment());
            dto.setScore(ar.getScore());
            dto.setCreatedAt(ar.getCreatedAt());
            dto.setLastLogin(ar.getApplicant().getUser().getLastLogin());
            return dto;
        }).collect(Collectors.toList());
    }
}
