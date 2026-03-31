package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.AccountSummaryDTO;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

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
}
