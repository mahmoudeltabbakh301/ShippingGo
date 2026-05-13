package com.shipment.shippinggo.service;

import com.shipment.shippinggo.dto.WebhookOrderDto;
import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.enums.IntegrationPlatform;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final StoreIntegrationService storeIntegrationService;
    private final BusinessDayService businessDayService;
    private final GovernorateRoutingService governorateRoutingService;
    private final OrderRepository orderRepository;
    private final QrCodeService qrCodeService;
    private final NotificationService notificationService;

    public WebhookService(StoreIntegrationService storeIntegrationService,
            BusinessDayService businessDayService,
            GovernorateRoutingService governorateRoutingService,
            OrderRepository orderRepository,
            QrCodeService qrCodeService,
            NotificationService notificationService) {
        this.storeIntegrationService = storeIntegrationService;
        this.businessDayService = businessDayService;
        this.governorateRoutingService = governorateRoutingService;
        this.orderRepository = orderRepository;
        this.qrCodeService = qrCodeService;
        this.notificationService = notificationService;
    }

    /**
     * التحقق من صحة الـ API Key وإرجاع الـ StoreIntegration
     *
     * @return StoreIntegration إذا كان الـ API Key صالحاً، وإلا Optional.empty()
     */
    public Optional<StoreIntegration> validateApiKey(String apiKey) {
        return storeIntegrationService.validateApiKey(apiKey);
    }

    /**
     * معالجة أوردر واحد وارد من Webhook
     * يدعم كلا النوعين: متاجر (Store) وأنظمة خارجية (Company/Office)
     */
    @Transactional
    public Order processWebhookOrder(StoreIntegration integration, WebhookOrderDto dto) {
        IntegrationPlatform platform = integration.getPlatform();

        // تحديد المنظمة المالكة: Store أو Organization
        Organization ownerOrg = storeIntegrationService.getIntegrationOwner(integration);
        User admin = ownerOrg.getAdmin();

        log.info("معالجة أوردر Webhook من {} للمنظمة '{}' - الطلب الخارجي: {}",
                platform.getDisplayName(), ownerOrg.getName(), dto.getExternalOrderId());

        // 1. جلب أو إنشاء يوم العمل الحالي للمنظمة
        BusinessDay businessDay = businessDayService.getOrCreateTodayBusinessDay(ownerOrg.getId(), admin);

        // 2. توليد كود فريد للأوردر
        String orderCode = qrCodeService.generateUniqueCode(ownerOrg.getId());

        // 3. تحويل المحافظة من نص إلى Enum
        Governorate governorate = parseGovernorate(dto.getGovernorate());

        // 4. إنشاء الأوردر
        Order order = Order.builder()
                .businessDay(businessDay)
                .code(orderCode)
                .companyName(dto.getCompanyName())
                .recipientName(dto.getRecipientName())
                .recipientPhone(dto.getRecipientPhone())
                .recipientAddress(dto.getRecipientAddress())
                .amount(dto.getAmount())
                .shippingPrice(dto.getShippingPrice())
                .orderPrice(dto.getOrderPrice())
                .quantity(dto.getQuantity() != null ? dto.getQuantity() : 1)
                .notes(dto.getNotes())
                .governorate(governorate)
                .status(OrderStatus.WAITING)
                .ownerOrganization(ownerOrg)
                .creatorOrganization(ownerOrg)
                .createdBy(admin)
                // حقول المصدر الخارجي
                .externalOrderId(dto.getExternalOrderId())
                .sourcePlatform(platform)
                .build();

        Order savedOrder = orderRepository.save(order);

        // 5. محاولة التوزيع التلقائي حسب المحافظة (فقط للمتاجر)
        if (ownerOrg instanceof Store && governorate != null) {
            governorateRoutingService.autoAssignIfRuleExists(savedOrder, (Store) ownerOrg);
        }

        // 6. تحديث وقت آخر Webhook
        storeIntegrationService.updateLastWebhookReceived(integration.getId());

        // 7. إرسال إشعار للمنظمة المستقبلة مع رابط مباشر لطلبات العملاء
        notificationService.sendClientOrderNotification(ownerOrg, savedOrder, platform.getDisplayName());

        log.info("تم إنشاء الأوردر {} بنجاح من {} في يوم العمل {}",
                orderCode, platform.getDisplayName(), businessDay.getDate());

        return savedOrder;
    }

    /**
     * معالجة أوردرات متعددة واردة من Webhook (Batch)
     */
    @Transactional
    public List<Order> processWebhookOrdersBatch(StoreIntegration integration, List<WebhookOrderDto> dtos) {
        List<Order> createdOrders = new ArrayList<>();

        for (WebhookOrderDto dto : dtos) {
            try {
                Order order = processWebhookOrder(integration, dto);
                createdOrders.add(order);
            } catch (Exception e) {
                log.error("فشل معالجة أوردر في الـ Batch (externalId: {}): {}",
                        dto.getExternalOrderId(), e.getMessage());
                // نستمر في معالجة باقي الأوردرات ولا نوقف الكل
            }
        }

        return createdOrders;
    }

    /**
     * إلغاء أوردر عبر الـ API بناءً على الـ externalOrderId
     * الشرط: لا يمكن الإلغاء إذا كان الأوردر مسنداً لمندوب (في الطريق)
     */
    @Transactional
    public Order cancelOrderByExternalId(StoreIntegration integration, String externalOrderId) {
        Organization ownerOrg = storeIntegrationService.getIntegrationOwner(integration);

        Order order = orderRepository.findByExternalOrderIdAndCreatorOrganizationId(externalOrderId, ownerOrg.getId())
                .orElseThrow(() -> new com.shipment.shippinggo.exception.ResourceNotFoundException(
                        "الطلب غير موجود بالرقم الخارجي: " + externalOrderId));

        // لا يمكن الإلغاء إذا كان مسنداً لمندوب
        if (order.getAssignedToCourier() != null) {
            throw new com.shipment.shippinggo.exception.BusinessLogicException(
                    "لا يمكن إلغاء الطلب لأنه مسند لمندوب توصيل بالفعل.");
        }

        // لا يمكن الإلغاء إذا في حالة نهائية
        if (order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.REFUSED ||
                order.getStatus() == OrderStatus.CANCELLED) {
            throw new com.shipment.shippinggo.exception.BusinessLogicException(
                    "لا يمكن إلغاء طلب في حالة نهائية (" + order.getStatus().getArabicName() + ").");
        }

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    /**
     * جلب حالة أوردر عبر الـ externalOrderId
     */
    public Order getOrderByExternalId(StoreIntegration integration, String externalOrderId) {
        Organization ownerOrg = storeIntegrationService.getIntegrationOwner(integration);

        return orderRepository.findByExternalOrderIdAndCreatorOrganizationId(externalOrderId, ownerOrg.getId())
                .orElseThrow(() -> new com.shipment.shippinggo.exception.ResourceNotFoundException(
                        "الطلب غير موجود بالرقم الخارجي: " + externalOrderId));
    }

    /**
     * تحويل اسم المحافظة (String) إلى Enum
     * يدعم الأسماء الإنجليزية (CAIRO) والعربية (القاهرة)
     */
    private Governorate parseGovernorate(String governorateName) {
        if (governorateName == null || governorateName.isBlank()) {
            return null;
        }

        // محاولة التحويل المباشر بالاسم الإنجليزي (مثل CAIRO)
        try {
            return Governorate.valueOf(governorateName.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }

        // محاولة البحث بالاسم العربي أو الإنجليزي
        String trimmed = governorateName.trim();
        for (Governorate g : Governorate.values()) {
            if (g.getArabicName().equals(trimmed) || g.getEnglishName().equalsIgnoreCase(trimmed)) {
                return g;
            }
        }

        log.warn("لم يتم التعرف على المحافظة: '{}'", governorateName);
        return null;
    }
}
