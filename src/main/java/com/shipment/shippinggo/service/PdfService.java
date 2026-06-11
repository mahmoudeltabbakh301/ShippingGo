package com.shipment.shippinggo.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.shipment.shippinggo.entity.Invoice;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class PdfService {

    private static final Logger log = LoggerFactory.getLogger(PdfService.class);

    private final StorageService storageService;

    public PdfService(StorageService storageService) {
        this.storageService = storageService;
    }

    private BaseFont cachedCairoBaseFont;

    private synchronized BaseFont getCairoBaseFont() throws Exception {
        if (cachedCairoBaseFont == null) {
            byte[] fontBytes = null;
            try {
                // محاولة استخدام خط Cairo المرفق
                try (java.io.InputStream is = getClass().getResourceAsStream("/fonts/Cairo-Regular.ttf")) {
                    if (is != null) {
                        fontBytes = is.readAllBytes();
                        cachedCairoBaseFont = BaseFont.createFont("CairoFont.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
                        log.info("Cairo font loaded successfully from resources.");
                        return cachedCairoBaseFont;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to load or parse Cairo font from resources (may be incompatible with OpenPDF). Trying Amiri: {}", e.getMessage());
            }

            // إذا فشل Cairo، نقوم بتحميل خط Amiri من GitHub مباشرة وتخزينه في الذاكرة
            log.info("Downloading Amiri font from GitHub Google Fonts mirror...");
            String url = "https://raw.githubusercontent.com/google/fonts/main/ofl/amiri/Amiri-Regular.ttf";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            try (java.io.InputStream in = conn.getInputStream()) {
                fontBytes = in.readAllBytes();
                cachedCairoBaseFont = BaseFont.createFont("Amiri.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontBytes, null);
                log.info("Amiri font downloaded and loaded successfully.");
            }
        }
        return cachedCairoBaseFont;
    }

    /**
     * معالجة النص العربي بالكامل عبر ICU4J:
     * 1. ArabicShaping — ربط الحروف (initial, medial, final, isolated)
     * 2. Bidi reordering — ترتيب الأحرف بصرياً (من اليمين لليسار → ترتيب بصري LTR)
     *
     * بعد المعالجة، النص جاهز للعرض مباشرة بدون الاعتماد على OpenPDF للتشكيل أو الاتجاه.
     */
    private String processArabicText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        try {
            // الخطوة 1: ربط الحروف العربية (shaping)
            ArabicShaping shaping = new ArabicShaping(ArabicShaping.LETTERS_SHAPE);
            String shaped = shaping.shape(text);

            // الخطوة 2: ترتيب بصري عبر Bidi (يتعامل مع النص المختلط: عربي + أرقام + إنجليزي)
            Bidi bidi = new Bidi(shaped.length(), 0);
            bidi.setPara(shaped, Bidi.LEVEL_DEFAULT_RTL, null);
            return bidi.writeReordered(Bidi.DO_MIRRORING);
        } catch (ArabicShapingException e) {
            log.error("Arabic text processing failed for: [{}]", text, e);
            return text;
        }
    }

    /**
     * إنشاء الخطوط — يرمي RuntimeException إذا فشل تحميل الخط العربي (لا يسقط على Helvetica).
     */
    private Font[] createFonts() {
        try {
            BaseFont baseFont = getCairoBaseFont();
            return new Font[]{
                    new Font(baseFont, 16, Font.BOLD, Color.BLACK),      // [0] titleFont
                    new Font(baseFont, 11, Font.BOLD, Color.DARK_GRAY),  // [1] headFont
                    new Font(baseFont, 10, Font.NORMAL, Color.BLACK),    // [2] bodyFont
                    new Font(baseFont, 8, Font.NORMAL, Color.GRAY),      // [3] smallFont
                    new Font(baseFont, 9, Font.NORMAL, new Color(100, 100, 100)) // [4] labelFont
            };
        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to create fonts with Cairo. Arabic PDF generation is impossible!", e);
        }
    }

    public byte[] generateInvoicePdf(Invoice invoice, List<Order> orders, Organization printingOrg) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A5);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            Font[] fonts = createFonts();
            Font titleFont = fonts[0], headFont = fonts[1], bodyFont = fonts[2],
                    smallFont = fonts[3], labelFont = fonts[4];

            Order order = orders.get(0);

            // === Header: Logo (right) | Org Name (center) | QR Code (left) ===
            // استخدام المنظمة الطابعة (ليست المالكة)
            Organization headerOrg = printingOrg != null ? printingOrg : order.getOwnerOrganization();
            addInvoiceHeader(document, headerOrg, invoice, order, titleFont);
            document.add(new Paragraph("\n"));

            // === الرقم المسلسل و الكود ===
            PdfPTable codeTable = createRtlTable(2, new float[]{1, 1});
            addLabelValueCell(codeTable, "الرقم المسلسل", order.getSequenceNumber() != null ? order.getSequenceNumber() : "-", labelFont, bodyFont);
            addLabelValueCell(codeTable, "الكود", order.getCode() != null ? order.getCode() : "-", labelFont, bodyFont);
            document.add(codeTable);
            document.add(new Paragraph("\n"));

            // === بيانات المستلم ===
            addArabicParagraph(document, "بيانات المستلم", headFont, Element.ALIGN_RIGHT);

            PdfPTable recipientTable = createRtlTable(2, new float[]{1, 1});
            addLabelValueCell(recipientTable, "المرسل إليه *", order.getRecipientName() != null ? order.getRecipientName() : "-", labelFont, bodyFont);
            addLabelValueCell(recipientTable, "رقم الهاتف", order.getRecipientPhone() != null ? order.getRecipientPhone() : "-", labelFont, bodyFont);
            document.add(recipientTable);

            // === المحافظة و الشركة ===
            PdfPTable govTable = createRtlTable(2, new float[]{1, 1});
            String govName = order.getGovernorate() != null ? order.getGovernorate().getArabicName() : "-";
            addLabelValueCell(govTable, "المحافظة", govName, labelFont, bodyFont);
            addLabelValueCell(govTable, "الشركة", order.getCompanyName() != null ? order.getCompanyName() : "-", labelFont, bodyFont);
            document.add(govTable);

            // === المنطقة (إذا وُجدت) ===
            if (order.getDistrict() != null && !order.getDistrict().isEmpty()) {
                PdfPTable districtTable = createRtlTable(1, null);
                addLabelValueCell(districtTable, "المنطقة", order.getDistrict(), labelFont, bodyFont);
                document.add(districtTable);
            }

            // === العنوان التفصيلي ===
            String fullAddress = buildFullAddress(order);
            PdfPTable addressTable = createRtlTable(1, null);
            addLabelValueCell(addressTable, "العنوان التفصيلي", fullAddress, labelFont, bodyFont);
            document.add(addressTable);
            document.add(new Paragraph("\n"));

            // === تفاصيل الشحنة ===
            addArabicParagraph(document, "تفاصيل الشحنة", headFont, Element.ALIGN_RIGHT);

            PdfPTable shippingTable = createRtlTable(3, new float[]{1, 1, 1});
            String qty = order.getQuantity() != null ? order.getQuantity().toString() : "-";
            String shipPrice = order.getShippingPrice() != null ? order.getShippingPrice().toString() : "-";
            String orderPrice = order.getOrderPrice() != null ? order.getOrderPrice().toString() : "-";
            addLabelValueCell(shippingTable, "الكمية", qty, labelFont, bodyFont);
            addLabelValueCell(shippingTable, "سعر الشحن (ج.م)", shipPrice, labelFont, bodyFont);
            addLabelValueCell(shippingTable, "سعر الأوردر (ج.م)", orderPrice, labelFont, bodyFont);
            document.add(shippingTable);
            document.add(new Paragraph("\n"));

            // === المبلغ الإجمالي ===
            BigDecimal totalAmount = order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO;
            addTotalAmountSection(document, totalAmount, labelFont);

            // رقم الفاتورة
            document.add(new Paragraph("\n"));
            addArabicParagraph(document, safeText(invoice.getInvoiceNumber()), smallFont, Element.ALIGN_CENTER);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }
    }

    /**
     * طباعة مجمعة: كل فاتورة في صفحة منفصلة
     */
    public byte[] generateBulkInvoicePdf(List<Invoice> invoices, Organization printingOrg) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A5);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            for (int i = 0; i < invoices.size(); i++) {
                if (i > 0) {
                    document.newPage();
                }
                Invoice invoice = invoices.get(i);
                Order order = invoice.getOrder();
                addSingleInvoicePage(document, writer, invoice, order, printingOrg);
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating bulk PDF: " + e.getMessage(), e);
        }
    }

    private void addSingleInvoicePage(Document document, PdfWriter writer, Invoice invoice, Order order, Organization printingOrg) throws Exception {
        Font[] fonts = createFonts();
        Font titleFont = fonts[0], headFont = fonts[1], bodyFont = fonts[2],
                smallFont = fonts[3], labelFont = fonts[4];

        // === Header: Logo (right) | Org Name (center) | QR Code (left) ===
        // استخدام المنظمة الطابعة (ليست المالكة)
        Organization headerOrg = printingOrg != null ? printingOrg : order.getOwnerOrganization();
        addInvoiceHeader(document, headerOrg, invoice, order, titleFont);
        document.add(new Paragraph("\n"));

        // الرقم المسلسل و الكود
        PdfPTable codeTable = createRtlTable(2, new float[]{1, 1});
        addLabelValueCell(codeTable, "الرقم المسلسل", order.getSequenceNumber() != null ? order.getSequenceNumber() : "-", labelFont, bodyFont);
        addLabelValueCell(codeTable, "الكود", order.getCode() != null ? order.getCode() : "-", labelFont, bodyFont);
        document.add(codeTable);
        document.add(new Paragraph("\n"));

        // بيانات المستلم
        addArabicParagraph(document, "بيانات المستلم", headFont, Element.ALIGN_RIGHT);

        PdfPTable recipientTable = createRtlTable(2, new float[]{1, 1});
        addLabelValueCell(recipientTable, "المرسل إليه *", order.getRecipientName() != null ? order.getRecipientName() : "-", labelFont, bodyFont);
        addLabelValueCell(recipientTable, "رقم الهاتف", order.getRecipientPhone() != null ? order.getRecipientPhone() : "-", labelFont, bodyFont);
        document.add(recipientTable);

        // المحافظة و الشركة
        PdfPTable govTable = createRtlTable(2, new float[]{1, 1});
        String govName = order.getGovernorate() != null ? order.getGovernorate().getArabicName() : "-";
        addLabelValueCell(govTable, "المحافظة", govName, labelFont, bodyFont);
        addLabelValueCell(govTable, "الشركة", order.getCompanyName() != null ? order.getCompanyName() : "-", labelFont, bodyFont);
        document.add(govTable);

        // المنطقة (إذا وُجدت)
        if (order.getDistrict() != null && !order.getDistrict().isEmpty()) {
            PdfPTable districtTable = createRtlTable(1, null);
            addLabelValueCell(districtTable, "المنطقة", order.getDistrict(), labelFont, bodyFont);
            document.add(districtTable);
        }

        // العنوان
        String fullAddress = buildFullAddress(order);
        PdfPTable addressTable = createRtlTable(1, null);
        addLabelValueCell(addressTable, "العنوان التفصيلي", fullAddress, labelFont, bodyFont);
        document.add(addressTable);
        document.add(new Paragraph("\n"));

        // تفاصيل الشحنة
        addArabicParagraph(document, "تفاصيل الشحنة", headFont, Element.ALIGN_RIGHT);

        PdfPTable shippingTable = createRtlTable(3, new float[]{1, 1, 1});
        addLabelValueCell(shippingTable, "الكمية", order.getQuantity() != null ? order.getQuantity().toString() : "-", labelFont, bodyFont);
        addLabelValueCell(shippingTable, "سعر الشحن (ج.م)", order.getShippingPrice() != null ? order.getShippingPrice().toString() : "-", labelFont, bodyFont);
        addLabelValueCell(shippingTable, "سعر الأوردر (ج.م)", order.getOrderPrice() != null ? order.getOrderPrice().toString() : "-", labelFont, bodyFont);
        document.add(shippingTable);
        document.add(new Paragraph("\n"));

        // المبلغ الإجمالي
        BigDecimal totalAmount = order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO;
        addTotalAmountSection(document, totalAmount, labelFont);

        // رقم الفاتورة
        document.add(new Paragraph("\n"));
        addArabicParagraph(document, safeText(invoice.getInvoiceNumber()), smallFont, Element.ALIGN_CENTER);
    }

    // =====================================================================
    //  Invoice Header — Logo (right) | Org Name (center) | Barcode (left)
    // =====================================================================

    /**
     * إضافة هيدر الفاتورة بتصميم ثلاثي الأعمدة:
     * - يمين (RTL أول عمود): لوجو المنظمة أو أول حرف من الاسم
     * - وسط: اسم المنظمة
     * - شمال (RTL آخر عمود): باركود الفاتورة
     */
    private void addInvoiceHeader(Document document, Organization org, Invoice invoice, Order order, Font titleFont) throws Exception {
        // إنشاء جدول عادي LTR لتجنب عكس النص مرتين (لأن processArabicText يعكسه بالفعل)
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1.2f, 2f, 1.2f});
        headerTable.setSpacingAfter(8);

        // === Column 1 (Left): QR Code (Order Code) ===
        PdfPCell qrCell = createHeaderCell();
        try {
            String barcodeData = order.getCode() != null && !order.getCode().isEmpty() 
                    ? order.getCode() : invoice.getInvoiceNumber();
                    
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(barcodeData, BarcodeFormat.QR_CODE, 140, 140);

            try (ByteArrayOutputStream qrBaos = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrBaos);
                Image barcodeImage = Image.getInstance(qrBaos.toByteArray());
                barcodeImage.scaleToFit(70, 70);
                barcodeImage.setAlignment(Element.ALIGN_CENTER);
                qrCell.setImage(barcodeImage);
            }
        } catch (Exception e) {
            log.warn("QR code generation failed for invoice header: {}", e.getMessage());
            BaseFont bf = getCairoBaseFont();
            Font codeFont = new Font(bf, 10, Font.NORMAL, Color.GRAY);
            String codeText = order.getCode() != null ? order.getCode() : "";
            qrCell.setPhrase(new Phrase(codeText, codeFont));
        }
        headerTable.addCell(qrCell);

        // === Column 2 (Center): Organization Name (Arabic supported) ===
        PdfPCell nameCell = createHeaderCell();
        nameCell.setPhrase(new Phrase(processArabicText(safeText(org.getName())), titleFont));
        headerTable.addCell(nameCell);

        // === Column 3 (Right): Organization Logo ===
        PdfPCell logoCell = createHeaderCell();

        boolean logoAdded = false;
        if (org.getLogoUrl() != null && !org.getLogoUrl().isEmpty()) {
            try {
                Path logoPath = storageService.load(org.getLogoUrl());
                if (Files.exists(logoPath)) {
                    byte[] logoBytes = Files.readAllBytes(logoPath);
                    Image logoImage = Image.getInstance(logoBytes);
                    logoImage.scaleToFit(70, 70);
                    logoImage.setAlignment(Element.ALIGN_CENTER);
                    logoCell.setImage(logoImage);
                    logoAdded = true;
                }
            } catch (Exception e) {
                log.warn("Failed to load organization logo for invoice: {}", e.getMessage());
            }
        }
        if (!logoAdded) {
            // Fallback: first letter of org name (with Arabic shaping)
            BaseFont bf = getCairoBaseFont();
            Font letterFont = new Font(bf, 28, Font.BOLD, new Color(100, 100, 100));
            String firstLetter = org.getName() != null && !org.getName().isEmpty()
                    ? processArabicText(org.getName().substring(0, 1)) : "?";
            logoCell.setPhrase(new Phrase(firstLetter, letterFont));
        }
        headerTable.addCell(logoCell);

        document.add(headerTable);
    }

    /**
     * إنشاء خلية Header موحدة الشكل
     */
    private PdfPCell createHeaderCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderWidthBottom(2);
        cell.setBorderColor(new Color(200, 200, 200));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setFixedHeight(80); // Increased height to match bigger QR/Logo
        return cell;
    }

    // =====================================================================
    //  Helper methods — كل النصوص تمر عبر processArabicText (ICU4J)
    // =====================================================================

    /**
     * إنشاء جدول RTL (لترتيب الأعمدة من اليمين لليسار).
     */
    private PdfPTable createRtlTable(int columns, float[] widths) throws DocumentException {
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        if (widths != null) {
            table.setWidths(widths);
        }
        table.setRunDirection(PdfWriter.RUN_DIRECTION_RTL);
        return table;
    }

    /**
     * إضافة فقرة نصية عربية.
     * النص يُعالج بالكامل عبر ICU4J (shaping + bidi) قبل إضافته.
     * لا نعتمد على RUN_DIRECTION_RTL في الخلية — النص جاهز بصرياً.
     */
    private void addArabicParagraph(Document document, String text, Font font, int alignment) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(new Phrase(processArabicText(safeText(text)), font));
        cell.setHorizontalAlignment(alignment);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingBottom(4);
        table.addCell(cell);

        document.add(table);
    }

    /**
     * إضافة خلية تحتوي على label و value.
     * كلاهما يُعالج عبر ICU4J (shaping + bidi).
     */
    private void addLabelValueCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell containerCell = new PdfPCell();
        containerCell.setBorder(Rectangle.BOX);
        containerCell.setBorderColor(new Color(220, 220, 220));
        containerCell.setPadding(0);

        PdfPTable innerTable = new PdfPTable(1);
        innerTable.setWidthPercentage(100);

        // Label cell — نص معالج بالكامل
        PdfPCell labelCell = new PdfPCell(new Phrase(processArabicText(safeText(label)), labelFont));
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingLeft(6);
        labelCell.setPaddingRight(6);
        labelCell.setPaddingTop(4);
        labelCell.setPaddingBottom(1);
        innerTable.addCell(labelCell);

        // Value cell — نص معالج بالكامل
        PdfPCell valueCell = new PdfPCell(new Phrase(processArabicText(safeText(value)), valueFont));
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingLeft(6);
        valueCell.setPaddingRight(6);
        valueCell.setPaddingTop(1);
        valueCell.setPaddingBottom(4);
        innerTable.addCell(valueCell);

        containerCell.addElement(innerTable);
        table.addCell(containerCell);
    }

    /**
     * قسم المبلغ الإجمالي
     */
    private void addTotalAmountSection(Document document, BigDecimal totalAmount, Font labelFont) throws Exception {
        PdfPTable totalTable = new PdfPTable(1);
        totalTable.setWidthPercentage(100);

        BaseFont bf = getCairoBaseFont();
        Font totalAmountFont = new Font(bf, 18, Font.BOLD, new Color(0, 128, 0));

        Phrase totalPhrase = new Phrase();
        totalPhrase.add(new Chunk(processArabicText("المبلغ الإجمالي (ج.م)  "), labelFont));
        totalPhrase.add(new Chunk(String.format("%.2f", totalAmount), totalAmountFont));

        PdfPCell totalCell = new PdfPCell(totalPhrase);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setBorder(Rectangle.TOP);
        totalCell.setBorderWidthTop(2);
        totalCell.setPadding(10);
        totalCell.setBackgroundColor(new Color(245, 245, 245));
        totalTable.addCell(totalCell);

        document.add(totalTable);
    }

    /**
     * إضافة QR Code
     */
    private void addQrCode(Document document, Invoice invoice, Order order, BigDecimal totalAmount) {
        try {
            String qrData = "INV:" + invoice.getInvoiceNumber() + "|CODE:" + (order.getCode() != null ? order.getCode() : "") + "|AMT:" + totalAmount;
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrData, BarcodeFormat.QR_CODE, 80, 80);

            try (ByteArrayOutputStream qrBaos = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrBaos);
                Image qrImage = Image.getInstance(qrBaos.toByteArray());
                qrImage.setAlignment(Element.ALIGN_CENTER);
                document.add(qrImage);
            }
        } catch (Exception e) {
            log.warn("QR code generation failed: {}", e.getMessage());
        }
    }

    private void addTableCell(PdfPTable table, String text, Font font, Color bgColor, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(processArabicText(safeText(text)), font));
        if (bgColor != null) cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5);
        table.addCell(cell);
    }

    /**
     * بناء العنوان الكامل: المركز + المنطقة + العنوان المكتوب
     */
    private String buildFullAddress(Order order) {
        StringBuilder sb = new StringBuilder();
        List<String> parts = new java.util.ArrayList<>();

        if (order.getCenter() != null && !order.getCenter().isEmpty()) {
            parts.add(order.getCenter());
        }
        if (order.getArea() != null && !order.getArea().isEmpty()) {
            parts.add(order.getArea());
        }
        // fallback للحقل القديم
        if (parts.isEmpty() && order.getDistrict() != null && !order.getDistrict().isEmpty()) {
            parts.add(order.getDistrict());
        }
        if (order.getRecipientAddress() != null && !order.getRecipientAddress().isEmpty()) {
            parts.add(order.getRecipientAddress());
        }

        if (parts.isEmpty()) return "-";
        return String.join(" - ", parts);
    }

    private String safeText(String text) {
        return text != null ? text : "";
    }
}
