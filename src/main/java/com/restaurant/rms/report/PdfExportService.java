package com.restaurant.rms.report;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.restaurant.rms.dto.response.report.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates professional PDF reports using iText5 and streams them directly
 * to the provided {@link OutputStream} — typically {@code HttpServletResponse.getOutputStream()}.
 *
 * <p><strong>Streaming contract:</strong> all methods write incrementally; the JVM
 * never holds the entire PDF in memory. The caller must flush and close the stream.</p>
 */
@Service
@Slf4j
public class PdfExportService {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final DateTimeFormatter D_FMT   = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Value("${restaurant.name:Restaurant Management System}")
    private String restaurantName;

    // ──────────────────────────────────────────────────────────────────────────
    //  Daily sales report
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Generates a daily sales PDF with:
     * <ol>
     *   <li>Header block (restaurant name, report title, date, generated-at)</li>
     *   <li>Summary metrics table (orders, revenue, avg value)</li>
     *   <li>Revenue by category table with alternating shading</li>
     *   <li>Page numbers in the footer (via {@link PageNumberFooter})</li>
     * </ol>
     */
    public void exportSalesReportToPdf(DailySalesReport report,
                                        List<CategoryRevenueDTO> categories,
                                        List<TopMenuItemReport> topItems,
                                        OutputStream out) {
        try {
            Document doc = new Document(PageSize.A4, 36, 36, 54, 54);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageNumberFooter(restaurantName));
            doc.open();

            // ── Header ──────────────────────────────────────────────────────────
            addReportHeader(doc,
                    restaurantName,
                    "Daily Sales Report",
                    "Date: " + report.getDate().format(D_FMT),
                    "Generated: " + LocalDateTime.now().format(DT_FMT));

            // ── Summary metrics ──────────────────────────────────────────────────
            doc.add(new Paragraph("Summary", PdfTableHelper.FONT_BODY_BOLD));
            doc.add(Chunk.NEWLINE);

            PdfPTable summary = PdfTableHelper.createTable(
                    new float[]{3, 2},
                    "Metric", "Value");
            int row = 0;
            summary.addCell(PdfTableHelper.bodyCell("Total Orders", row));
            summary.addCell(PdfTableHelper.numericCell(str(report.getTotalOrders()), row++));
            summary.addCell(PdfTableHelper.bodyCell("Total Revenue", row));
            summary.addCell(PdfTableHelper.numericCell("$" + str(report.getTotalRevenue()), row++));
            summary.addCell(PdfTableHelper.bodyCell("Average Order Value", row));
            summary.addCell(PdfTableHelper.numericCell("$" + str(report.getAverageOrderValue()), row++));
            doc.add(summary);
            doc.add(PdfTableHelper.horizontalRule());

            // ── Revenue by category ──────────────────────────────────────────────
            if (categories != null && !categories.isEmpty()) {
                doc.add(new Paragraph("Revenue by Category", PdfTableHelper.FONT_BODY_BOLD));
                doc.add(Chunk.NEWLINE);

                PdfPTable catTable = PdfTableHelper.createTable(
                        new float[]{3, 1.5f, 2, 1.5f},
                        "Category", "Orders", "Revenue", "% of Total");
                int catRow = 0;
                for (CategoryRevenueDTO cat : categories) {
                    catTable.addCell(PdfTableHelper.bodyCell(cat.getCategoryName(), catRow));
                    catTable.addCell(PdfTableHelper.numericCell(str(cat.getOrderCount()), catRow));
                    catTable.addCell(PdfTableHelper.numericCell("$" + str(cat.getTotalRevenue()), catRow));
                    catTable.addCell(PdfTableHelper.numericCell(cat.getPercentageOfTotal() + "%", catRow++));
                }
                doc.add(catTable);
                doc.add(PdfTableHelper.horizontalRule());
            }

            // ── Top 10 items ─────────────────────────────────────────────────────
            if (topItems != null && !topItems.isEmpty()) {
                doc.add(new Paragraph("Top Menu Items", PdfTableHelper.FONT_BODY_BOLD));
                doc.add(Chunk.NEWLINE);

                PdfPTable itemTable = PdfTableHelper.createTable(
                        new float[]{3, 2, 1.5f, 2},
                        "Menu Item", "Category", "Qty Sold", "Revenue");
                int itemRow = 0;
                for (TopMenuItemReport item : topItems.stream().limit(10).toList()) {
                    itemTable.addCell(PdfTableHelper.bodyCell(item.getMenuItemName(), itemRow));
                    itemTable.addCell(PdfTableHelper.bodyCell(item.getCategoryName(), itemRow));
                    itemTable.addCell(PdfTableHelper.numericCell(str(item.getTotalQuantity()), itemRow));
                    itemTable.addCell(PdfTableHelper.numericCell("$" + str(item.getTotalRevenue()), itemRow++));
                }
                doc.add(itemTable);
            }

            doc.close();
            log.debug("PDF daily sales report generated for date={}", report.getDate());
        } catch (Exception ex) {
            log.error("Failed to generate daily sales PDF: {}", ex.getMessage(), ex);
            throw new RuntimeException("PDF generation failed: " + ex.getMessage(), ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Low-stock report
    // ──────────────────────────────────────────────────────────────────────────

    public void exportLowStockToPdf(List<LowStockReportDTO> items, OutputStream out) {
        try {
            Document doc = new Document(PageSize.A4, 36, 36, 54, 54);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            writer.setPageEvent(new PageNumberFooter(restaurantName));
            doc.open();

            addReportHeader(doc, restaurantName, "Low Stock Report",
                    "Items requiring reorder",
                    "Generated: " + LocalDateTime.now().format(DT_FMT));

            PdfPTable table = PdfTableHelper.createTable(
                    new float[]{3, 1.2f, 1.5f, 1.5f, 1.5f, 2},
                    "Item Name", "Unit", "Current Stock", "Threshold", "Deficit", "Supplier");

            int row = 0;
            for (LowStockReportDTO item : items) {
                table.addCell(PdfTableHelper.bodyCell(item.getItemName(), row));
                table.addCell(PdfTableHelper.bodyCell(item.getUnit(), row));
                table.addCell(PdfTableHelper.numericCell(str(item.getCurrentStock()), row));
                table.addCell(PdfTableHelper.numericCell(str(item.getReorderThreshold()), row));
                table.addCell(PdfTableHelper.numericCell(str(item.getDeficit()), row));
                table.addCell(PdfTableHelper.bodyCell(item.getSupplierName(), row++));
            }
            doc.add(table);
            doc.close();
            log.debug("PDF low-stock report generated: {} items", items.size());
        } catch (Exception ex) {
            log.error("Failed to generate low-stock PDF: {}", ex.getMessage(), ex);
            throw new RuntimeException("PDF generation failed: " + ex.getMessage(), ex);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Shared helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void addReportHeader(Document doc, String restaurant,
                                  String title, String... subtitles) throws DocumentException {
        doc.add(PdfTableHelper.titleParagraph(restaurant));
        doc.add(PdfTableHelper.titleParagraph(title));
        for (String s : subtitles) {
            doc.add(PdfTableHelper.subtitleParagraph(s));
        }
        doc.add(PdfTableHelper.horizontalRule());
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Page-number footer (iText5 page event)
    // ──────────────────────────────────────────────────────────────────────────

    private static final class PageNumberFooter extends PdfPageEventHelper {

        private final String restaurantName;
        private PdfTemplate pageCountTemplate;
        private BaseFont baseFont;

        PageNumberFooter(String restaurantName) {
            this.restaurantName = restaurantName;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
                pageCountTemplate = writer.getDirectContent().createTemplate(30, 16);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float x = (document.left() + document.right()) / 2;
            float y = document.bottom() - 10;

            cb.beginText();
            cb.setFontAndSize(baseFont, 8);
            cb.setTextMatrix(x - 50, y);
            cb.showText(restaurantName + "  |  Page " + writer.getPageNumber() + " of ");
            cb.endText();

            cb.addTemplate(pageCountTemplate, x + 20, y);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (pageCountTemplate != null && baseFont != null) {
                pageCountTemplate.beginText();
                pageCountTemplate.setFontAndSize(baseFont, 8);