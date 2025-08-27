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
            "Resultado", "Comentario", "Puntaje"
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
    public ExcelUploadResponse processResultsExcel(MultipartFile file) {
        ExcelUploadResponse resp = new ExcelUploadResponse();
        List<ExcelUploadResponse.ExcelError> errors = new ArrayList<>();
        int total = 0, processed = 0;

        // Estructura para almacenar filas leídas antes de persistir
        class RowData {
            int rowNum;
            String curp;
            String resultado;
            String comentario;
            Double rawScore;
            Applicant applicant; // cache del applicant encontrado
        }
        List<RowData> rows = new ArrayList<>();

        // Maps por carrera
        Map<String, Integer> totalByCareer = new HashMap<>();
        Map<String, Integer> acceptedByCareer = new HashMap<>();
        Map<String, Integer> rejectedByCareer = new HashMap<>();

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

            // Primera pasada: leer filas, cachear applicants y contadores por carrera
            Iterator<Row> it = sheet.rowIterator();
            it.next(); // saltar header

            while (it.hasNext()) {
                Row row = it.next();
                total++;

                RowData rd = new RowData();
                rd.rowNum = row.getRowNum() + 1;

                try {
                    rd.curp = row.getCell(0, MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue().trim();
                    rd.resultado = row.getCell(3, MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue().trim();
                    rd.comentario = row.getCell(4, MissingCellPolicy.CREATE_NULL_AS_BLANK).getStringCellValue().trim();

                    Cell scoreCell = row.getCell(5, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    rd.rawScore = scoreCell.getCellType() == CellType.NUMERIC ? scoreCell.getNumericCellValue() : null;

                    rd.applicant = applicantRepo.findByCurp(rd.curp).orElse(null);
                    if (rd.applicant == null) {
                        errors.add(new ExcelUploadResponse.ExcelError(rd.rowNum,
                                "No existe aspirante con CURP " + rd.curp));
                    } else {
                        String career = rd.applicant.getCareer();
                        // Total
                        totalByCareer.merge(career, 1, Integer::sum);
                        // Aceptados / Rechazados
                        if (isAcceptedResult(rd.resultado)) {
                            acceptedByCareer.merge(career, 1, Integer::sum);
                        } else if (isRejectedResult(rd.resultado)) {
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
            // Si hay errores críticos encontrados en lectura (p.ej aspirantes faltantes),
            // devolvemos sin procesar nada (puedes cambiar esto si prefieres ignorar solo
            // las filas malas).
            /*
             * boolean hasMissingApplicants = errors.stream()
             * .anyMatch(e -> e.getError().contains("No existe aspirante"));
             * if (hasMissingApplicants) {
             * resp.setSuccess(false);
             * resp.
             * setMessage("Existen aspirantes no encontrados. Corrige el archivo o la base de datos."
             * );
             * resp.setErrors(errors);
             * return resp;
             * }
             */

            // 2) Validación de cupos por carrera
            // Contar cuántos "ACEPTADOS" habría por carrera en este lote
            Map<String, Integer> acceptCountsByCareer = new HashMap<>();
            for (RowData rd : rows) {
                if (rd.applicant == null)
                    continue; // ya reportado como error
                if (isAcceptedResult(rd.resultado)) {
                    String career = rd.applicant.getCareer();
                    acceptCountsByCareer.merge(career, 1, Integer::sum);
                }
            }

            // Verificar vacantes para cada carrera involucrada
            // Validación de cupos: para cada carrera verificar availableSlots >=
            // totalByCareer (Aceptados+Rechazados)
            List<String> insufficient = new ArrayList<>();
            int year = Year.now().getValue(); // o el año que uses para admissionYear
            for (Map.Entry<String, Integer> e : totalByCareer.entrySet()) {
                String career = e.getKey();
                int needed = e.getValue(); // total (aceptados + rechazados + otros resultados)
                Optional<Vacancy> optVac = vacancyRepo.findByCareerAndAdmissionYear(career, year);
                if (optVac.isEmpty()) {
                    insufficient.add(String.format("%s (no existe vacante definida, requiere %d)", career, needed));
                    continue;
                }
                Vacancy v = optVac.get();
                int available = v.getAvailableSlots() != null ? v.getAvailableSlots() : 0;
                if (available < needed) {
                    insufficient.add(
                            String.format("%s (cupos disponibles: %d, requeridos: %d)", career, available, needed));
                }
            }

            if (!insufficient.isEmpty()) {
                resp.setSuccess(false);
                resp.setMessage(
                        "❌ No hay cupos suficientes para las siguientes carreras: " + String.join("; ", insufficient)
                                + ". Agrega cupos y vuelve a intentar.");
                resp.setErrors(errors);
                return resp;
            }

            // Procesar filas (guardar Applicant.status y AdmissionResult)
            // Llevamos un contador por carrera de cuántos se procesaron correctamente
            Map<String, Integer> processedByCareer = new HashMap<>();

            for (RowData rd : rows) {
                try {
                    Applicant applicant = rd.applicant;
                    if (applicant == null)
                        continue; // ya reportado

                    // Actualizar status
                    applicant.setStatus(rd.resultado);
                    applicantRepo.save(applicant);

                    // Guardar AdmissionResult
                    AdmissionResult ar = new AdmissionResult();
                    ar.setApplicant(applicant);
                    ar.setResult(rd.resultado);
                    ar.setComment(rd.comentario.isEmpty() ? null : rd.comentario);

                    if ("LICENCIATURA EN MEDICINA".equalsIgnoreCase(applicant.getCareer()) && rd.rawScore != null) {
                        ar.setScore(BigDecimal.valueOf(rd.rawScore));
                    }

                    resultRepo.save(ar);
                    processed++;

                    // contar procesados por carrera (para descontar después)
                    String career = applicant.getCareer();
                    processedByCareer.merge(career, 1, Integer::sum);

                } catch (Exception ex) {
                    errors.add(new ExcelUploadResponse.ExcelError(rd.rowNum, ex.getMessage()));
                }
            }

            // Después de procesar, descontar processedByCareer de availableSlots (solo los
            // que realmente se guardaron)
            for (Map.Entry<String, Integer> e : processedByCareer.entrySet()) {
                String career = e.getKey();
                int dec = e.getValue();
                Vacancy v = vacancyRepo.findByCareerAndAdmissionYear(career, year)
                        .orElseThrow(() -> new RuntimeException(
                                "Vacante no encontrada al actualizar contadores para " + career));
                int currentAvailable = v.getAvailableSlots() != null ? v.getAvailableSlots() : 0;
                int newAvailable = currentAvailable - dec;
                if (newAvailable < 0)
                    newAvailable = 0;
                v.setAvailableSlots(newAvailable);
                // También puedes actualizar acceptedCount/rejectedCount si deseas:
                int accepted = acceptedByCareer.getOrDefault(career, 0);
                int rejected = rejectedByCareer.getOrDefault(career, 0);
                v.setAcceptedCount(Math.max(0, v.getAcceptedCount() + accepted)); // ejemplo de cómo actualizar counters
                v.setPendingCount(Math.max(0, v.getPendingCount())); // si aplica
                vacancyRepo.save(v);
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


/*Implementar
 * Race conditions: si existe concurrencia (varios procesos subiendo
 * simultáneamente), todavía puede haber overbooking entre la validación y el
 * save() final. Para evitarlo:
 * 
 * Usa bloqueo pesimista (@Lock(PESSIMISTIC_WRITE) en un método del repo o
 * SELECT FOR UPDATE) cuando leas la Vacancy para validación y actualización, o
 * 
 * Implementa una actualización atómica en la DB (por ejemplo un UPDATE vacancy
 * SET available_slots = available_slots - :n WHERE career = :c AND
 * admission_year = :y AND available_slots >= :n y comprobar que rowsAffected ==
 * 1), o
 * 
 * Usa restricciones a nivel DB (CHECK / triggers) para que no permita valores
 * negativos.
 * Nutri esta liberando vacantes si hace un cambio de carrera y fue rechazado no
 * no deberia liberar la vacante, solo ocupar a la carrera que solicite.
 */