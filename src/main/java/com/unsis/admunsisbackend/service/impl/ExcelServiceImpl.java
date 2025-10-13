package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.ExcelUploadResponse;
import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.model.Role;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.repository.UserRepository;
import com.unsis.admunsisbackend.repository.RoleRepository;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.service.ExcelService;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

import java.time.ZoneId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
public class ExcelServiceImpl implements ExcelService {
    private static final Logger logger = LoggerFactory.getLogger(ExcelServiceImpl.class);
    private static final Pattern CURP_PATTERN = Pattern.compile("^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$");

    private static final List<String> VALID_CAREERS = Arrays.asList(
            "LICENCIATURA EN ADMINISTRACIÓN MUNICIPAL",
            "LICENCIATURA EN ADMINISTRACIÓN PÚBLICA",
            "LICENCIATURA EN CIENCIAS BIOMÉDICAS",
            "LICENCIATURA EN CIENCIAS EMPRESARIALES",
            "LICENCIATURA EN ENFERMERÍA",
            "LICENCIATURA EN INFORMÁTICA",
            "LICENCIATURA EN MEDICINA",
            "LICENCIATURA EN NUTRICIÓN",
            "LICENCIATURA EN ODONTOLOGÍA");

    private static final String[] EXPECTED_HEADERS = {
            "Número Ficha",
            "Nombre completo",
            "Carrera",
            "CURP",
            "Lugar",
            "Aula/Sala de Cómputo",
            "Fecha Examen",
    };

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ApplicantRepository applicantRepository;
    /**
     * @Autowired
     *            private VacancyRepository vacancyRepository;
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ExcelUploadResponse processExcel(MultipartFile file) {
        ExcelUploadResponse response = new ExcelUploadResponse();
        List<ExcelUploadResponse.ExcelError> errors = new ArrayList<>();
        int totalRows = 0;
        int processedRows = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (!validateHeaders(headerRow)) {
                response.setSuccess(false);
                response.setMessage("Estructura de Excel inválida");
                return response;
            }

            Role applicantRole = roleRepository.findByName("ROLE_APPLICANT")
                    .orElseThrow(() -> new RuntimeException("Rol APPLICANT no encontrado"));

            Iterator<Row> rowIterator = sheet.iterator();
            rowIterator.next();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                totalRows++;

                try {
                    processRow(row, applicantRole);
                    processedRows++;
                } catch (Exception e) {
                    errors.add(new ExcelUploadResponse.ExcelError(
                            row.getRowNum() + 1,
                            e.getMessage()));
                }
            }

        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage("Error al procesar el archivo: " + e.getMessage());
            return response;
        }

        response.setSuccess(true);
        response.setMessage(String.format("%d/%d registros procesados", processedRows, totalRows));
        response.setErrors(errors);
        return response;
    }

    private boolean validateHeaders(Row headerRow) {
        if (headerRow == null)
            return false;

        for (int i = 0; i < EXPECTED_HEADERS.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !cell.getStringCellValue().equals(EXPECTED_HEADERS[i])) {
                return false;
            }
        }
        return true;
    }

    @Transactional
    private void processRow(Row row, Role applicantRole) {
        // Ficha primero (necesaria para la validación de username)
        Long fichaExcel;
        try {
            fichaExcel = Long.valueOf(getCellValue(row.getCell(0)));
        } catch (Exception e) {
            throw new RuntimeException("Número de ficha inválido");
        }
        String fichaStr = fichaExcel.toString();

        // Validar CURP
        String curp = getCellValue(row.getCell(3));
        if (!CURP_PATTERN.matcher(curp).matches()) {
            throw new RuntimeException("Formato de CURP inválido");
        }

        // Validar carrera
        String career = getCellValue(row.getCell(2)).toUpperCase();
        if (!VALID_CAREERS.contains(career)) {
            throw new RuntimeException("Carrera no válida");
        }

        /*
         * Validar usuario existente no exista ya por CURP
         * if (userRepository.existsByUsername(curp)) {
         * throw new RuntimeException("El usuario con CURP ya está registrado");
         * }
         */
        // Validar usuario existente por username (ficha)
        if (userRepository.existsByUsername(fichaStr)) {
            throw new RuntimeException("El usuario (ficha) ya está registrado: " + fichaStr);
        }

        // Validar aspirante existente por CURP
        if (applicantRepository.existsByCurp(curp)) {
            throw new RuntimeException("Ya existe un usuario con esta CURP");
        }
        /*
         * Long fichaExcel = Long.valueOf(getCellValue(row.getCell(0)));
         * String fichaStr = fichaExcel.toString();
         */
        /*
         * // ** INTEGRACIÓN DE VALIDACIÓN DE VACANTES: **
         * int currentYear = Year.now().getValue();
         * long inscritos = applicantRepository.countByCareerAndAdmissionYear(career,
         * currentYear);
         * 
         * var vac = vacancyRepository
         * .findByCareerAndAdmissionYear(career, currentYear)
         * .orElseThrow(
         * () -> new RuntimeException("Vacantes no configuradas para " + career + " en "
         * + currentYear));
         * 
         * if (inscritos >= vac.getLimitCount()) {
         * throw new RuntimeException("Cupo agotado para " + career);
         * }
         * // ** fin de la integración **
         */
        // Crear usuario
        User user = new User();
        user.setUsername(fichaStr); // login = ficha
        user.setPassword(passwordEncoder.encode(curp)); // pass = curp
        user.setFullName(getCellValue(row.getCell(1)));
        user.setActive(true);
        user.setRoles(Set.of(applicantRole));
        user = userRepository.save(user);

        // EXTRAER fecha de examen (soporta celdas tipo fecha y texto con varios
        // patterns)
        LocalDateTime examDate = null;
        Cell examCell = row.getCell(6);

        if (examCell != null) {
            // 1) Si la celda es NUMERIC y está formateada como fecha en Excel
            if (examCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(examCell)) {
                Date date = examCell.getDateCellValue(); // java.util.Date
                examDate = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
            } else {
                // 2) Si la celda es STRING (o NUMERIC no-formateado), intentamos parsear con
                // varios patrones
                String examDateStr = getCellValue(examCell);
                if (!examDateStr.isBlank()) {
                    // patrones permitidos (añade más si los necesitas)
                    DateTimeFormatter[] fmts = new DateTimeFormatter[] {
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
                            DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    };

                    boolean parsed = false;
                    for (DateTimeFormatter fmt : fmts) {
                        try {
                            if (fmt.toString().contains("H")) {
                                // patrón con hora -> LocalDateTime
                                examDate = LocalDateTime.parse(examDateStr, fmt);
                            } else {
                                // patrón solo fecha -> LocalDate a medianoche
                                LocalDate d = LocalDate.parse(examDateStr, fmt);
                                examDate = d.atStartOfDay();
                            }
                            parsed = true;
                            break;
                        } catch (DateTimeParseException pe) {
                            // intentar siguiente patrón
                        }
                    }

                    if (!parsed) {
                        logger.warn("No se pudo parsear Fecha Examen '{}' en fila {}", examDateStr,
                                row.getRowNum() + 1);
                        examDate = null;
                    }
                }
            }
        }

        // Crear aspirante
        Applicant applicant = new Applicant();
        applicant.setUser(user);
        applicant.setFicha(fichaExcel);
        applicant.setCurp(curp);
        applicant.setCareer(career);
        applicant.setLocation(getCellValue(row.getCell(4)));
        applicant.setExamRoom(getCellValue(row.getCell(5)));
        applicant.setExamDate(examDate);
        applicant.setExamAssigned(false);
        applicant.setStatus("PENDIENTE");
        // applicant.setAdmissionYear(currentYear);
        applicant.setAdmissionYear(Year.now().getValue());
        applicantRepository.save(applicant);
    }

    private String getCellValue(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Si la celda es fecha y quieres un string, podrías formatearla aquí,
                // pero preferimos manejar fechas en processRow con
                // DateUtil.isCellDateFormatted.
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) {
                    // entero
                    return String.valueOf((long) d);
                } else {
                    // decimal: evitar notación científica
                    return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // intentar el resultado cached de la fórmula
                switch (cell.getCachedFormulaResultType()) {
                    case STRING:
                        return cell.getStringCellValue().trim();
                    case NUMERIC:
                        double v = cell.getNumericCellValue();
                        if (v == Math.floor(v))
                            return String.valueOf((long) v);
                        else
                            return BigDecimal.valueOf(v).stripTrailingZeros().toPlainString();
                    default:
                        return "";
                }
            case BLANK:
            default:
                return "";
        }
    }
}
