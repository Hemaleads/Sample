package com.juvarya.nivaas.customer.service.impl;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.juvarya.nivaas.customer.client.AccessMgmtClient;
import com.juvarya.nivaas.commonservice.dto.UserDTO;
import com.juvarya.nivaas.customer.model.ApartmentDebitHistoryModel;
import com.juvarya.nivaas.customer.model.NivaasApartmentModel;
import com.juvarya.nivaas.customer.model.SocietyDue;
import com.juvarya.nivaas.customer.repository.ApartmentDebitHistoryRepository;
import com.juvarya.nivaas.customer.repository.SocietyDueRepository;
import com.juvarya.nivaas.customer.service.NivaasFlatService;
import com.juvarya.nivaas.customer.service.ReportService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Month;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ApartmentDebitHistoryRepository apartmentDebitHistoryRepository;

    @Autowired
    private SocietyDueRepository societyDueRepository;

    @Autowired
    private NivaasFlatService flatService;

    @Autowired
    private AccessMgmtClient accessMgmtClient;

    private static final String watermark = "NIVAAS";

    @Override
    public byte[] generateDebitReport(final NivaasApartmentModel nivaasApartmentModel, final int year, final int month) {
    	 log.info("Generating debit report for apartment {} for {} - {}", nivaasApartmentModel.getId(), Month.of(month), year);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(byteArrayOutputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Add title
        document.add(new Paragraph(nivaasApartmentModel.getName()).setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(16));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        //Add debit history
        document.add(new Paragraph("Debit Report for " + Month.of(month) + " - " + year).setBold().setFontSize(12));
        document.add(new Paragraph(" "));

        document.add(createDebitReportTable(nivaasApartmentModel.getId(), year, month));

        // Add watermark
        try {
            addWatermarkAndBorder(pdfDoc);
        } catch (IOException e) {
        	 log.warn("Failed to add watermark to debit report: {}", e.getMessage());
            //TODO: log failed to add watermark
        }

        document.close();
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public byte[] generateCreditReport(final NivaasApartmentModel nivaasApartmentModel, final int year, final int month) {
    	log.info("Generating credit report for apartment {} for {} - {}", nivaasApartmentModel.getId(), Month.of(month), year);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(byteArrayOutputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Add title
        document.add(new Paragraph(nivaasApartmentModel.getName()).setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(16));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        //Add credit history
        document.add(new Paragraph("Credit Report for " + Month.of(month) + " - " + year).setBold().setFontSize(12));
        document.add(new Paragraph(" "));

        document.add(createCreditReportTable(nivaasApartmentModel.getId(), year, month));

        // Add watermark
        try {
            addWatermarkAndBorder(pdfDoc);
        } catch (IOException e) {
        	 log.warn("Failed to add watermark to credit report: {}", e.getMessage());
            //TODO: log failed to add watermark
        }

        document.close();
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public byte[] generateMonthlyReport(final NivaasApartmentModel nivaasApartmentModel, final int year, final int month) {
    	log.info("Generating monthly report for apartment {} for {} - {}", nivaasApartmentModel.getId(), Month.of(month), year);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(byteArrayOutputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // Add title
        document.add(new Paragraph(nivaasApartmentModel.getName()).setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(16));
        document.add(new Paragraph(" "));
        document.add(new Paragraph(" "));

        document.add(new Paragraph("Maintenance report for " + Month.of(month) + " - " + year).setBold().setFontSize(12));
        document.add(new Paragraph(" "));

        //Add credit history
        document.add(new Paragraph("Credit Report for " + Month.of(month) + " - " + year).setBold().setFontSize(12));
        document.add(new Paragraph(" "));

        document.add(createCreditReportTable(nivaasApartmentModel.getId(), year, month));

        //Add debit history
        document.add(new Paragraph("Debit report for " + Month.of(month) + " - " + year).setBold().setFontSize(12));
        document.add(new Paragraph(" "));

        document.add(createDebitReportTable(nivaasApartmentModel.getId(), year, month));

        // Add watermark
        try {
            addWatermarkAndBorder(pdfDoc);
        } catch (IOException e) {
        	log.warn("Failed to add watermark to monthly report: {}", e.getMessage());
            //TODO: log failed to add watermark
        }

        document.close();
        return byteArrayOutputStream.toByteArray();
    }

    private Table createDebitReportTable(final Long apartmentId, final int year, final int month) {
    	log.debug("Creating debit report table for apartment {} for {} - {}", apartmentId, Month.of(month), year);
        // Add table with border
        float[] columnWidths = {1, 2, 6, 2, 2, 2};
        Table table = new Table(columnWidths);
        table.useAllAvailableWidth();

        // Add table header
        table.addHeaderCell(createCell("S.No", true, false));
        table.addHeaderCell(createCell("Transaction Date", true, false));
        table.addHeaderCell(createCell("Type", true, false));
        table.addHeaderCell(createCell("Description", true, false));
        table.addHeaderCell(createCell("Amount", true, false));
        table.addHeaderCell(createCell("Transaction By", true, false));

        List<ApartmentDebitHistoryModel> debits = apartmentDebitHistoryRepository.findByApartmentIdAndYearAndMonth(apartmentId, year, month);
        // Add table rows
        int counter = 1;
        double totalAmount = 0;
        for (ApartmentDebitHistoryModel debit : debits) {
            UserDTO user = accessMgmtClient.getUserById(debit.getUpdatedBy());
            table.addCell(createCell(String.valueOf(counter), false, false));
            table.addCell(createCell(debit.getTransactionDate().toString(), false, false));
            table.addCell(createCell(null != debit.getType() ? debit.getType().toString() : "", false, false));
            table.addCell(createCell(debit.getDescription(), false, false));
            table.addCell(createCell(debit.getAmount().toString(), false, false));
            table.addCell(createCell(null != user ? user.getFullName() : "", false, false));
            counter++;
            totalAmount += debit.getAmount();
        }
        if (counter > 1) {
            table.addFooterCell(createCell("", false, false));
            table.addFooterCell(createCell("Total", false, true));
            table.addFooterCell(createCell("", false, false));
            table.addFooterCell(createCell("", false, false));
            table.addFooterCell(createCell(String.valueOf(totalAmount), false, true));
        }
        return table;
    }

    private Table createCreditReportTable(final Long apartmentId, final int year, final int month) {
    	log.debug("Creating credit report table for apartment {} for {} - {}", apartmentId, Month.of(month), year);
        // Add table with border
        float[] columnWidths = {1, 2, 2, 2, 2};
        Table table = new Table(columnWidths);
        table.useAllAvailableWidth();

        // Add table header
        table.addHeaderCell(createCell("S.No", true, false));
        table.addHeaderCell(createCell("Flat Number", true, false));
        table.addHeaderCell(createCell("Due Date", true, false));
        table.addHeaderCell(createCell("Amount", true, false));
        table.addHeaderCell(createCell("Status", true, false));

        List<SocietyDue> credits = societyDueRepository.findByApartmentIdAndYearAndMonth(apartmentId, year, month);
        Map<Long, String> flatsMapByApartmentMap = flatService.getFlatsMapByApartment(apartmentId);
        // Add table rows
        int counter = 1;
        double totalAmount = 0;
        for (SocietyDue credit : credits) {
            final String flatNumber = flatsMapByApartmentMap.get(credit.getFlatId());
            table.addCell(createCell(String.valueOf(counter), false, false));
            table.addCell(createCell(null != flatNumber ? flatNumber : "", false, false));
            table.addCell(createCell(credit.getDueDate().toString(), false, false));
            table.addCell(createCell(null != credit.getCost()? String.valueOf(credit.getCost()) : "0", false, false));
            table.addCell(createCell(credit.getStatus().name(), false, false));
            counter++;
            totalAmount += credit.getCost();
        }
        if (counter > 1) {
            table.addFooterCell(createCell("", false, false));
            table.addFooterCell(createCell("Total", false, true));
            table.addFooterCell(createCell("", false, false));
            table.addFooterCell(createCell(String.valueOf(totalAmount), false, true));
        }
        return table;
    }

    private Cell createCell(String content, boolean isHeader, boolean isFooter) {
        Cell cell = new Cell().add(new Paragraph(content));
        if (isHeader) {
            cell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            cell.setBold();
        } else if (isFooter) {
            cell.setBold();
        }
        cell.setBorder(Border.NO_BORDER);
        cell.setTextAlignment(TextAlignment.CENTER);
        return cell;
    }

    private void addWatermarkAndBorder(PdfDocument pdfDoc) throws IOException {
    	 log.debug("Adding watermark and border to PDF document");
        PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        int numberOfPages = pdfDoc.getNumberOfPages();
        for (int i = 1; i <= numberOfPages; i++) {
            PdfPage page = pdfDoc.getPage(i);
            PdfCanvas pdfCanvas = new PdfCanvas(page);

            // Draw border
            float x = page.getPageSize().getLeft();
            float y = page.getPageSize().getBottom();
            float width = page.getPageSize().getWidth();
            float height = page.getPageSize().getHeight();

            pdfCanvas.saveState();
            pdfCanvas.setLineWidth(2);
            pdfCanvas.setStrokeColor(ColorConstants.BLACK);
            pdfCanvas.rectangle(x + 10, y + 10, width - 20, height - 20);
            pdfCanvas.stroke();
            pdfCanvas.restoreState();

            //Add watermark
            pdfCanvas.saveState();
            pdfCanvas.setFillColor(ColorConstants.LIGHT_GRAY);
            Canvas canvas = new Canvas(pdfCanvas, new Rectangle(44, 44));
            canvas.setFont(font);
            canvas.setFontSize(60);
            canvas.showTextAligned(watermark, 298, 421, TextAlignment.CENTER, VerticalAlignment.MIDDLE, 45);
            canvas.close();
            pdfCanvas.restoreState();
        }
    }
}
