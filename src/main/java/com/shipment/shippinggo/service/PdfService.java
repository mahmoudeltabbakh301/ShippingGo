package com.shipment.shippinggo.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.shipment.shippinggo.entity.Invoice;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.enums.OrderStatus;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.ArabicShaping;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfService {

    private static final String FONT_PATH = "d:/myproj/shippinggo/src/main/resources/fonts/Cairo-Regular.ttf";

    public byte[] generateInvoicePdf(Invoice invoice, List<Order> orders) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            // Load font using FontFactory which is more robust
            Font titleFont, headFont, bodyFont, smallFont;
            try {
                // Register the font from classpath
                String fontPath = getClass().getResource("/fonts/Cairo-Regular.ttf").toExternalForm();
                FontFactory.register(fontPath, "CairoFont");
                
                BaseFont baseFont = FontFactory.getFont("CairoFont", BaseFont.IDENTITY_H, BaseFont.EMBEDDED).getBaseFont();
                titleFont = new Font(baseFont, 18, Font.BOLD, Color.BLACK);
                headFont = new Font(baseFont, 12, Font.BOLD, Color.DARK_GRAY);
                bodyFont = new Font(baseFont, 10, Font.NORMAL, Color.BLACK);
                smallFont = new Font(baseFont, 8, Font.NORMAL, Color.GRAY);
            } catch (Exception e) {
                // Final fallback to Helvetica (Arabic won't display correctly, but PDF will generate)
                titleFont = new Font(Font.HELVETICA, 18, Font.BOLD, Color.BLACK);
                headFont = new Font(Font.HELVETICA, 12, Font.BOLD, Color.DARK_GRAY);
                bodyFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.BLACK);
                smallFont = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.GRAY);
            }

            // --- Header ---
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1, 1});

            PdfPCell companyCell = new PdfPCell();
            companyCell.setBorder(Rectangle.NO_BORDER);
            companyCell.addElement(new Phrase(shapeArabic("ShippingGo"), titleFont));
            companyCell.addElement(new Phrase(shapeArabic("نظام شحن متكامل"), bodyFont));
            headerTable.addCell(companyCell);

            PdfPCell invoiceInfoCell = new PdfPCell();
            invoiceInfoCell.setBorder(Rectangle.NO_BORDER);
            invoiceInfoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            invoiceInfoCell.addElement(new Phrase(shapeArabic("فاتورة رقم: " + invoice.getInvoiceNumber()), headFont));
            invoiceInfoCell.addElement(new Phrase(shapeArabic("التاريخ: " + invoice.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))), bodyFont));
            invoiceInfoCell.addElement(new Phrase(shapeArabic("الحالة: " + (invoice.getStatus().equals("PAID") ? "تم الدفع" : "لم يتم الدفع")), bodyFont));
            headerTable.addCell(invoiceInfoCell);

            document.add(headerTable);
            document.add(new Paragraph("\n"));

            // --- Billing Info ---
            PdfPTable billToTable = new PdfPTable(1);
            billToTable.setWidthPercentage(100);
            billToTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

            Organization org = invoice.getOrganization();
            PdfPCell billToCell = new PdfPCell();
            billToCell.setBorder(Rectangle.BOTTOM);
            billToCell.setPaddingBottom(10);
            billToCell.addElement(new Phrase(shapeArabic("إلى: " + org.getName()), headFont));
            billToCell.addElement(new Phrase(shapeArabic("العنوان: " + org.getAddress()), bodyFont));
            billToCell.addElement(new Phrase(shapeArabic("الهاتف: " + org.getPhone()), bodyFont));
            billToTable.addCell(billToCell);

            document.add(billToTable);
            document.add(new Paragraph("\n"));

            // --- Orders Table ---
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1, 3, 3, 2, 2});
            table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

            addTableCell(table, "#", headFont, Color.LIGHT_GRAY, Element.ALIGN_CENTER);
            addTableCell(table, "كود الشحنة", headFont, Color.LIGHT_GRAY, Element.ALIGN_CENTER);
            addTableCell(table, "المستلم", headFont, Color.LIGHT_GRAY, Element.ALIGN_CENTER);
            addTableCell(table, "التاريخ", headFont, Color.LIGHT_GRAY, Element.ALIGN_CENTER);
            addTableCell(table, "القيمة", headFont, Color.LIGHT_GRAY, Element.ALIGN_CENTER);

            int count = 1;
            for (Order order : orders) {
                addTableCell(table, String.valueOf(count++), bodyFont, null, Element.ALIGN_CENTER);
                addTableCell(table, order.getCode(), bodyFont, null, Element.ALIGN_CENTER);
                addTableCell(table, order.getRecipientName(), bodyFont, null, Element.ALIGN_CENTER);
                addTableCell(table, order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), bodyFont, null, Element.ALIGN_CENTER);
                addTableCell(table, order.getAmount().toString() + " ج.م", bodyFont, null, Element.ALIGN_CENTER);
            }

            document.add(table);

            // --- Total & QR Code ---
            document.add(new Paragraph("\n"));
            PdfPTable footerTable = new PdfPTable(2);
            footerTable.setWidthPercentage(100);
            footerTable.setWidths(new float[]{2, 1});
            footerTable.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

            PdfPCell totalCell = new PdfPCell();
            totalCell.setBorder(Rectangle.NO_BORDER);
            Phrase totalPhrase = new Phrase();
            totalPhrase.add(new Chunk(shapeArabic("الإجمالي المستحق: "), headFont));
            totalPhrase.add(new Chunk(invoice.getTotalAmount().toString() + " ج.م", titleFont));
            totalCell.addElement(totalPhrase);
            footerTable.addCell(totalCell);

            // Generate QR Code
            String qrData = "Invoice: " + invoice.getInvoiceNumber() + "\nTotal: " + invoice.getTotalAmount() + " EGP\nOrg: " + org.getName();
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 100, 100);
            
            try (ByteArrayOutputStream qrBaos = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrBaos);
                Image qrImage = Image.getInstance(qrBaos.toByteArray());
                PdfPCell qrCell = new PdfPCell(qrImage);
                qrCell.setBorder(Rectangle.NO_BORDER);
                qrCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                footerTable.addCell(qrCell);
            }

            document.add(footerTable);

            // --- Footer Notes ---
            Paragraph footerNotes = new Paragraph(shapeArabic("\nشكراً لتعاملكم مع ShippingGo\nيرجى تسوية المستحقات في غضون 7 أيام عمل"), smallFont);
            footerNotes.setAlignment(Element.ALIGN_CENTER);
            document.add(footerNotes);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }
    }

    private void addTableCell(PdfPTable table, String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(shapeArabic(text), font));
        if (bgColor != null) cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String shapeArabic(String text) {
        if (text == null) return "";
        try {
            ArabicShaping shaper = new ArabicShaping(ArabicShaping.LETTERS_SHAPE | ArabicShaping.DIGITS_EN2AN);
            String shaped = shaper.shape(text);
            Bidi bidi = new Bidi(shaped, Bidi.DIRECTION_DEFAULT_LEFT_TO_RIGHT);
            bidi.setReorderingMode(Bidi.REORDER_DEFAULT);
            return bidi.writeReordered(Bidi.DO_MIRRORING);
        } catch (Exception e) {
            return text;
        }
    }
}
