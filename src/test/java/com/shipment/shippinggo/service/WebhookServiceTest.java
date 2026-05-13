package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.WebhookOrderDto;
import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.IntegrationPlatform;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.exception.BusinessLogicException;
import com.shipment.shippinggo.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private StoreIntegrationService storeIntegrationService;

    @Mock
    private BusinessDayService businessDayService;

    @Mock
    private GovernorateRoutingService governorateRoutingService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private QrCodeService qrCodeService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private WebhookService webhookService;

    private StoreIntegration integration;
    private WebhookOrderDto webhookOrderDto;
    private Store store;

    @BeforeEach
    void setUp() {
        store = new Store();
        store.setId(10L);
        store.setName("Salla Test Store");
        store.setAdmin(new User());

        integration = new StoreIntegration();
        integration.setId(1L);
        integration.setPlatform(IntegrationPlatform.SALLA);
        integration.setStore(store);

        webhookOrderDto = new WebhookOrderDto();
        webhookOrderDto.setExternalOrderId("EXT-8899");
        webhookOrderDto.setRecipientName("Mahmoud");
        webhookOrderDto.setRecipientPhone("01010101");
        webhookOrderDto.setGovernorate("Giza"); // String name that should be parsed
        webhookOrderDto.setAmount(new BigDecimal("350.0"));
    }

    @Test
    void processWebhookOrder_ShouldParseGovernorateCorrectlyAndCreateWaitingOrder() {
        // Mocking behavior
        when(storeIntegrationService.getIntegrationOwner(integration)).thenReturn(store);

        BusinessDay businessDay = new BusinessDay();
        when(businessDayService.getOrCreateTodayBusinessDay(eq(store.getId()), any()))
                .thenReturn(businessDay);

        when(qrCodeService.generateUniqueCode(store.getId())).thenReturn("QR-1234");
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(100L);
            return o;
        });

        // تشغيل العملية (استقبال الويب هوك)
        Order savedOrder = webhookService.processWebhookOrder(integration, webhookOrderDto);

        // التحقق من أن النص "Giza" تم تحويله بنجاح للـ Enum المناسب
        assertThat(savedOrder.getGovernorate()).isEqualTo(Governorate.GIZA);
        // التحقق من الحالة الابتدائية
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.WAITING);
        // التحقق من ربط المعرف الخارجي
        assertThat(savedOrder.getExternalOrderId()).isEqualTo("EXT-8899");

        verify(orderRepository).save(any(Order.class));
        verify(notificationService).sendClientOrderNotification(any(Organization.class), any(Order.class), anyString());
    }

    @Test
    void cancelOrderByExternalId_ShouldCancelWhenOrderIsWaiting() {
        when(storeIntegrationService.getIntegrationOwner(integration)).thenReturn(store);
        
        Order targetOrder = new Order();
        targetOrder.setId(55L);
        targetOrder.setStatus(OrderStatus.WAITING);
        
        when(orderRepository.findByExternalOrderIdAndCreatorOrganizationId("EXT-8899", store.getId()))
                .thenReturn(Optional.of(targetOrder));
        
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = webhookService.cancelOrderByExternalId(integration, "EXT-8899");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelOrderByExternalId_ShouldThrowExceptionWhenAssignedToCourier() {
        when(storeIntegrationService.getIntegrationOwner(integration)).thenReturn(store);
        
        Order targetOrder = new Order();
        targetOrder.setId(55L);
        targetOrder.setStatus(OrderStatus.WAITING);
        targetOrder.setAssignedToCourier(new User()); // تم التعيين למندوب
        
        when(orderRepository.findByExternalOrderIdAndCreatorOrganizationId("EXT-8899", store.getId()))
                .thenReturn(Optional.of(targetOrder));

        // محاولة إرسال إلغاء من المنصة الخارجية (شوبيفاي) يجب أن تُرفض من النظام بـ Exception
        BusinessLogicException exception = assertThrows(BusinessLogicException.class, 
                () -> webhookService.cancelOrderByExternalId(integration, "EXT-8899"));

        assertThat(exception.getMessage()).contains("لا يمكن إلغاء الطلب لأنه مسند لمندوب توصيل بالفعل");
        verify(orderRepository, never()).save(targetOrder);
    }
}
