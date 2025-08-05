package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.ExcelUploadResponse;
import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.model.Role;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.repository.UserRepository;
import com.unsis.admunsisbackend.repository.RoleRepository;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.repository.VacancyRepository; // ← nuevo
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
import java.time.Year; // ← para Year.now()
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class ExcelServiceImpl implements ExcelService {
    private static final Logger logger = LoggerFactory.getLogger(ExcelServiceImpl.class);
    private static final Pattern CURP_PATTERN = Pattern.compile("^[A-Z]{4}\\d{6}[HM][A-Z]{5}[A-Z0-9]{2}$");

    private static final List<String> VALID_CAREERS = Arrays.asList(
            "LICENCIATURA EN ADMINISTRACION MUNICIPAL",
            "LICENCIATURA EN ADMINISTRACION PÚBLICA",
            "LICENCIATURA EN CIENCIAS BIOMÉDICAS",
            "LICENCIATURA EN CIENCIAS EMPRESARIALES",
            "LICENCIATURA EN ENFERMERÍA",
            "LICENCIATURA EN INFORMATICA",
            "LICENCIATURA EN MEDICINA",
            "LICENCIATURA EN NUTRICION",
            "LICENCIATURA EN ODONTOLOGÍA");

    private static final String[] EXPECTED_HEADERS = {
            "Número Ficha",
            "Nombre completo",
            "Carrera",
            "CURP",
            "Lugar",
            "Aula/Sala de Cómputo",
            "Fecha Examen",
            "Teléfono"
    };

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ApplicantRepository applicantRepository;

    @Autowired
    private VacancyRepository vacancyRepository; // ← inyectado

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

        // Validar usuario existente no exista ya por CURP
        if (userRepository.existsByUsername(curp)) {
            throw new RuntimeException("El usuario con CURP ya está registrado");
        }

        // Validar aspirante existente no exista ya por CURP
        if (applicantRepository.existsByCurp(curp)) {
            throw new RuntimeException("Ya existe un usuario con esta CURP");
        }

        Long fichaExcel = Long.valueOf(getCellValue(row.getCell(0)));
        String fichaStr = fichaExcel.toString();

        // ** INTEGRACIÓN DE VALIDACIÓN DE VACANTES: **
        int currentYear = Year.now().getValue();
        long inscritos = applicantRepository.countByCareerAndAdmissionYear(career, currentYear);

        var vac = vacancyRepository
                .findByCareerAndAdmissionYear(career, currentYear)
                .orElseThrow(
                        () -> new RuntimeException("Vacantes no configuradas para " + career + " en " + currentYear));

        if (inscritos >= vac.getLimitCount()) {
            throw new RuntimeException("Cupo agotado para " + career);
        }
        // ** fin de la integración **

        // Crear usuario
        User user = new User();
        user.setUsername(fichaStr); // login = ficha
        user.setPassword(passwordEncoder.encode(curp)); // pass = curp
        user.setFullName(getCellValue(row.getCell(1)));
        user.setActive(true);
        user.setRoles(Set.of(applicantRole));
        user = userRepository.save(user);

        // Extraer fecha de examen y teléfono
        String examDateStr = getCellValue(row.getCell(6)); 
        String phone = getCellValue(row.getCell(7)); 

        LocalDateTime examDate = null;
        if (!examDateStr.isBlank()) {
            examDate = LocalDateTime.parse(
                    examDateStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }

        // Crear aspirante
        Applicant applicant = new Applicant();
        applicant.setUser(user);
        applicant.setFicha(fichaExcel);
        applicant.setCurp(curp);
        applicant.setCareer(career);
        applicant.setLocation(getCellValue(row.getCell(4)));
        applicant.setExamRoom(getCellValue(row.getCell(5)));
        applicant.setPhone(phone);
        applicant.setExamDate(examDate);
        applicant.setExamAssigned(false);
        applicant.setStatus("PENDING");
        applicant.setAdmissionYear(currentYear);
        applicantRepository.save(applicant);
    }

    private String getCellValue(Cell cell) {
        if (cell == null)
            return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((int) cell.getNumericCellValue());
            default:
                return "";
        }
    }
}
