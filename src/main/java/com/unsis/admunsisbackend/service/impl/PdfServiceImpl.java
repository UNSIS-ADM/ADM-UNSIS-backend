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

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

// Importaciones para iText (PDF)
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.pdf.*;

// Importaciones para Apache POI (Excel)
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFColor;

/**
 * Servicio unificado para generar reportes en PDF y Excel en una sola petición.
 */
@Service
public class PdfServiceImpl implements PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfServiceImpl.class);

    @Autowired
    private ApplicantRepository applicantRepository;

    /**
     * Genera un único JSON que contiene tanto el reporte PDF como el Excel mapeados en Base64.
     */
    @Override
    @Transactional
    public PdfResponse generateApplicantsReport() {
        PdfResponse response = new PdfResponse();
        List<Applicant> applicants = applicantRepository.findAll();

        byte[] pdfResultBytes = null;
        byte[] excelResultBytes = null;

        // ==========================================
        // 1. GENERACIÓN DEL ARCHIVO PDF
        // ==========================================
        try (ByteArrayOutputStream pdfBaos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
            PdfWriter.getInstance(document, pdfBaos);
            document.open();

            // Título
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
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

                float[] columnWidths = {1f, 3f, 2.2f, 2.5f, 1.5f, 1.3f, 1f};
                table.setWidths(columnWidths);

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
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC)
            );
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();
            pdfResultBytes = pdfBaos.toByteArray();

        } catch (Exception e) {
            logger.error("Error crítico al fabricar el PDF: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setMessage("Error al generar sección PDF: " + e.getMessage());
            return response;
        }

        // ==========================================
        // 2. GENERACIÓN DEL ARCHIVO EXCEL
        // ==========================================
        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream excelBaos = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Aspirantes");

            // Estilo para el encabezado (#6a1b1b Guinda)
            CellStyle headerStyle = workbook.createCellStyle();
            byte[] guindaRGB = new byte[]{(byte) 106, (byte) 27, (byte) 27};
            XSSFColor xssfGuinda = new XSSFColor(guindaRGB, null);
            
            headerStyle.setFillForegroundColor(xssfGuinda);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            String[] headers = {"Ficha", "Nombre completo", "CURP", "Carrera", "Lugar", "Estado", "Asistió"};
            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(25);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            for (Applicant a : applicants) {
                Row row = sheet.createRow(rowNum++);
                row.setHeightInPoints(18);

                row.createCell(0).setCellValue(a.getFicha());
                row.createCell(1).setCellValue(a.getUser() != null ? a.getUser().getFullName() : "N/D");
                row.createCell(2).setCellValue(a.getCurp());
                row.createCell(3).setCellValue(a.getCareer());
                row.createCell(4).setCellValue(a.getLocation());
                row.createCell(5).setCellValue(a.getStatus());
                row.createCell(6).setCellValue(a.getAttendanceStatus());

                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(excelBaos);
            excelResultBytes = excelBaos.toByteArray();

        } catch (Exception e) {
            logger.error("Error crítico al fabricar el Excel: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setMessage("Error al generar sección Excel: " + e.getMessage());
            return response;
        }

        // ==========================================
        // 3. ARMADO DE LA RESPUESTA UNIFICADA JSON
        // ==========================================
        response.setSuccess(true);
        response.setMessage("Reportes PDF y Excel empaquetados con éxito.");
        response.setPdfBytes(pdfResultBytes);
        response.setExcelBytes(excelResultBytes);
        response.setPdfFileName("reporte_aspirantes.pdf");
        response.setExcelFileName("reporte_aspirantes.xlsx");

        return response;
    }

    private void addTableHeader(PdfPTable table, String[] headers) {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
        BaseColor headerColor = new BaseColor(106, 27, 27);

        for (String h : headers) {
            PdfPCell headerCell = new PdfPCell(new Phrase(h, headerFont));
            headerCell.setBackgroundColor(headerColor);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            headerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            headerCell.setPadding(6);
            table.addCell(headerCell);
        }
    }
}