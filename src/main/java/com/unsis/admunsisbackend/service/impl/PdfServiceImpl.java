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

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

/**
 * Servicio para generar reportes PDF a partir de la tabla de aspirantes.
 */
@Service
public class PdfServiceImpl implements PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfServiceImpl.class);

    @Autowired
    private ApplicantRepository applicantRepository;

    /**
     * Genera un PDF con la lista de aspirantes registrados en el sistema.
     */
    @Override
    @Transactional
    public PdfResponse generateApplicantsReport() {
        PdfResponse response = new PdfResponse();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Crear documento PDF
            Document document = new Document(PageSize.A4.rotate(), 36, 36, 54, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Título
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Paragraph title = new Paragraph("Reporte de Aspirantes - UNSIS", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Obtener datos
            List<Applicant> applicants = applicantRepository.findAll();

            if (applicants.isEmpty()) {
                document.add(new Paragraph("No hay aspirantes registrados."));
            } else {
                // Crear tabla con encabezados
                PdfPTable table = new PdfPTable(7);
                table.setWidthPercentage(100);
                table.setSpacingBefore(10f);
                table.setSpacingAfter(10f);
                table.setHeaderRows(1);

                addTableHeader(table, new String[]{
                    "Ficha", "Nombre completo", "CURP", "Carrera", "Lugar", "Estado","Asistió"
                });

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                // Llenar filas
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

            // Pie de página
            Paragraph footer = new Paragraph(
                "Generado automáticamente el " +
                java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC)
            );
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);

            document.close();

            response.setSuccess(true);
            response.setMessage("Reporte PDF generado correctamente.");
            response.setFileBytes(baos.toByteArray());
            response.setFileName("reporte_aspirantes.pdf");

        } catch (Exception e) {
            logger.error("Error al generar el PDF: {}", e.getMessage(), e);
            response.setSuccess(false);
            response.setMessage("Error al generar el PDF: " + e.getMessage());
        }

        return response;
    }

    /**
     * Agrega el encabezado de la tabla con estilo.
     */
    private void addTableHeader(PdfPTable table, String[] headers) {
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
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
