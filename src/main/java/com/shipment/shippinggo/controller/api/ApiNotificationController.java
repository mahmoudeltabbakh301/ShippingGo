package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.NotificationDto;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class ApiNotificationController {

    private final NotificationService notificationService;

    public ApiNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * جلب الإشعارات مع pagination
     * يستخدم في الـ dropdown (size=10) وصفحة الإشعارات
     */
    @GetMapping
    public ResponseEntity<?> getNotifications(@AuthenticationPrincipal User user,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        Page<NotificationDto> notifPage = notificationService.getNotifications(user, page, size);
        long unreadCount = notificationService.getUnreadCount(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", Map.of(
                        "notifications", notifPage.getContent(),
                        "currentPage", notifPage.getNumber(),
                        "totalPages", notifPage.getTotalPages(),
                        "totalElements", notifPage.getTotalElements(),
                        "unreadCount", unreadCount
                )
        ));
    }

    /**
     * عدد الإشعارات غير المقروءة - يستخدم في الـ polling
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("unreadCount", count)));
    }

    /**
     * تعليم إشعار واحد كمقروء
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@AuthenticationPrincipal User user, @PathVariable Long id) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        notificationService.markAsRead(id, user);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * تعليم جميع الإشعارات كمقروءة
     */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }

        int count = notificationService.markAllAsRead(user);
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("count", count)));
    }
}
