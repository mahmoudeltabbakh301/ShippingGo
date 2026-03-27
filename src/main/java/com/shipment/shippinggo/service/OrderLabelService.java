package com.shipment.shippinggo.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.BaseFont;
import com.shipment.shippinggo.entity.Order;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class OrderLabelService {

    private final QrCodeService qrCodeService;

    public OrderLabelService(QrCodeService qrCodeService) {
        this.qrCodeService = qrCodeService;
    }

    // إنشاء ملصق (بوليصة) شحن لطلب واحد بصيغة PDF
    public byte[] generateOrderLabel(Order order) throws IOException, DocumentException {
        // Label size: 10cm x 15cm (approx 4x6 inches)
        Rectangle labelSize = new Rectangle(283, 425);
        Document document = new Document(labelSize, 10, 10, 10, 10);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);
        document.open();

        addOrderToDocument(document, order);

        document.close();
        return out.toByteArray();
    }

    // إنشاء بوليصات شحن مجمعة لعدة طلبات في ملف PDF واحد
    public byte[] generateBulkLabels(List<Order> orders) throws IOException, DocumentException {
        // Label size: 10cm x 15cm
        Rectangle labelSize = new Rectangle(283, 425);
        Document document = new Document(labelSize, 10, 10, 10, 10);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);
        document.open();

        for (int i = 0; i < orders.size(); i++) {
            addOrderToDocument(document, orders.get(i));
            if (i < orders.size() - 1) {
                document.newPage();
            }
        }

        document.close();
        return out.toByteArray();
    }

    // دالة مساعدة لإضافة تفاصيل الطلب (QR كود، اسم الشركة، بيانات المستلم) إلى وثيقة الـ PDF
    private void addOrderToDocument(Document document, Order order) throws DocumentException, IOException {
        // Font setup (Cairo supports Arabic)
        BaseFont cairoBaseFont;
        try {
            cairoBaseFont = BaseFont.createFont("src/main/resources/fonts/Cairo-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            // Fallback if running compiled jar or not found
            System.err.println("Could not load Cairo font from src, falling back to HELVETICA");
            cairoBaseFont = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        }
        
        Font titleFont = new Font(cairoBaseFont, 16, Font.BOLD);
        Font normalFont = new Font(cairoBaseFont, 14, Font.NORMAL);
        Font largeFont = new Font(cairoBaseFont, 24, Font.BOLD);
        Font hugeFont = new Font(cairoBaseFont, 28, Font.BOLD);

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);

        // 1. Company Name (Header)
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBorder(Rectangle.NO_BORDER);
        headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        headerCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        headerCell.setPhrase(new Phrase(order.getOwnerOrganization().getName(), titleFont));
        table.addCell(headerCell);

        // 2. QR Code
        try {
            byte[] qrBytes = qrCodeService.generateQrCodeImage(order.getCode(), 250, 250);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            qrImage.scaleToFit(220, 220);

            PdfPCell qrCell = new PdfPCell(qrImage);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrCell.setPadding(10);
            table.addCell(qrCell);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Phone Number
        PdfPCell phoneCell = new PdfPCell(new Phrase("هاتف: " + order.getRecipientPhone(), hugeFont));
        phoneCell.setBorder(Rectangle.NO_BORDER);
        phoneCell.setPaddingTop(10);
        phoneCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        phoneCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(phoneCell);

        document.add(table);
    }

    private void addDetailRow(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(5);
        cell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT); // Since it's RTL, Left alignment in LTR becomes Right alignment functionally, or we can use ALIGN_RIGHT
        cell.setHorizontalAlignment(Element.ALIGN_LEFT); // Using ALIGN_LEFT works correctly with RTL run direction
        table.addCell(cell);
    }
}
