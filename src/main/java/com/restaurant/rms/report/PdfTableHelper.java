package com.restaurant.rms.report;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;

/**
 * Utility class for consistent iText5 table and cell styling across all PDF exports.
 *
 * <p>Centralising fonts and colour constants here means every report looks identical
 * without duplicating style logic across {@link PdfExportService} methods.</p>
 */
public final class PdfTableHelper {

    // ── Brand palette ─────────────────────────────────────────────────────────
    public static final BaseColor HEADER_BG    = new BaseColor(0x2C, 0x3E, 0x50); // dark navy
    public static final BaseColor ROW_EVEN_BG  = new BaseColor(0xEC, 0xF0, 0xF1); // light grey
    public static final BaseColor ROW_ODD_BG   = BaseColor.WHITE;
    public static final BaseColor ACCENT_COLOR  = new BaseColor(0xE7, 0x4C, 0x3C); // restaurant red

    // ── Fonts ─────────────────────────────────────────────────────────────────
    public static final Font FONT_TITLE;
    public static final Font FONT_SUBTITLE;
    public static final Font FONT_TABLE_HEADER;
    public static final Font FONT_BODY;
    public static final Font FONT_BODY_BOLD;

    static {
        try {
            FONT_TITLE        = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.BLACK);
            FONT_SUBTITLE     = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
            FONT_TABLE_HEADER = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            FONT_BODY         = new Font(Font.FontFamily.HELVETICA, 9,  Font.NORMAL, BaseColor.BLACK);
            FONT_BODY_BOLD    = new Font(Font.FontFamily.HELVETICA, 9,  Font.BOLD,   BaseColor.BLACK);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private PdfTableHelper() { /* static utility */ }

    // ── Table factory ─────────────────────────────────────────────────────────

    /**
     * Creates a full-width table with styled header cells.
     *
     * @param widths      relative column widths, e.g. {@code new float[]{2, 1, 1, 1}}
     * @param headerLabels column header text in the same order as {@code widths}
     */
    public static PdfPTable createTable(float[] widths, String... headerLabels) throws DocumentException {
        PdfPTable table = new PdfPTable(widths.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        table.setSpacingBefore(8f);
        table.setSpacingAfter(8f);
        table.setHeaderRows(1); // repeat header on each page

        for (String label : headerLabels) {
            table.addCell(headerCell(label));
        }
        return table;
    }

    /** Dark header cell with white bold text. */
    public static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TABLE_HEADER));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setBorderColor(HEADER_BG);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    /** Body cell with alternating row colour. */
    public static PdfPCell bodyCell(String text, int rowIndex) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, FONT_BODY));
        cell.setBackgroundColor(rowIndex % 2 == 0 ? ROW_EVEN_BG : ROW_ODD_BG);
        cell.setPadding(5);
        cell.setBorderColor(new BaseColor(0xCC, 0xCC, 0xCC));
        return cell;
    }

    /** Right-aligned body cell for numeric values. */
    public static PdfPCell numericCell(String text, int rowIndex) {
        PdfPCell cell = bodyCell(text, rowIndex);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        return cell;
    }

    /** Creates a paragraph with title styling. */
    public static Paragraph titleParagraph(String text) {
        Paragraph p = new Paragraph(text, FONT_TITLE);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(4);
        return p;
    }

    /** Creates a paragraph with subtitle styling. */
    public static Paragraph subtitleParagraph(String text) {
        Paragraph p = new Paragraph(text, FONT_SUBTITLE);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setSpacingAfter(2);
        return p;
    }

    /** A thin horizontal rule drawn as a full-width table. */
    public static PdfPTable horizontalRule() throws DocumentException {
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        rule.setSpacingBefore(4);
        rule.setSpacingAfter(8);
        PdfPCell line = new PdfPCell();
        line.setBorderColorBottom(ACCENT_COLOR);
        line.setBorderWidthBottom(1.5f);
        line.setBorderWidthTop(0);
        line.setBorderWidthLeft(0);
        line.setBorderWidthRight(0);
        line.setFixedHeight(1f);
        rule.addCell(line);
        return rule;
    }
}
