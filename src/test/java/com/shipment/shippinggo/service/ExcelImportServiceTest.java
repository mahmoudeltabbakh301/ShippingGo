package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.OrderDto;
import com.shipment.shippinggo.entity.BusinessDay;
import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExcelImportServiceTest {

    @Mock
    private OrderService orderService;

    private ExcelImportService excelImportService;

    @BeforeEach
    void setUp() {
        excelImportService = new ExcelImportService(orderService);
    }

    @Test
    void importOrders_InvalidFile_ReturnsErrors() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "not excel".getBytes());

        BusinessDay bd = new BusinessDay();
        bd.setId(1L);
        Organization org = new Company();
        User user = new User();

        ExcelImportService.ImportResult result = excelImportService.importOrders(file, bd, user, org, null);
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    void importOrders_EmptyExcel_ReturnsZeroSuccess() throws Exception {
        org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");
        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("م");
        header.createCell(1).setCellValue("الاسم");

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MockMultipartFile file = new MockMultipartFile(
                "file", "orders.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray());

        BusinessDay bd = new BusinessDay();
        bd.setId(1L);
        Organization org = new Company();
        User user = new User();

        ExcelImportService.ImportResult result = excelImportService.importOrders(file, bd, user, org, null);

        assertEquals(0, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void importOrders_WithValidRow_CreatesOrder() throws Exception {
        org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Sheet1");

        org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("م");

        org.apache.poi.ss.usermodel.Row dataRow = sheet.createRow(1);
        dataRow.createCell(0).setCellValue("1");
        dataRow.createCell(1).setCellValue("عميل تجريبي");
        dataRow.createCell(2).setCellValue("01234567890");
        dataRow.createCell(3).setCellValue("القاهرة");
        dataRow.createCell(4).setCellValue(1);
        dataRow.createCell(5).setCellValue(50);
        dataRow.createCell(6).setCellValue(200);
        dataRow.createCell(7).setCellValue(250);

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        MockMultipartFile file = new MockMultipartFile(
                "file", "orders.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray());

        BusinessDay bd = new BusinessDay();
        bd.setId(1L);
        Organization org = new Company();
        User user = new User();

        Order createdOrder = new Order();
        createdOrder.setId(1L);
        when(orderService.createOrdersBulk(anyList(), any(User.class), any(Organization.class), any(BusinessDay.class)))
                .thenReturn(java.util.Collections.singletonList(createdOrder));

        ExcelImportService.ImportResult result = excelImportService.importOrders(file, bd, user, org, null);

        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getErrorCount());
        assertEquals(1, result.getImportedOrders().size());
    }
}
