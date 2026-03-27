package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.dto.ApiResponse;
import com.shipment.shippinggo.dto.NotificationDto;
import com.shipment.shippinggo.entity.AppNotification;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.repository.AppNotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
public class ApiNotificationController {

    private final AppNotificationRepository notificationRepository;

    public ApiNotificationController(AppNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthenticated"));
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<AppNotification> notificationPage = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        long unreadCount = notificationRepository.countByUserAndIsReadFalse(user);

        List<NotificationDto> dtos = notificationPage.getContent().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("notifications", dtos);
        data.put("unreadCount", unreadCount);
        data.put("totalPages", notificationPage.getTotalPages());
        data.put("totalElements", notificationPage.getTotalElements());

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthenticated"));
        }
        
        long unreadCount = notificationRepository.countByUserAndIsReadFalse(user);
        Map<String, Object> data = new HashMap<>();
        data.put("unreadCount", unreadCount);
        
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthenticated"));
        }

        return notificationRepository.findById(id).map(notification -> {
            if (!notification.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.<Void>error("Forbidden"));
            }
            notification.setRead(true);
            notificationRepository.save(notification);
            return ResponseEntity.ok(ApiResponse.<Void>success(null, "Notification marked as read"));
        }).orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.<Void>error("Notification not found")));
    }

    @Transactional
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthenticated"));
        }

        int updatedCount = notificationRepository.markAllAsReadByUser(user);
        return ResponseEntity.ok(ApiResponse.<Void>success(null, updatedCount + " notifications marked as read"));
    }

    private NotificationDto mapToDto(AppNotification notification) {
        return NotificationDto.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .body(notification.getBody())
                .type(notification.getType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
