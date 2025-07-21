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
            "Número Ficha", "Nombre completo", "Carrera",
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
                    resp.setMessage("Estructura de Excel inválida, encabezado '" + HEADERS[i] + "' no encontrado.");
                    return resp;
                }
            }

            // 2) Iterar sobre las filas de datos
            Iterator<Row> it = sheet.rowIterator();
            it.next(); // saltar header

            while (it.hasNext()) {
                Row row = it.next();
                total++;

                try {
                    // Leer ficha (columna 0)
                    Cell fichaCell = row.getCell(0, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    long ficha = (long) fichaCell.getNumericCellValue();

                    // Leer resultado (columna 3)
                    String resultado = row.getCell(3, MissingCellPolicy.CREATE_NULL_AS_BLANK)
                            .getStringCellValue().trim();

                    // Leer comentario (columna 4)
                    String comentario = row.getCell(4, MissingCellPolicy.CREATE_NULL_AS_BLANK)
                            .getStringCellValue().trim();

                    // Leer puntaje (columna 5), puede venir vacío
                    Cell scoreCell = row.getCell(5, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    Double rawScore = scoreCell.getCellType() == CellType.NUMERIC
                            ? scoreCell.getNumericCellValue()
                            : null;

                    // --- BUSCAR AL ASPIRANTE POR 'ficha' ---
                    Applicant applicant = applicantRepo.findByFicha(ficha)
                            .orElseThrow(() -> new RuntimeException("No existe aspirante con ficha " + ficha));

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
            dto.setFullName(ar.getApplicant().getUser().getFullName());
            dto.setCareer(ar.getApplicant().getCareer());
            dto.setResult(ar.getResult());
            dto.setComment(ar.getComment());
            dto.setScore(ar.getScore());
            dto.setCreatedAt(ar.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }
}



/*package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.AdmissionResultDTO;
import com.unsis.admunsisbackend.dto.ExcelUploadResponse;
import com.unsis.admunsisbackend.model.AdmissionResult;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.repository.AdmissionResultRepository;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.service.AdmissionResultService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
        "Número Ficha", "Nombre completo", "Carrera",
        "Resultado", "Comentario", "Puntaje"
    };

    @Autowired private AdmissionResultRepository resultRepo;
    @Autowired private ApplicantRepository applicantRepo;

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
                if (header.getCell(i) == null
                    || !HEADERS[i].equalsIgnoreCase(header.getCell(i).getStringCellValue().trim())) {
                    resp.setSuccess(false);
                    resp.setMessage("Estructura de Excel inválida, encabezado '" + HEADERS[i] + "' no encontrado.");
                    return resp;
                }
            }

            // 2) Iterar filas
            Iterator<Row> it = sheet.rowIterator();
            it.next(); // salta header
            while (it.hasNext()) {
                Row row = it.next();
                total++;
                try {
                    Long ficha = (long) row.getCell(0).getNumericCellValue();
                    String resultado = row.getCell(3).getStringCellValue().trim();
                    String comentario = row.getCell(4).getStringCellValue().trim();
                    Double rawScore = row.getCell(5).getCellType() == CellType.NUMERIC
                            ? row.getCell(5).getNumericCellValue()
                            : null;

                    // --- AQUÍ: buscar Applicant por 'ficha' ---
                    Applicant applicant = applicantRepo.findByFicha(ficha)
                        .orElseThrow(() ->
                            new RuntimeException("No existe aspirante con ficha " + ficha)
                        );

                    // --- ACTUALIZAR EL STATUS DEL APPLICANT ---
                    applicant.setStatus(resultado); 
                    applicantRepo.save(applicant);

                    // --- GUARDAR EL AdmissionResult ---
                    AdmissionResult result = new AdmissionResult();
                    result.setApplicant(applicant);
                    result.setResult(resultado);
                    result.setComment(comentario.isEmpty() ? null : comentario);

                    // Asignar puntaje solo para Medicina
                    if ("LICENCIATURA EN MEDICINA"
                            .equalsIgnoreCase(applicant.getCareer())
                        && rawScore != null) {
                        result.setScore(BigDecimal.valueOf(rawScore));
                    }
                    resultRepo.save(result);

                    processed++;
                } catch (Exception ex) {
                    errors.add(new ExcelUploadResponse.ExcelError(
                        row.getRowNum() + 1, ex.getMessage()));
                }
            }
/*
 * @Override
 * 
 * @Transactional
 * public ExcelUploadResponse processResultsExcel(MultipartFile file) {
 * // ... validación de headers y setup ...
 * while (it.hasNext()) {
 * Row row = it.next();
 * try {
 * Long ficha = (long) row.getCell(0).getNumericCellValue();
 * String resultado = row.getCell(3).getStringCellValue().trim();
 * String comment = row.getCell(4).getStringCellValue().trim();
 * Double rawScore = row.getCell(5).getCellType() == CellType.NUMERIC
 * ? row.getCell(5).getNumericCellValue()
 * : null;
 * 
 * // 1) Obtener aspirante por ficha
 * Applicant applicant = applicantRepo.findByFicha(ficha)
 * .orElseThrow(() -> new RuntimeException("No existe aspirante con ficha " +
 * ficha));
 * 
 * // 2) Actualizar su status
 * applicant.setStatus(resultado);
 * applicantRepo.save(applicant);
 * 
 * // 3) Crear histórico en admission_results
 * AdmissionResult ar = new AdmissionResult();
 * ar.setApplicant(applicant);
 * ar.setResult(resultado);
 * ar.setComment(comment.isEmpty() ? null : comment);
 * if ("LICENCIATURA EN MEDICINA".equalsIgnoreCase(applicant.getCareer())) {
 * ar.setScore(rawScore != null
 * ? BigDecimal.valueOf(rawScore)
 * : null);
 * }
 * resultRepo.save(ar);
 * 
 * processed++;
 * } catch (Exception ex) {
 * errors.add(new ExcelUploadResponse.ExcelError(row.getRowNum() + 1,
 * ex.getMessage()));
 * }
 * }
 *          

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



/*  private static final String[] HEADERS = {
            "Número Ficha", "Nombre completo", "Carrera", "Resultado", "Comentario", "Puntaje"
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
                Cell c = header.getCell(i);
                if (c == null || !HEADERS[i].equalsIgnoreCase(c.getStringCellValue().trim())) {
                    resp.setSuccess(false);
                    resp.setMessage("Estructura de Excel inválida, encabezado '" + HEADERS[i] + "' no encontrado.");
                    return resp;
                }
            }
            // 2) Iterar filas
            Iterator<Row> it = sheet.rowIterator();
            it.next();
            while (it.hasNext()) {
                Row row = it.next();
                total++;
                try {
                    Long ficha = (long) row.getCell(0).getNumericCellValue();
                    String nombre = row.getCell(1).getStringCellValue().trim();
                    String carrera = row.getCell(2).getStringCellValue().trim();
                    String resultado = row.getCell(3).getStringCellValue().trim();
                    String comment = row.getCell(4).getStringCellValue().trim();
                    Double score = row.getCell(5).getCellType() == CellType.NUMERIC
                            ? row.getCell(5).getNumericCellValue()
                            : null;

                    Applicant applicant = applicantRepo.findByFicha(ficha)
                        .orElseThrow(() -> new RuntimeException("No existe aspirante con ficha " + ficha));

                    AdmissionResult result = new AdmissionResult();
                    result.setApplicant(applicant);
                    result.setResult(resultado);
                    result.setComment(comment.isEmpty() ? null : comment);
                    // Sólo asigna “score” si carrera == Medicina
                    if ("LICENCIATURA EN MEDICINA".equalsIgnoreCase(carrera)) {
                        result.setScore(score != null ? new java.math.BigDecimal(score) : null);
                    }
                    resultRepo.save(result);
                    processed++;
                } catch (Exception ex) {
                    errors.add(new ExcelUploadResponse.ExcelError(
                            row.getRowNum() + 1, ex.getMessage()));
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
*
    

    @Override
    public List<AdmissionResultDTO> getAllResults() {
        return resultRepo.findAll().stream().map(ar -> {
            AdmissionResultDTO dto = new AdmissionResultDTO();
            dto.setId(ar.getId());
            dto.setApplicantId(ar.getApplicant().getId());
            dto.setFullName(ar.getApplicant().getUser().getFullName());
            dto.setCareer(ar.getApplicant().getCareer());
            dto.setResult(ar.getResult());
            dto.setComment(ar.getComment());
            dto.setScore(ar.getScore());
            dto.setCreatedAt(ar.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
    }
}
*/