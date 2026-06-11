package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.BroadcastTargetType;
import com.shipment.shippinggo.enums.BroadcastType;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.repository.BroadcastMessageRepository;
import com.shipment.shippinggo.repository.OrganizationRepository;
import com.shipment.shippinggo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class BroadcastService {

    private final BroadcastMessageRepository broadcastRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PlatformSettingService platformSettingService;

    public BroadcastService(BroadcastMessageRepository broadcastRepository,
                            NotificationService notificationService,
                            UserRepository userRepository,
                            OrganizationRepository organizationRepository,
                            PlatformSettingService platformSettingService) {
        this.broadcastRepository = broadcastRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.platformSettingService = platformSettingService;
    }

    /**
     * إرسال رسالة بث
     */
    @Transactional
    public BroadcastMessage sendBroadcast(String title, String body, BroadcastType type,
                                          BroadcastTargetType targetType, Long targetOrgId,
                                          User sentBy) {
        BroadcastMessage broadcast = BroadcastMessage.builder()
                .title(title)
                .body(body)
                .type(type)
                .targetType(targetType)
                .sentBy(sentBy)
                .active(true)
                .build();

        if (targetType == BroadcastTargetType.SPECIFIC_ORG && targetOrgId != null) {
            Organization org = organizationRepository.findById(targetOrgId).orElse(null);
            broadcast.setTargetOrganization(org);
        }

        broadcast = broadcastRepository.save(broadcast);

        // إرسال الإشعارات الفعلية
        deliverBroadcast(broadcast);

        return broadcast;
    }

    /**
     * توصيل الرسالة للمستخدمين المستهدفين
     */
    private void deliverBroadcast(BroadcastMessage broadcast) {
        Map<String, String> data = Map.of("type", "BROADCAST", "broadcastType", broadcast.getType().name());

        switch (broadcast.getTargetType()) {
            case ALL_USERS -> {
                List<User> allUsers = userRepository.findAll();
                for (User user : allUsers) {
                    if (user.isEnabled()) {
                        notificationService.sendNotificationToUser(user, broadcast.getTitle(),
                                broadcast.getBody(), data, "BROADCAST");
                    }
                }
            }
            case ALL_ADMINS -> {
                List<User> admins = userRepository.findAll().stream()
                        .filter(u -> u.isEnabled() && (u.getRole() == Role.ADMIN || u.getRole() == Role.MANAGER))
                        .toList();
                for (User admin : admins) {
                    notificationService.sendNotificationToUser(admin, broadcast.getTitle(),
                            broadcast.getBody(), data, "BROADCAST");
                }
            }
            case SPECIFIC_ORG -> {
                if (broadcast.getTargetOrganization() != null) {
                    notificationService.sendNotificationToOrganization(
                            broadcast.getTargetOrganization(),
                            broadcast.getTitle(), broadcast.getBody(), data, "BROADCAST");
                }
            }
        }
    }

    /**
     * تفعيل/تعطيل وضع الصيانة
     */
    @Transactional
    public void toggleMaintenanceMode(boolean enabled, String message, User updatedBy) {
        platformSettingService.setSetting("maintenance_mode", String.valueOf(enabled), updatedBy);
        if (message != null && !message.isEmpty()) {
            platformSettingService.setSetting("maintenance_message", message, updatedBy);
        }

        // إشعار جميع المستخدمين عند تفعيل الصيانة
        if (enabled) {
            sendBroadcast("🔧 وضع الصيانة",
                    message != null ? message : "المنصة تحت الصيانة حالياً. سنعود قريباً.",
                    BroadcastType.MAINTENANCE, BroadcastTargetType.ALL_USERS, null, updatedBy);
        }
    }

    public boolean isMaintenanceMode() {
        String val = platformSettingService.getSetting("maintenance_mode", "false");
        return "true".equalsIgnoreCase(val);
    }

    public String getMaintenanceMessage() {
        return platformSettingService.getSetting("maintenance_message", "المنصة تحت الصيانة حالياً.");
    }

    // ===== Query Methods =====

    public List<BroadcastMessage> getAllBroadcasts() {
        return broadcastRepository.findAllByOrderBySentAtDesc();
    }

    public List<BroadcastMessage> getActiveBroadcasts() {
        return broadcastRepository.findByActiveTrueOrderBySentAtDesc();
    }

    @Transactional
    public void deleteBroadcast(Long id) {
        broadcastRepository.findById(id).ifPresent(b -> {
            b.setActive(false);
            broadcastRepository.save(b);
        });
    }
}
