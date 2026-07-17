package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.AccountSummaryDTO;
import com.shipment.shippinggo.dto.report.*;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final ReportingService reportingService;

    // =====================================================================
    // Old Export Methods
    // =====================================================================

    public ByteArrayInputStream exportOrdersToExcel(List<Order> orders) throws IOException {
        String[] columns = {
                "م", "اسم العميل", "التيلفون", "العنوان", "الكمية", 
                "الشحن", "السعر", "الاجمالي", "الشركة", "الكود", 
                "المحافظة", "الحالة", "الملاحظات", "تاريخ الإضافة"
        };

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("الطلبات");

            // Header Font
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLACK.getIndex());

            // Header Style
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Row for Header
            Row headerRow = sheet.createRow(0);

            // Header
            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(headerCellStyle);
            }

            // Cell Style for Date formatter
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            int rowIdx = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(order.getSequenceNumber() != null ? order.getSequenceNumber() : "");
                row.createCell(1).setCellValue(order.getRecipientName() != null ? order.getRecipientName() : "");
                row.createCell(2).setCellValue(order.getRecipientPhone() != null ? order.getRecipientPhone() : "");
                row.createCell(3).setCellValue(order.getRecipientAddress() != null ? order.getRecipientAddress() : "");
                
                if (order.getQuantity() != null) {
                    row.createCell(4).setCellValue(order.getQuantity());
                } else {
                    row.createCell(4).setCellValue("");
                }
                
                if (order.getShippingPrice() != null) {
                    row.createCell(5).setCellValue(order.getShippingPrice().doubleValue());
                } else {
                    row.createCell(5).setCellValue("");
                }
                
                if (order.getOrderPrice() != null) {
                    row.createCell(6).setCellValue(order.getOrderPrice().doubleValue());
                } else {
                    row.createCell(6).setCellValue("");
                }
                
                if (order.getAmount() != null) {
                    row.createCell(7).setCellValue(order.getAmount().doubleValue());
                } else {
                    row.createCell(7).setCellValue("");
                }

                row.createCell(8).setCellValue(order.getCompanyName() != null ? order.getCompanyName() : "");
                row.createCell(9).setCellValue(order.getCode() != null ? order.getCode() : "");
                
                row.createCell(10).setCellValue(order.getGovernorate() != null ? order.getGovernorate().getArabicName() : "");
                row.createCell(11).setCellValue(order.getStatus() != null ? order.getStatus().getArabicName() : "");
                row.createCell(12).setCellValue(order.getNotes() != null ? order.getNotes() : "");
                
                row.createCell(13).setCellValue(order.getCreatedAt() != null ? order.getCreatedAt().format(dateFormatter) : "");
            }

            // Auto size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            // Set right to left
            sheet.setRightToLeft(true);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream exportOrganizationAccountToExcel(List<Order> orders, Organization sourceOrg, Organization targetOrg, String direction, AccountSummaryDTO summary) throws IOException {
        String[] columns = {
                "تاريخ الأوردر", "الكود", "المستلم", "المبلغ", "الحالة", 
                "المندوب", "قيمة الرفض", "تاريخ الإسناد", "الاتجاه"
        };

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("حساب المنظمة");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLACK.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);

            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(headerCellStyle);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

            int rowIdx = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(order.getBusinessDay() != null ? order.getBusinessDay().getDate().toString() : "-");
                row.createCell(1).setCellValue(order.getCode() != null ? order.getCode() : "");
                row.createCell(2).setCellValue(order.getRecipientName() != null ? order.getRecipientName() : "");
                
                double amount = 0;
                if (order.getStatus().name().equals("PARTIAL_DELIVERY") && order.getPartialDeliveryAmount() != null) {
                    amount = order.getPartialDeliveryAmount().doubleValue();
                } else if (order.getAmount() != null) {
                    amount = order.getAmount().doubleValue();
                }
                row.createCell(3).setCellValue(amount);

                row.createCell(4).setCellValue(order.getStatus() != null ? order.getStatus().getArabicName() : "");

                row.createCell(5).setCellValue(order.getAssignedToCourier() != null ? order.getAssignedToCourier().getFullName() : "-");

                row.createCell(6).setCellValue(order.getRejectionPayment() != null ? order.getRejectionPayment().doubleValue() : 0);

                row.createCell(7).setCellValue(order.getAssignmentDate() != null ? order.getAssignmentDate().format(dateFormatter) : "-");

                String orderDirection = "-";
                if ("OUTGOING".equals(direction) || (direction == null && order.getOwnerOrganization().getId().equals(sourceOrg.getId()) 
                    && order.getAssignedToOrganization() != null 
                    && order.getAssignedToOrganization().getId().equals(targetOrg.getId()))) {
                    orderDirection = "صادر";
                } else if ("INCOMING".equals(direction) || (direction == null && order.getOwnerOrganization().getId().equals(targetOrg.getId()) 
                    && order.getAssignedToOrganization() != null 
                    && order.getAssignedToOrganization().getId().equals(sourceOrg.getId()))) {
                    orderDirection = "وارد";
                }
                row.createCell(8).setCellValue(orderDirection);
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.setRightToLeft(true);

            if (summary != null) {
                appendSummaryToSheet(sheet, workbook, rowIdx + 2, summary);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public ByteArrayInputStream exportCourierAccountToExcel(List<Order> orders, User courier, AccountSummaryDTO summary) throws IOException {
        String[] columns = {
                "تاريخ الأوردر", "الكود", "المستلم", "المبلغ", "الحالة", 
                "المكتب/الشركة", "العمولة الفردية", "قيمة الرفض", "تاريخ الإسناد"
        };

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("حساب المندوب");

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLACK.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);

            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(headerCellStyle);
            }

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

            int rowIdx = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(order.getBusinessDay() != null ? order.getBusinessDay().getDate().toString() : "-");
                row.createCell(1).setCellValue(order.getCode() != null ? order.getCode() : "");
                row.createCell(2).setCellValue(order.getRecipientName() != null ? order.getRecipientName() : "");
                
                double amount = 0;
                if (order.getStatus().name().equals("PARTIAL_DELIVERY") && order.getPartialDeliveryAmount() != null) {
                    amount = order.getPartialDeliveryAmount().doubleValue();
                } else if (order.getAmount() != null) {
                    amount = order.getAmount().doubleValue();
                }
                row.createCell(3).setCellValue(amount);

                row.createCell(4).setCellValue(order.getStatus() != null ? order.getStatus().getArabicName() : "");

                row.createCell(5).setCellValue(order.getAssignedToOrganization() != null ? order.getAssignedToOrganization().getName() : (order.getOwnerOrganization() != null ? order.getOwnerOrganization().getName() : "-"));

                row.createCell(6).setCellValue(order.getManualCourierCommission() != null ? order.getManualCourierCommission().doubleValue() : 0);
                
                row.createCell(7).setCellValue(order.getRejectionPayment() != null ? order.getRejectionPayment().doubleValue() : 0);

                row.createCell(8).setCellValue(order.getCourierAssignmentDate() != null ? order.getCourierAssignmentDate().format(dateFormatter) : "-");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }
            sheet.setRightToLeft(true);

            if (summary != null) {
                appendSummaryToSheet(sheet, workbook, rowIdx + 2, summary);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private void appendSummaryToSheet(Sheet sheet, Workbook workbook, int startRowIdx, AccountSummaryDTO summary) {
        // Label style
        CellStyle labelStyle = workbook.createCellStyle();
        Font labelFont = workbook.createFont();
        labelFont.setBold(true);
        labelStyle.setFont(labelFont);
        labelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        labelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Value style
        CellStyle valueStyle = workbook.createCellStyle();

        int currentRow = startRowIdx;

        Row firstRow = sheet.createRow(currentRow++);
        Cell labelCell1 = firstRow.createCell(0);
        labelCell1.setCellValue("ملخص الحساب");
        labelCell1.setCellStyle(labelStyle);

        String[] labels = {
            "إجمالي الأوردرات", "الأوردرات المسلمة", "الأوردرات المرفوضة", "الأوردرات الملغية",
            "إجمالي المبلغ", "المبلغ المسلم", "مبلغ الرفض", "العمولة", "الصافي (بعد الخصم)"
        };

        String[] values = {
            String.valueOf(summary.getTotalOrders()),
            String.valueOf(summary.getDeliveredOrders()),
            String.valueOf(summary.getRefusedOrders()),
            String.valueOf(summary.getCancelledOrders()),
            summary.getTotalAmount() != null ? summary.getTotalAmount().toString() : "0",
            summary.getDeliveredAmount() != null ? summary.getDeliveredAmount().toString() : "0",
            summary.getReturnedAmount() != null ? summary.getReturnedAmount().toString() : "0",
            summary.getTotalCommission() != null ? summary.getTotalCommission().toString() : "0",
            summary.getNetAmount() != null ? summary.getNetAmount().toString() : "0"
        };

        for (int i = 0; i < labels.length; i++) {
            Row row = sheet.createRow(currentRow++);
            
            Cell lCell = row.createCell(0);
            lCell.setCellValue(labels[i]);
            lCell.setCellStyle(labelStyle);

            Cell vCell = row.createCell(1);
            vCell.setCellValue(values[i]);
            vCell.setCellStyle(valueStyle);
        }
    }

    // =====================================================================
    // New Report Export Methods
    // =====================================================================

    public byte[] exportPeriodReport(Long orgId, LocalDate from, LocalDate to) throws IOException {
        PeriodReport report = reportingService.getPeriodReport(orgId, from, to);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("تقرير الفترة");
            sheet.setRightToLeft(true);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("تقرير الفترة: " + from + " إلى " + to);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            rowIdx++;

            String[][] data = {
                    {"إجمالي الأوردرات", String.valueOf(report.getTotalOrders())},
                    {"المسلمة", String.valueOf(report.getDelivered())},
                    {"المرفوضة", String.valueOf(report.getRefused())},
                    {"الملغاة", String.valueOf(report.getCancelled())},
                    {"المؤجلة", String.valueOf(report.getDeferred())},
                    {"التسليم الجزئي", String.valueOf(report.getPartial())},
                    {"قيد التوصيل", String.valueOf(report.getInTransit())},
                    {"", ""},
                    {"المبلغ المحصل", formatBD(report.getTotalCollected())},
                    {"نسبة التوصيل", report.getDeliveryRate() + "%"},
                    {"نسبة الرفض", report.getRefusalRate() + "%"},
                    {"متوسط قيمة الطلب", formatBD(report.getAvgOrderValue())}
            };

            for (String[] row : data) {
                Row r = sheet.createRow(rowIdx++);
                createCell(r, 0, row[0], headerStyle);
                createCell(r, 1, row[1], dataStyle);
            }

            autoSizeColumns(sheet, 2);
            return toBytes(workbook);
        }
    }

    public byte[] exportReturnsReport(Long orgId, LocalDate from, LocalDate to) throws IOException {
        ReturnsReport report = reportingService.getReturnsReport(orgId, from, to);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("تقرير المرتجعات");
            sheet.setRightToLeft(true);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("تقرير المرتجعات: " + from + " إلى " + to);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
            rowIdx++;

            String[][] summary = {
                    {"إجمالي المرتجعات", String.valueOf(report.getTotalReturns())},
                    {"المرفوضة", String.valueOf(report.getRefusedCount())},
                    {"الملغاة", String.valueOf(report.getCancelledCount())},
                    {"المؤجلة", String.valueOf(report.getDeferredCount())},
                    {"نسبة المرتجعات", report.getReturnRate() + "%"},
            };
            for (String[] row : summary) {
                Row r = sheet.createRow(rowIdx++);
                createCell(r, 0, row[0], headerStyle);
                createCell(r, 1, row[1], dataStyle);
            }

            if (report.getTopReasons() != null && !report.getTopReasons().isEmpty()) {
                rowIdx += 2;
                Row subTitle = sheet.createRow(rowIdx++);
                createCell(subTitle, 0, "أسباب الرفض", titleStyle);

                Row hdr = sheet.createRow(rowIdx++);
                createCell(hdr, 0, "السبب", headerStyle);
                createCell(hdr, 1, "العدد", headerStyle);
                createCell(hdr, 2, "النسبة", headerStyle);

                for (ReturnsReport.ReasonStat rs : report.getTopReasons()) {
                    Row r = sheet.createRow(rowIdx++);
                    createCell(r, 0, rs.getReasonLabel(), dataStyle);
                    createCell(r, 1, String.valueOf(rs.getCount()), dataStyle);
                    createCell(r, 2, rs.getPercentage() + "%", dataStyle);
                }
            }

            if (report.getByCourier() != null && !report.getByCourier().isEmpty()) {
                rowIdx += 2;
                Row subTitle = sheet.createRow(rowIdx++);
                createCell(subTitle, 0, "المرتجعات حسب المندوب", titleStyle);

                Row hdr = sheet.createRow(rowIdx++);
                createCell(hdr, 0, "المندوب", headerStyle);
                createCell(hdr, 1, "إجمالي", headerStyle);
                createCell(hdr, 2, "المرتجع", headerStyle);
                createCell(hdr, 3, "النسبة", headerStyle);

                for (ReturnsReport.CourierReturnStat cs : report.getByCourier()) {
                    Row r = sheet.createRow(rowIdx++);
                    createCell(r, 0, cs.getCourierName(), dataStyle);
                    createCell(r, 1, String.valueOf(cs.getTotalOrders()), dataStyle);
                    createCell(r, 2, String.valueOf(cs.getReturnedOrders()), dataStyle);
                    createCell(r, 3, cs.getReturnRate() + "%", dataStyle);
                }
            }

            autoSizeColumns(sheet, 5);
            return toBytes(workbook);
        }
    }

    public byte[] exportDeliveryTimeReport(Long orgId, LocalDate from, LocalDate to) throws IOException {
        DeliveryTimeReport report = reportingService.getDeliveryTimeReport(orgId, from, to);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("تقرير وقت التوصيل");
            sheet.setRightToLeft(true);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("تقرير وقت التوصيل: " + from + " إلى " + to);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            rowIdx++;

            String[][] stats = {
                    {"متوسط وقت التوصيل (ساعات)", String.valueOf(report.getAvgDeliveryTimeHours())},
                    {"أسرع توصيل (ساعات)", String.valueOf(report.getFastestDeliveryHours())},
                    {"أبطأ توصيل (ساعات)", String.valueOf(report.getSlowestDeliveryHours())},
                    {"", ""},
                    {"خلال ساعتين", String.valueOf(report.getWithin2Hours())},
                    {"خلال 4 ساعات", String.valueOf(report.getWithin4Hours())},
                    {"خلال 8 ساعات", String.valueOf(report.getWithin8Hours())},
                    {"خلال 24 ساعة", String.valueOf(report.getWithin24Hours())},
                    {"أكثر من 24 ساعة", String.valueOf(report.getMoreThan24Hours())},
            };
            for (String[] row : stats) {
                Row r = sheet.createRow(rowIdx++);
                createCell(r, 0, row[0], headerStyle);
                createCell(r, 1, row[1], dataStyle);
            }

            if (report.getCourierTimes() != null && !report.getCourierTimes().isEmpty()) {
                rowIdx += 2;
                Row subTitle = sheet.createRow(rowIdx++);
                createCell(subTitle, 0, "أداء المناديب (حسب سرعة التوصيل)", titleStyle);

                Row hdr = sheet.createRow(rowIdx++);
                createCell(hdr, 0, "الترتيب", headerStyle);
                createCell(hdr, 1, "المندوب", headerStyle);
                createCell(hdr, 2, "المتوسط (ساعات)", headerStyle);
                createCell(hdr, 3, "عدد التوصيلات", headerStyle);

                for (DeliveryTimeReport.CourierDeliveryTime ct : report.getCourierTimes()) {
                    Row r = sheet.createRow(rowIdx++);
                    createCell(r, 0, String.valueOf(ct.getRank()), dataStyle);
                    createCell(r, 1, ct.getCourierName(), dataStyle);
                    createCell(r, 2, String.valueOf(ct.getAvgHours()), dataStyle);
                    createCell(r, 3, String.valueOf(ct.getTotalDelivered()), dataStyle);
                }
            }

            autoSizeColumns(sheet, 4);
            return toBytes(workbook);
        }
    }

    public byte[] exportTripReport(Long orgId, LocalDate from, LocalDate to) throws IOException {
        TripReport report = reportingService.getTripReport(orgId, from, to);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("تقرير الرحلات");
            sheet.setRightToLeft(true);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("تقرير الرحلات: " + from + " إلى " + to);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            rowIdx++;

            String[][] stats = {
                    {"إجمالي الرحلات", String.valueOf(report.getTotalTrips())},
                    {"جاري التجهيز", String.valueOf(report.getPreparingTrips())},
                    {"في الطريق", String.valueOf(report.getInTransitTrips())},
                    {"وصلت", String.valueOf(report.getArrivedTrips())},
                    {"مكتملة", String.valueOf(report.getCompletedTrips())},
                    {"في طريق العودة", String.valueOf(report.getReturningTrips())},
                    {"عادت", String.valueOf(report.getReturnedTrips())},
                    {"ملغاة", String.valueOf(report.getCancelledTrips())},
                    {"", ""},
                    {"إجمالي الأوردرات المنقولة", String.valueOf(report.getTotalOrdersShipped())},
                    {"متوسط أوردرات/رحلة", String.valueOf(report.getAvgOrdersPerTrip())},
            };
            for (String[] row : stats) {
                Row r = sheet.createRow(rowIdx++);
                createCell(r, 0, row[0], headerStyle);
                createCell(r, 1, row[1], dataStyle);
            }

            if (report.getByVehicle() != null && !report.getByVehicle().isEmpty()) {
                rowIdx += 2;
                Row subTitle = sheet.createRow(rowIdx++);
                createCell(subTitle, 0, "أداء الشاحنات", titleStyle);

                Row hdr = sheet.createRow(rowIdx++);
                createCell(hdr, 0, "لوحة الشاحنة", headerStyle);
                createCell(hdr, 1, "النوع", headerStyle);
                createCell(hdr, 2, "عدد الرحلات", headerStyle);
                createCell(hdr, 3, "الأوردرات", headerStyle);
                createCell(hdr, 4, "المكتملة", headerStyle);

                for (TripReport.VehicleTripStat vs : report.getByVehicle()) {
                    Row r = sheet.createRow(rowIdx++);
                    createCell(r, 0, vs.getVehiclePlateNumber(), dataStyle);
                    createCell(r, 1, vs.getVehicleType(), dataStyle);
                    createCell(r, 2, String.valueOf(vs.getTotalTrips()), dataStyle);
                    createCell(r, 3, String.valueOf(vs.getTotalOrdersShipped()), dataStyle);
                    createCell(r, 4, String.valueOf(vs.getCompletedTrips()), dataStyle);
                }
            }

            autoSizeColumns(sheet, 5);
            return toBytes(workbook);
        }
    }

    public byte[] exportPerformanceComparison(PerformanceComparison report, String title) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("مقارنة الأداء");
            sheet.setRightToLeft(true);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
            rowIdx++;

            Row hdr = sheet.createRow(rowIdx++);
            String[] headers = {"الترتيب", "الاسم", "إجمالي", "مسلمة", "مرفوضة", "نسبة التوصيل", "المحصل", "الصافي"};
            for (int i = 0; i < headers.length; i++) {
                createCell(hdr, i, headers[i], headerStyle);
            }

            if (report.getEntries() != null) {
                for (PerformanceComparison.PerformanceEntry e : report.getEntries()) {
                    Row r = sheet.createRow(rowIdx++);
                    createCell(r, 0, String.valueOf(e.getRank()), dataStyle);
                    createCell(r, 1, e.getEntityName(), dataStyle);
                    createCell(r, 2, String.valueOf(e.getTotalOrders()), dataStyle);
                    createCell(r, 3, String.valueOf(e.getDeliveredOrders()), dataStyle);
                    createCell(r, 4, String.valueOf(e.getRefusedOrders()), dataStyle);
                    createCell(r, 5, e.getDeliveryRate() + "%", dataStyle);
                    createCell(r, 6, formatBD(e.getTotalCollected()), dataStyle);
                    createCell(r, 7, formatBD(e.getNetAmount()), dataStyle);
                }
            }

            autoSizeColumns(sheet, 8);
            return toBytes(workbook);
        }
    }

    public byte[] exportFinancialReport(FinancialSummaryReport report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("التقرير المالي");
            sheet.setRightToLeft(true);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("التقرير المالي الشامل: " + report.getFromDate() + " إلى " + report.getToDate());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
            rowIdx++;

            // --- ملخص الأوردرات ---
            Row subTitle1 = sheet.createRow(rowIdx++);
            createCell(subTitle1, 0, "إحصائيات الأوردرات", titleStyle);

            String[][] orderStats = {
                    {"إجمالي الأوردرات", String.valueOf(report.getTotalOrders())},
                    {"المسلمة", String.valueOf(report.getDeliveredOrders())},
                    {"المرفوضة", String.valueOf(report.getRefusedOrders())},
                    {"الملغاة", String.valueOf(report.getCancelledOrders())},
                    {"المؤجلة", String.valueOf(report.getDeferredOrders())},
                    {"التسليم الجزئي", String.valueOf(report.getPartialDeliveryOrders())},
                    {"قيد التوصيل", String.valueOf(report.getInTransitOrders())},
                    {"في الانتظار", String.valueOf(report.getWaitingOrders())},
            };
            for (String[] row : orderStats) {
                Row r = sheet.createRow(rowIdx++);
                createCell(r, 0, row[0], headerStyle);
                createCell(r, 1, row[1], dataStyle);
            }

            // --- العمولات ---
            rowIdx++;
            Row subTitle2 = sheet.createRow(rowIdx++);
            createCell(subTitle2, 0, "تفاصيل العمولات", titleStyle);

            String[][] commStats = {
                    {"عمولات صادرة (إجمالي)", formatBD(report.getOutgoingOrgCommissions())},
                    {"  توصيل صادر", formatBD(report.getOutgoingDeliveryComm())},
                    {"  رفض صادر", formatBD(report.getOutgoingRejectionComm())},
                    {"  إلغاء صادر", formatBD(report.getOutgoingCancellationComm())},
                    {"", ""},
                    {"عمولات واردة (إجمالي)", formatBD(report.getIncomingOrgCommissions())},
                    {"  توصيل وارد", formatBD(report.getIncomingDeliveryComm())},
                    {"  رفض وارد", formatBD(report.getIncomingRejectionComm())},
                    {"  إلغاء وارد", formatBD(report.getIncomingCancellationComm())},
                    {"", ""},
                    {"عمولات المناديب (إجمالي)", formatBD(report.getCourierCommissions())},
                    {"  توصيل مناديب", formatBD(report.getCourierDeliveryComm())},
                    {"  رفض مناديب", formatBD(report.getCourierRejectionComm())},
                    {"  إلغاء مناديب", formatBD(report.getCourierCancellationComm())},
                    {"", ""},
                    {"إجمالي إيرادات العمولات", formatBD(report.getTotalCommissionRevenue())},
                    {"صافي الربح", formatBD(report.getNetProfit())},
            };
            for (String[] row : commStats) {
                Row r = sheet.createRow(rowIdx++);
                createCell(r, 0, row[0], headerStyle);
                createCell(r, 1, row[1], dataStyle);
            }

            // --- تفاصيل المنظمات الصادرة ---
            if (report.getOutgoingOrgDetails() != null && !report.getOutgoingOrgDetails().isEmpty()) {
                rowIdx++;
                Row subTitle3 = sheet.createRow(rowIdx++);
                createCell(subTitle3, 0, "تفاصيل المنظمات الصادرة", titleStyle);
                rowIdx = writeEntityDetailsTable(sheet, rowIdx, report.getOutgoingOrgDetails(), headerStyle, dataStyle);
            }

            // --- تفاصيل المنظمات الواردة ---
            if (report.getIncomingOrgDetails() != null && !report.getIncomingOrgDetails().isEmpty()) {
                rowIdx++;
                Row subTitle4 = sheet.createRow(rowIdx++);
                createCell(subTitle4, 0, "تفاصيل المنظمات الواردة", titleStyle);
                rowIdx = writeEntityDetailsTable(sheet, rowIdx, report.getIncomingOrgDetails(), headerStyle, dataStyle);
            }

            // --- تفاصيل المناديب ---
            if (report.getCourierDetails() != null && !report.getCourierDetails().isEmpty()) {
                rowIdx++;
                Row subTitle5 = sheet.createRow(rowIdx++);
                createCell(subTitle5, 0, "تفاصيل عمولات المناديب", titleStyle);
                rowIdx = writeEntityDetailsTable(sheet, rowIdx, report.getCourierDetails(), headerStyle, dataStyle);
            }

            autoSizeColumns(sheet, 7);
            return toBytes(workbook);
        }
    }

    private int writeEntityDetailsTable(Sheet sheet, int rowIdx,
            List<FinancialSummaryReport.EntityFinancialDetail> details,
            CellStyle headerStyle, CellStyle dataStyle) {
        Row hdr = sheet.createRow(rowIdx++);
        String[] headers = {"الاسم", "إجمالي", "مسلمة", "مرفوضة", "عمولة توصيل", "عمولة رفض", "الصافي"};
        for (int i = 0; i < headers.length; i++) {
            createCell(hdr, i, headers[i], headerStyle);
        }
        for (FinancialSummaryReport.EntityFinancialDetail d : details) {
            Row r = sheet.createRow(rowIdx++);
            createCell(r, 0, d.getEntityName(), dataStyle);
            createCell(r, 1, String.valueOf(d.getTotalOrders()), dataStyle);
            createCell(r, 2, String.valueOf(d.getDeliveredOrders()), dataStyle);
            createCell(r, 3, String.valueOf(d.getRefusedOrders()), dataStyle);
            createCell(r, 4, formatBD(d.getDeliveryCommission()), dataStyle);
            createCell(r, 5, formatBD(d.getRejectionCommission()), dataStyle);
            createCell(r, 6, formatBD(d.getNetAmount()), dataStyle);
        }
        return rowIdx;
    }

    public byte[] exportGeographicReport(GeographicReport report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("التوزيع الجغرافي");
            sheet.setRightToLeft(true);

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);

            int rowIdx = 0;
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("تقرير التوزيع الجغرافي");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
            rowIdx++;

            Row hdr = sheet.createRow(rowIdx++);
            createCell(hdr, 0, "المحافظة", headerStyle);
            createCell(hdr, 1, "عدد الطلبات", headerStyle);
            createCell(hdr, 2, "نسبة التوصيل", headerStyle);
            createCell(hdr, 3, "الإيرادات", headerStyle);

            if (report.getStatsByGovernorate() != null) {
                for (var entry : report.getStatsByGovernorate().entrySet()) {
                    Row r = sheet.createRow(rowIdx++);
                    createCell(r, 0, entry.getKey().getArabicName(), dataStyle);
                    createCell(r, 1, String.valueOf(entry.getValue().getTotalOrders()), dataStyle);
                    createCell(r, 2, entry.getValue().getDeliveryRate() + "%", dataStyle);
                    createCell(r, 3, formatBD(entry.getValue().getTotalRevenue()), dataStyle);
                }
            }

            autoSizeColumns(sheet, 4);
            return toBytes(workbook);
        }
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createDataStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) {
                sheet.setColumnWidth(i, 3000);
            }
        }
    }

    private String formatBD(BigDecimal bd) {
        return bd != null ? bd.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() : "0.00";
    }

    private byte[] toBytes(XSSFWorkbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}
