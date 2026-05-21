package com.unsis.admunsisbackend.service.impl;

import com.unsis.admunsisbackend.dto.PdfResponse;
import com.unsis.admunsisbackend.model.Applicant;
import com.unsis.admunsisbackend.repository.ApplicantRepository;
import com.unsis.admunsisbackend.service.PdfService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

// Imports para iText (PDF)
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

// Imports para Apache POI (Excel)
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFColor; // 👈 Importante para colores personalizados en Excel

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio para generar reportes unificados (PDF + Excel) en un ZIP con colores institucionales.
 */
@Service
public class PdfServiceImpl implements PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfServiceImpl.class);

    @Autowired
    private ApplicantRepository applicantRepository;

    @Override
    @Transactional
    public PdfResponse generateApplicantsReport() {
        PdfResponse response = new PdfResponse();
        List<Applicant> applicants = applicantRepository.findAll();

        byte[] pdfBytes = null;
        byte[] excelBytes = null;

        // --- GENERACIÓN DEL PDF ---
        try (ByteArrayOutputStream baosPdf = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
            PdfWriter.getInstance(document, baosPdf);
            document.open();

            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
            Paragraph title = new Paragraph("Reporte de Aspirantes - UNSIS", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            if (applicants.isEmpty()) {
                document.add(new Paragraph("No hay aspirantes registrados."));
            } else {
                PdfPTable table = new PdfPTable(7);
                table.setWidthPercentage(100);
                table.setSpacingBefore(10f);
                table.setSpacingAfter(10f);
                table.setHeaderRows(1);

                addTableHeader(table, new String[]{
                    "Ficha", "Nombre completo", "CURP", "Carrera", "Lugar", "Estado", "Asistió"
                });

                for (Applicant a : applicants) {
                    table.addCell(String.valueOf(a.getFicha()));
                    table.addCell(a.getUser() != null ? a.getUser().getFullName() : "N/D");
                    table.addCell(a.getCurp());
                    table.addCell(a.getCareer());
                    table.addCell(a.getLocation());
                    table.addCell(a.getStatus());
                    table.addCell(a.getAttendanceStatus());
                }
                document.add(table);
            }

            Paragraph footer = new Paragraph(
                "Generado automáticamente el " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 8, com.itextpdf.text.Font.ITALIC)
            );
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
            pdfBytes = baosPdf.toByteArray();

        } catch (Exception e) {
            logger.error("Error al construir el PDF en el servicio: {}", e.getMessage(), e);
        }

        // --- GENERACIÓN DEL EXCEL ---
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baosExcel = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Aspirantes");

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);

            // Convertimos el color hexadecimal #6a1b1b a bytes para Apache POI
            byte[] rgbGunder = new byte[]{(byte) 106, (byte) 27, (byte) 27};
            XSSFColor customColor = new XSSFColor(rgbGunder, null);
            
            headerCellStyle.setFillForegroundColor(customColor);
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);

            // Crear Fila de Encabezados
            String[] columns = {"Ficha", "Nombre completo", "CURP", "Carrera", "Lugar", "Estado", "Asistió"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Llenar Filas con Datos
            int rowNum = 1;
            for (Applicant a : applicants) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getFicha());
                row.createCell(1).setCellValue(a.getUser() != null ? a.getUser().getFullName() : "N/D");
                row.createCell(2).setCellValue(a.getCurp());
                row.createCell(3).setCellValue(a.getCareer());
                row.createCell(4).setCellValue(a.getLocation());
                row.createCell(5).setCellValue(a.getStatus());
                row.createCell(6).setCellValue(a.getAttendanceStatus());
            }

            // Autoajustar las columnas
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baosExcel);
            excelBytes = baosExcel.toByteArray();

        } catch (Exception e) {
            logger.error("Error al construir el Excel en el servicio: {}", e.getMessage(), e);
        }

        // --- RESPUESTA UNIFICADA ---
        if (pdfBytes != null && excelBytes != null) {
            response.setSuccess(true);
            response.setMessage("Reportes PDF y Excel empaquetados con éxito.");
            response.setPdfBytes(pdfBytes);     
            response.setExcelBytes(excelBytes); 
            
            response.setFileBytes(pdfBytes);
            response.setFileName("reportes_admision.zip");
        } else {
            response.setSuccess(false);
            response.setMessage("Error interno al procesar uno o ambos archivos del reporte.");
        }

        return response;
    }

    private void addTableHeader(PdfPTable table, String[] headers) {
        com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD, BaseColor.WHITE);
        
        // 106, 27, 27 equivalen al hexadecimal #6A1B1B en RGB
        BaseColor headerColor = new BaseColor(106, 27, 27);

        for (String h : headers) {
            PdfPCell headerCell = new PdfPCell(new Phrase(h, headerFont));
            headerCell.setBackgroundColor(headerColor);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setPadding(5);
            table.addCell(headerCell);
        }
    }
}