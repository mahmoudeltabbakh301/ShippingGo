package com.shipment.shippinggo.service;

import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrCodeServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private QrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        qrCodeService = new QrCodeService(orderRepository);
    }

    @Test
    void generateUniqueCode_ShouldReturnValidFormat() {
        when(orderRepository.existsByCode(anyString())).thenReturn(false);

        String code = qrCodeService.generateUniqueCode(5L);

        assertNotNull(code);
        assertTrue(code.startsWith("SG-5-"));
    }

    @Test
    void generateUniqueCode_DuplicateFirstTime_RetriesAndSucceeds() {
        when(orderRepository.existsByCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        String code = qrCodeService.generateUniqueCode(1L);
        assertNotNull(code);
        assertTrue(code.startsWith("SG-1-"));
    }

    @Test
    void generateQrCodeImage_ValidText_ReturnsBytes() {
        byte[] result = qrCodeService.generateQrCodeImage("https://example.com", 200, 200);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateQrCodeImage_EmptyText_ThrowsException() {
        assertThrows(BusinessLogicException.class,
                () -> qrCodeService.generateQrCodeImage("", 200, 200));
    }
}
