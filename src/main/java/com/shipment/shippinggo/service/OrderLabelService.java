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
        Font normalFont = new Font(cairoBaseFont, 12, Font.NORMAL);
        Font largeFont = new Font(cairoBaseFont, 20, Font.BOLD);

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
            byte[] qrBytes = qrCodeService.generateQrCodeImage(order.getCode(), 150, 150);
            Image qrImage = Image.getInstance(qrBytes);
            qrImage.setAlignment(Element.ALIGN_CENTER);
            qrImage.scaleToFit(120, 120);

            PdfPCell qrCell = new PdfPCell(qrImage);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrCell.setPadding(10);
            table.addCell(qrCell);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Order Code
        PdfPCell codeCell = new PdfPCell();
        codeCell.setBorder(Rectangle.NO_BORDER);
        codeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        codeCell.setPhrase(new Phrase(order.getCode(), FontFactory.getFont(FontFactory.COURIER_BOLD, 18)));
        table.addCell(codeCell);

        // Spacer
        PdfPCell spacer = new PdfPCell(new Phrase(" "));
        spacer.setBorder(Rectangle.NO_BORDER);
        table.addCell(spacer);

        // 4. Recipient Details
        addDetailRow(table, "المستلم: " + order.getRecipientName(), normalFont);
        addDetailRow(table, "رقم الهاتف: " + order.getRecipientPhone(), normalFont);

        if (order.getRecipientAddress() != null && !order.getRecipientAddress().isEmpty()) {
            addDetailRow(table, "العنوان: " + order.getRecipientAddress(), normalFont);
        }

        // 5. Amount
        PdfPCell amountCell = new PdfPCell();
        amountCell.setBorder(Rectangle.BOX);
        amountCell.setBorderWidth(2);
        amountCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        amountCell.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        amountCell.setPadding(10);
        amountCell.setPhrase(new Phrase(order.getAmount() + " ج.م", largeFont));
        table.addCell(amountCell);

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
