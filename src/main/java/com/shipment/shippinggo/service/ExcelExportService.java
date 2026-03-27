package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.Order;
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
}
