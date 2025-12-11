package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.model.Vacancy;
import com.unsis.admunsisbackend.repository.VacancyRepository;
import java.time.Year;
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
            "Resultado", "Comentario", "Puntaje", "Calificación"
    };

    @Autowired
    private AdmissionResultRepository resultRepo;

    @Autowired
    private ApplicantRepository applicantRepo;

    @Autowired
    private VacancyRepository vacancyRepo;

    private static final int ADMISSION_YEAR = Year.now().getValue(); // ajuste si requieres otro año

    // Helper para considerar resultados válidos como "aceptados"
    private boolean isAcceptedResult(String resultado) {
        if (resultado == null)
            return false;
        String r = resultado.trim().toUpperCase();
        return r.equals("ACEPTADO") || r.equals("APROBADO") || r.equals("ADMITIDO");
    } // Si quieres más variantes, agrégalas aquí

    private boolean isRejectedResult(String resultado) {
        if (resultado == null)
            return false;
        String r = resultado.trim().toUpperCase();
        return r.equals("RECHAZADO") || r.equals("NO ACEPTADO") || r.equals("NO APROBADO");
    }

    @Override
    @Transactional
    public ExcelUploadResponse processResultsExcel(MultipartFile file, Integer admissionYear) {
        ExcelUploadResponse resp = new ExcelUploadResponse();
        List<ExcelUploadResponse.ExcelError> errors = new ArrayList<>();
        int total = 0, processed = 0;

        // Estructura para almacenar filas leídas antes de persistir
        class RowData {
            int rowNum;
            String curp;
            String result;
            String comentario;
            Double rawScore; // Puntaje
            Double rawFinalGrade; // Calificación
            Applicant applicant; // cache del applicant encontrado
        }
        List<RowData> rows = new ArrayList<>();

        // Maps por carrera
        Map<String, Integer> totalByCareer = new HashMap<>();
        Map<String, Integer> acceptedByCareer = new HashMap<>();
        Map<String, Integer> rejectedByCareer = new HashMap<>();

        // elegir año efectivo (si viene del front lo usamos, si no el por defecto)
        int year = (admissionYear != null) ? admissionYear : ADMISSION_YEAR;

        // validación básica (opcional)
        int current = Year.now().getValue();
        if (year < 2000 || year > current + 1) {
            resp.setSuccess(false);
            resp.setMessage("Año de admisión inválido: " + year);
            return resp;
        }

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

            //Primera pasada: leer filas, cachear applicants y contadores por carrera
            Iterator<Row> it = sheet.rowIterator();
            it.next(); // saltar header

            while (it.hasNext()) {
                Row row = it.next();
                total++;

                RowData rd = new RowData();
                rd.rowNum = row.getRowNum() + 1;

                try {
                    rd.curp = row.getCell(0, MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue().trim();
                    rd.result = row.getCell(3, MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue().trim();
                    rd.comentario = row.getCell(4, MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue().trim();

                    Cell scoreCell = row.getCell(5, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    //rd.rawScore = scoreCell.getCellType() == CellType.NUMERIC ? scoreCell.getNumericCellValue() : null;
                    rd.rawScore = parseNumericCell(scoreCell, rd.rowNum, errors);

                    // Calificación: columna índice 6
                    Cell gradeCell = row.getCell(6, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rd.rawFinalGrade = parseNumericCell(gradeCell, rd.rowNum, errors);

                    rd.applicant = applicantRepo.findByCurp(rd.curp).orElse(null);
                    if (rd.applicant == null) {
                        errors.add(new ExcelUploadResponse.ExcelError(rd.rowNum,
                                "No existe aspirante con CURP " + rd.curp));
                    } else {
                        String career = rd.applicant.getCareer();
                        // Total
                        totalByCareer.merge(career, 1, Integer::sum);
                        // Aceptados / Rechazados
                        if (isAcceptedResult(rd.result)) {
                            acceptedByCareer.merge(career, 1, Integer::sum);
                        } else if (isRejectedResult(rd.result)) {
                            rejectedByCareer.merge(career, 1, Integer::sum);
                        } else {
                            // Si tu flujo considera otros estados, los tratamos como "otros"
                            // y también cuentan en total (ya se incrementó totalByCareer).
                        }
                    }

                    rows.add(rd);
                } catch (Exception ex) {
                    errors.add(new ExcelUploadResponse.ExcelError(rd.rowNum, ex.getMessage()));
                }
            }

            // Procesar filas (guardar Applicant.status y AdmissionResult)
            Map<String, Integer> acceptCountsByCareer = new HashMap<>();
            for (RowData rd : rows) {
                if (rd.applicant == null)
                    continue;
                if (isAcceptedResult(rd.result)) {
                    String career = rd.applicant.getCareer();
                    acceptCountsByCareer.merge(career, 1, Integer::sum);
                }
            }
            // Procesar filas (guardar Applicant.status y AdmissionResult)
            // === cambio: loop de guardado idempotente (actualiza en vez de crear
            // duplicados) ===
            for (RowData rd : rows) {
                try {
                    Applicant applicant = rd.applicant;
                    if (applicant == null)
                        continue; // ya reportado

                    // Siempre actualizamos el status del Applicant
                    applicant.setStatus(rd.result);
                    applicantRepo.save(applicant);

                    // Buscar AdmissionResult previo
                    Optional<AdmissionResult> optPrev = resultRepo.findTopByApplicantOrderByCreatedAtDesc(applicant);

                    // Normalizar valores
                    String newStatus = rd.result;
                    String newComment = rd.comentario.isEmpty() ? null : rd.comentario;
                    BigDecimal newScore = (rd.rawScore != null
                            && "LICENCIATURA EN MEDICINA".equalsIgnoreCase(applicant.getCareer()))
                                    ? BigDecimal.valueOf(rd.rawScore)
                                    : null;
                    BigDecimal newFinalGrade = rd.rawFinalGrade != null ? BigDecimal.valueOf(rd.rawFinalGrade) : null;

                    if (optPrev.isPresent()) {
                        AdmissionResult prev = optPrev.get();
                        // Compara todos los campos relevantes
                        boolean sameStatus = Objects.equals(prev.getStatus(), newStatus);
                        boolean sameComment = Objects.equals(prev.getComment(), newComment);
                        boolean sameScore = Objects.equals(prev.getScore(), newScore);

                        boolean sameCareer = Objects.equals(applicant.getCareer(),
                        applicant.getCareer());
                        boolean sameCurp = Objects.equals(applicant.getCurp(), rd.curp);
                        boolean sameName = Objects.equals(applicant.getUser().getFullName(),
                        applicant.getUser().getFullName());

                         if (sameCurp && sameName && sameCareer && sameStatus && sameComment &&
                        sameScore) {
                        // No hay cambios -> no hacemos nada
                        continue;
                        } else {
                            // Hay cambios -> actualizamos el registro existente
                            prev.setStatus(newStatus);
                            prev.setComment(newComment);
                            prev.setScore(newScore);
                            prev.setFinalGrade(newFinalGrade);
                            // opcional: actualizar o no el snapshot según tu política
                            prev.setCareerAtResult(applicant.getCareer());
                            prev.setAdmissionYear(year);
                            resultRepo.save(prev);
                            processed++;
                        }
                    } else {
                        // No existe resultado previo -> creamos uno nuevo
                        AdmissionResult ar = new AdmissionResult();
                        ar.setApplicant(applicant);
                        ar.setStatus(newStatus);
                        ar.setComment(newComment);
                        ar.setScore(newScore);
                        ar.setFinalGrade(newFinalGrade);
                        ar.setCareerAtResult(applicant.getCareer());
                        ar.setAdmissionYear(year);
                        resultRepo.save(ar);
                        processed++;
                    }
                } catch (Exception ex) {
                    errors.add(new ExcelUploadResponse.ExcelError(rd.rowNum, ex.getMessage()));
                }
            }

            for (Map.Entry<String, Integer> e : totalByCareer.entrySet()) {
                String career = e.getKey();
                int tot = e.getValue(); // inscritos detectados en el archivo
                int acc = acceptedByCareer.getOrDefault(career, 0); // aceptados detectados en el archivo
                int rej = rejectedByCareer.getOrDefault(career, 0); // rechazados -> los guardamos en pending_count

                Vacancy v = vacancyRepo.findByCareerAndAdmissionYear(career, year)
                        .orElseGet(() -> {
                            Vacancy nv = new Vacancy();
                            nv.setCareer(career);
                            nv.setAdmissionYear(year);
                            nv.setInscritosCount(0); // nuevo campo
                            nv.setCuposInserted(0); // nuevo campo (se actualiza desde el front)
                            nv.setReservedCount(0);
                            nv.setAvailableSlots(0);
                            return nv;
                        });

                boolean changed = false;

                // 1) inscritos (lo que viene del Excel)
                if (!Objects.equals(v.getInscritosCount(), tot)) {
                    v.setInscritosCount(tot);
                    changed = true;
                }

                // 2) Recalcular availableSlots = cuposInserted - inscritosCount
                int cupos = Optional.ofNullable(v.getCuposInserted()).orElse(0);
                int inscritos = Optional.ofNullable(v.getInscritosCount()).orElse(0);
                int available = Math.max(0, cupos - inscritos);

                if (!Objects.equals(v.getAvailableSlots(), available)) {
                    v.setAvailableSlots(available);
                    changed = true;
                }

                // Guardar solo si hubo cambios
                if (changed) {
                    vacancyRepo.save(v);
                }
            }

        } catch (Exception ex) {
            resp.setSuccess(false);
            resp.setMessage("Error leyendo Excel: " + ex.getMessage());
            resp.setErrors(errors);
            return resp;
        }

        // Mensaje resumen con contadores por carrera
        StringBuilder summary = new StringBuilder();
        for (String career : totalByCareer.keySet()) {
            int tot = totalByCareer.getOrDefault(career, 0);
            int acc = acceptedByCareer.getOrDefault(career, 0);
            int rej = rejectedByCareer.getOrDefault(career, 0);
            summary.append(String.format("%s -> total: %d (aceptados: %d, rechazados: %d). ", career, tot, acc, rej));
        }

        resp.setSuccess(true);
        resp.setMessage(String.format("%d/%d registros procesados. %s", processed, total, summary.toString()));
        resp.setErrors(errors);
        return resp;
    }

    private Double parseNumericCell(Cell c, int rowNum, List<ExcelUploadResponse.ExcelError> errors) {
        try {
            if (c == null)
                return null;
            if (c.getCellType() == CellType.NUMERIC)
                return c.getNumericCellValue();
            String s = c.getStringCellValue();
            if (s == null || s.isBlank())
                return null;
            return Double.parseDouble(s.trim());
        } catch (Exception ex) {
            errors.add(new ExcelUploadResponse.ExcelError(rowNum, "Formato numérico inválido: " + ex.getMessage()));
            return null;
        }
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
            dto.setCareerAtResult(ar.getCareerAtResult());
            dto.setStatus(ar.getStatus());
            dto.setComment(ar.getComment());
            dto.setScore(ar.getScore());
            dto.setFinalGrade(ar.getFinalGrade());
            dto.setAdmissionYear(ar.getAdmissionYear());
            dto.setCreatedAt(ar.getCreatedAt());
            dto.setLastLogin(ar.getApplicant().getUser().getLastLogin());
            return dto;
        }).collect(Collectors.toList());
    }
}
