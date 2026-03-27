package com.shipment.shippinggo.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.shipment.shippinggo.entity.AppNotification;
import com.shipment.shippinggo.entity.Membership;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.MembershipStatus;
import com.shipment.shippinggo.repository.AppNotificationRepository;
import com.shipment.shippinggo.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MembershipRepository membershipRepository;
    private final AppNotificationRepository notificationRepository;

    private boolean isFirebaseAvailable() {
        return !FirebaseApp.getApps().isEmpty();
    }

    public void sendNotificationToUser(User user, String title, String body, Map<String, String> data, String type) {
        // Save to Database
        if (user != null) {
            AppNotification appNotification = AppNotification.builder()
                    .user(user)
                    .title(title)
                    .body(body)
                    .type(type != null ? type : "INFO")
                    .isRead(false)
                    .build();
            notificationRepository.save(appNotification);
        }

        if (user == null || user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
            return;
        }

        if (!isFirebaseAvailable()) {
            System.out.println("Firebase is not initialized. Skipping notification to user " + user.getId());
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(user.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .build();

            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            System.err.println("Error sending FCM message to user " + user.getId() + ": " + e.getMessage());
        }
    }

    public void sendOrderUnassignmentNotification(User courier, Order order) {
        String title = "إلغاء إسناد طلب";
        String body = String.format("تم إلغاء إسناد الطلب رقم %s (العميل: %s) منك.", 
                order.getCode(), 
                order.getRecipientName() != null ? order.getRecipientName() : "غير معروف");
        
        sendNotificationToUser(courier, title, body, 
                Map.of("orderCode", order.getCode(), "type", "UNASSIGNMENT"), "UNASSIGNMENT");
    }

    public void sendNotificationToOrganization(Organization org, String title, String body, Map<String, String> data, String type) {
        if (org == null) return;

        if (!isFirebaseAvailable()) {
            System.out.println("Firebase is not initialized. Skipping notification to organization " + org.getId());
        }

        List<Membership> memberships = membershipRepository.findByOrganizationAndStatus(org, MembershipStatus.ACCEPTED);
        List<User> users = memberships.stream()
                .map(Membership::getUser)
                .filter(u -> u.getRole() != com.shipment.shippinggo.enums.Role.COURIER) // Skip couriers for general org notifications
                .filter(u -> u.getFcmToken() != null && !u.getFcmToken().isEmpty())
                .collect(Collectors.toList());

        for (User user : users) {
            sendNotificationToUser(user, title, body, data, type);
        }
    }

    public void sendOrderAssignmentNotification(User courier, Order order) {
        String title = "طلب جديد مسند إليك";
        String body = String.format("تم إسناد الطلب رقم %s (العميل: %s) إليك للتوصيل.", 
                order.getCode(), 
                order.getRecipientName() != null ? order.getRecipientName() : "غير معروف");
        
        sendNotificationToUser(courier, title, body, 
                Map.of("orderCode", order.getCode(), "type", "ASSIGNMENT"), "ASSIGNMENT");
    }

    public void sendBulkOrderAssignmentNotification(User courier, int orderCount) {
        String title = "إسناد طلبات جديدة";
        String body = "تم إسناد " + orderCount + " طلب جديد إليك للتوصيل.";
        sendNotificationToUser(courier, title, body, Map.of("orderCount", String.valueOf(orderCount), "type", "BULK_ASSIGNMENT"), "BULK_ASSIGNMENT");
    }

    public void sendOrderStatusUpdateNotification(Organization ownerOrg, String orderCode, String newStatusArabic) {
        String title = "تحديث حالة الطلب";
        String body = "الطلب رقم " + orderCode + " أصبح الآن: " + newStatusArabic;
        sendNotificationToOrganization(ownerOrg, title, body, Map.of("orderCode", orderCode, "type", "STATUS_UPDATE"), "STATUS_UPDATE");
    }
}
