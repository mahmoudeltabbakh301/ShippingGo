package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.OrderDto;
import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Governorate;
import lombok.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelImportService {

    private final OrderService orderService;

    public ExcelImportService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Data
    public static class ImportResult {
        private int successCount = 0;
        private int errorCount = 0;
        private List<String> errors = new ArrayList<>();
        private List<Order> importedOrders = new ArrayList<>();
    }

    // استيراد الطلبات من ملف إكسيل (Excel) وحفظها، مع إمكانية إسنادها فورياً למكتب معين
    public ImportResult importOrders(MultipartFile file, BusinessDay businessDay, User createdBy,
            Organization organization, Long assignedToOrganizationId) {
        ImportResult result = new ImportResult();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Skip header row
            boolean isFirstRow = true;
            int rowNum = 0;

            for (Row row : sheet) {
                rowNum++;
                if (isFirstRow) {
                    isFirstRow = false;
                    continue;
                }

                try {
                    OrderDto dto = parseRow(row, businessDay.getId());
                    // Check if row has any useful data
                    if (hasData(dto)) {
                        Order order = orderService.createOrder(dto, createdBy, organization);

                        // Assign immediately if requested
                        if (assignedToOrganizationId != null) {
                            try {
                                order = orderService.assignToOrganization(order.getId(), assignedToOrganizationId,
                                        createdBy);
                            } catch (Exception e) {
                                // Keep order but flag assignable failure
                                result.getErrors().add("Row " + rowNum + " (Assigned Failed): " + e.getMessage());
                            }
                        }

                        result.getImportedOrders().add(order);
                        result.setSuccessCount(result.getSuccessCount() + 1);
                    }
                } catch (Exception e) {
                    result.setErrorCount(result.getErrorCount() + 1);
                    result.getErrors().add("Row " + rowNum + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            result.getErrors().add("Error reading file: " + e.getMessage());
        }

        return result;
    }

    // قراءة وتحويل صف من ملف الإكسيل إلى كائن (OrderDto) يحتوي على بيانات الطلب
    private OrderDto parseRow(Row row, Long businessDayId) {
        OrderDto dto = new OrderDto();
        dto.setBusinessDayId(businessDayId);

        // Updated mapping based on Left-to-Right layout:
        // 0 (A): م (Sequence Number)
        // 1 (B): سم العميل (Recipient Name)
        // 2 (C): التيلفون (Phone)
        // 3 (D): العنوان (Address)
        // 4 (E): الكمية (Quantity)
        // 5 (F): الشحن (Shipping Price)
        // 6 (G): السعر (Order Price)
        // 7 (H): الاجمالي (Amount)
        // 8 (I): الشركة (Company)
        // 9 (J): الكود (Code)
        // 10 (K): المحافظة (Governorate)

        dto.setSequenceNumber(getCellValueAsString(row.getCell(0)));
        // Ensure name is not null (DB constraint)
        String name = getCellValueAsString(row.getCell(1));
        dto.setRecipientName(name != null ? name : "");

        dto.setRecipientPhone(getCellValueAsString(row.getCell(2)));
        dto.setRecipientAddress(getCellValueAsString(row.getCell(3)));
        dto.setQuantity(getCellValueAsInteger(row.getCell(4)));
        dto.setShippingPrice(getCellValueAsBigDecimal(row.getCell(5)));
        dto.setOrderPrice(getCellValueAsBigDecimal(row.getCell(6)));

        BigDecimal amount = getCellValueAsBigDecimal(row.getCell(7));
        if (amount == null) {
            BigDecimal shipping = dto.getShippingPrice() != null ? dto.getShippingPrice() : BigDecimal.ZERO;
            BigDecimal price = dto.getOrderPrice() != null ? dto.getOrderPrice() : BigDecimal.ZERO;
            amount = shipping.add(price);
        }
        // Ensure amount is never null
        dto.setAmount(amount != null ? amount : BigDecimal.ZERO);

        dto.setCompanyName(getCellValueAsString(row.getCell(8)));
        dto.setCode(getCellValueAsString(row.getCell(9)));

        String governorateStr = getCellValueAsString(row.getCell(10));
        if (governorateStr != null && !governorateStr.trim().isEmpty()) {
            try {
                // محاولة البحث عن المحافظة بالاسم العربي أولاً ثم الإنجليزي
                Governorate matchedGov = null;
                for (Governorate gov : Governorate.values()) {
                    if (gov.getArabicName().equals(governorateStr.trim())
                            || gov.name().equalsIgnoreCase(governorateStr.trim())) {
                        matchedGov = gov;
                        break;
                    }
                }
                if (matchedGov != null) {
                    dto.setGovernorate(matchedGov);
                }
            } catch (Exception e) {
                // تجاهل الخطأ وترك المحافظة فارغة
            }
        }

        return dto;
    }

    // التأكد من أن الصف يحتوي على بيانات صحيحة ومفيدة قبل محاولة حفظه
    private boolean hasData(OrderDto dto) {
        return (dto.getSequenceNumber() != null && !dto.getSequenceNumber().isEmpty()) ||
                (dto.getRecipientName() != null && !dto.getRecipientName().isEmpty()) ||
                (dto.getRecipientPhone() != null && !dto.getRecipientPhone().isEmpty()) ||
                (dto.getCode() != null && !dto.getCode().isEmpty());
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null)
            return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null)
            return null;

        return switch (cell.getCellType()) {
            case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
            case STRING -> {
                try {
                    yield new BigDecimal(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null)
            return null;

        return switch (cell.getCellType()) {
            case NUMERIC -> (int) cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Integer.parseInt(cell.getStringCellValue());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }
}
