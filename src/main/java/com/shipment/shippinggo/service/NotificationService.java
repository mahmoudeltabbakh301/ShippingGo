package com.shipment.shippinggo.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.ApsAlert;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.shipment.shippinggo.dto.NotificationDto;
import com.shipment.shippinggo.entity.AppNotification;
import com.shipment.shippinggo.entity.Membership;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.OrderAssignment;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.MembershipStatus;
import com.shipment.shippinggo.repository.AppNotificationRepository;
import com.shipment.shippinggo.repository.MembershipRepository;
import com.shipment.shippinggo.repository.OrderAssignmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MembershipRepository membershipRepository;
    private final AppNotificationRepository notificationRepository;
    private final OrderAssignmentRepository orderAssignmentRepository;

    private boolean isFirebaseAvailable() {
        return !FirebaseApp.getApps().isEmpty();
    }

    // ==================== Core Send Methods ====================

    public void sendNotificationToUser(User user, String title, String body, Map<String, String> data, String type) {
        sendNotificationToUser(user, title, body, data, type, null, null);
    }

    public void sendNotificationToUser(User user, String title, String body, Map<String, String> data, String type,
            String linkUrl, Long referenceId) {
        // Save to Database
        if (user != null) {
            AppNotification appNotification = AppNotification.builder()
                    .user(user)
                    .title(title)
                    .body(body)
                    .type(type != null ? type : "INFO")
                    .linkUrl(linkUrl)
                    .referenceId(referenceId)
                    .isRead(false)
                    .build();
            notificationRepository.save(appNotification);
        }

        if (user == null || user.getFcmToken() == null || user.getFcmToken().isEmpty()) {
            System.out.println("[FCM] Skipping push: user=" + (user != null ? user.getId() : "null")
                    + ", fcmToken=" + (user != null ? (user.getFcmToken() != null ? "present" : "null") : "N/A"));
            return;
        }

        if (!isFirebaseAvailable()) {
            System.out.println("[FCM] Firebase is not initialized. Skipping notification to user " + user.getId());
            return;
        }

        try {
            Map<String, String> enrichedData = new HashMap<>(data);
            if (linkUrl != null)
                enrichedData.put("linkUrl", linkUrl);
            if (referenceId != null)
                enrichedData.put("referenceId", referenceId.toString());

            // Include title/body in data for Flutter foreground handler
            enrichedData.put("title", title);
            enrichedData.put("body", body);

            Message message = Message.builder()
                    .setToken(user.getFcmToken())
                    // Set Notification object to show system tray notification on Android
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    // For iOS: APNs config to ensure notification shows when app is killed
                    .setApnsConfig(ApnsConfig.builder()
                            .setAps(Aps.builder()
                                    .setAlert(ApsAlert.builder()
                                            .setTitle(title)
                                            .setBody(body)
                                            .build())
                                    .setSound("default")
                                    .build())
                            .build())
                    // For Android: Use High Priority data message to trigger Flutter background handler natively.
                    // The Flutter background handler now uses fullScreenIntent to force the screen to wake up.
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("shippinggo_channel_v2")
                                    .build())
                            .build())
                    // Data payload — for Flutter handler in foreground/background
                    .putAllData(enrichedData)
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            System.out.println("[FCM] Notification sent successfully to user " + user.getId()
                    + ", messageId=" + messageId);
        } catch (FirebaseMessagingException e) {
            System.err.println("[FCM] Error sending to user " + user.getId()
                    + ": " + e.getMessagingErrorCode() + " - " + e.getMessage());
            // If token is invalid, clear it so we don't keep trying
            if (e.getMessagingErrorCode() == com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED
                    || e.getMessagingErrorCode() == com.google.firebase.messaging.MessagingErrorCode.INVALID_ARGUMENT) {
                System.out.println("[FCM] Clearing invalid FCM token for user " + user.getId());
                user.setFcmToken(null);
            }
        }
    }

    public void sendNotificationToOrganization(Organization org, String title, String body, Map<String, String> data,
            String type) {
        sendNotificationToOrganization(org, title, body, data, type, null, null);
    }

    public void sendNotificationToOrganization(Organization org, String title, String body, Map<String, String> data,
            String type, String linkUrl, Long referenceId) {
        if (org == null)
            return;

        if (!isFirebaseAvailable()) {
            System.out.println("Firebase is not initialized. Skipping notification to organization " + org.getId());
        }

        List<Membership> memberships = membershipRepository.findByOrganizationAndStatus(org, MembershipStatus.ACCEPTED);
        List<User> users = memberships.stream()
                .map(Membership::getUser)
                .filter(u -> u.getRole() != com.shipment.shippinggo.enums.Role.COURIER) // Skip couriers for general org
                                                                                        // notifications
                .collect(Collectors.toList());

        // أضف الأدمن أيضاً (قد لا يكون عضواً عبر Membership)
        if (org.getAdmin() != null) {
            boolean adminAlreadyIncluded = users.stream().anyMatch(u -> u.getId().equals(org.getAdmin().getId()));
            if (!adminAlreadyIncluded) {
                users.add(org.getAdmin());
            }
        }

        for (User user : users) {
            sendNotificationToUser(user, title, body, data, type, linkUrl, referenceId);
        }
    }

    // ==================== Order Status Update Notifications ====================

    /**
     * إرسال إشعار تحديث حالة الأوردر لجميع المنظمات المشاركة في سلسلة الإسناد
     */
    public void sendOrderStatusUpdateNotification(Order order, String newStatusArabic) {
        String title = "تحديث حالة الطلب";
        String body = "الطلب رقم " + order.getCode() + " أصبح الآن: " + newStatusArabic;
        String linkUrl = "/orders/" + order.getId();
        Map<String, String> data = Map.of("orderCode", order.getCode(), "type", "STATUS_UPDATE");

        // تجميع جميع المنظمات المشاركة في هذا الأوردر
        Set<Long> notifiedOrgIds = new HashSet<>();

        // 1. المنظمة المالكة
        if (order.getOwnerOrganization() != null) {
            notifiedOrgIds.add(order.getOwnerOrganization().getId());
            sendNotificationToOrganization(order.getOwnerOrganization(), title, body, data, "STATUS_UPDATE", linkUrl,
                    order.getId());
        }

        // 2. المنظمة المسند إليها حالياً
        if (order.getAssignedToOrganization() != null
                && !notifiedOrgIds.contains(order.getAssignedToOrganization().getId())) {
            notifiedOrgIds.add(order.getAssignedToOrganization().getId());
            sendNotificationToOrganization(order.getAssignedToOrganization(), title, body, data, "STATUS_UPDATE",
                    linkUrl, order.getId());
        }

        // 3. جميع المنظمات في سلسلة الإسناد
        try {
            List<OrderAssignment> chain = orderAssignmentRepository.findByOrderIdOrderByLevelAsc(order.getId());
            for (OrderAssignment assignment : chain) {
                Organization assignerOrg = assignment.getAssignerOrganization();
                Organization assigneeOrg = assignment.getAssigneeOrganization();

                if (assignerOrg != null && !notifiedOrgIds.contains(assignerOrg.getId())) {
                    notifiedOrgIds.add(assignerOrg.getId());
                    sendNotificationToOrganization(assignerOrg, title, body, data, "STATUS_UPDATE", linkUrl,
                            order.getId());
                }
                if (assigneeOrg != null && !notifiedOrgIds.contains(assigneeOrg.getId())) {
                    notifiedOrgIds.add(assigneeOrg.getId());
                    sendNotificationToOrganization(assigneeOrg, title, body, data, "STATUS_UPDATE", linkUrl,
                            order.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching assignment chain for notifications: " + e.getMessage());
        }

        // 4. المنظمة المنشئة (قد تكون متجراً أو عميلاً)
        if (order.getCreatorOrganization() != null
                && !notifiedOrgIds.contains(order.getCreatorOrganization().getId())) {
            sendNotificationToOrganization(order.getCreatorOrganization(), title, body, data, "STATUS_UPDATE", linkUrl,
                    order.getId());
        }
    }

    // Backward compatibility - old signature
    public void sendOrderStatusUpdateNotification(Organization ownerOrg, String orderCode, String newStatusArabic) {
        String title = "تحديث حالة الطلب";
        String body = "الطلب رقم " + orderCode + " أصبح الآن: " + newStatusArabic;
        sendNotificationToOrganization(ownerOrg, title, body, Map.of("orderCode", orderCode, "type", "STATUS_UPDATE"),
                "STATUS_UPDATE");
    }

    // ==================== Order Assignment Notifications ====================

    public void sendOrderAssignmentNotification(User courier, Order order) {
        String title = "طلب جديد مسند إليك";
        String body = String.format("تم إسناد الطلب رقم %s (العميل: %s) إليك للتوصيل.",
                order.getCode(),
                order.getRecipientName() != null ? order.getRecipientName() : "غير معروف");

        sendNotificationToUser(courier, title, body,
                Map.of("orderCode", order.getCode(), "type", "ASSIGNMENT"), "ASSIGNMENT",
                "/orders/" + order.getId(), order.getId());
    }

    public void sendOrderUnassignmentNotification(User courier, Order order) {
        String title = "إلغاء إسناد طلب";
        String body = String.format("تم إلغاء إسناد الطلب رقم %s (العميل: %s) منك.",
                order.getCode(),
                order.getRecipientName() != null ? order.getRecipientName() : "غير معروف");

        sendNotificationToUser(courier, title, body,
                Map.of("orderCode", order.getCode(), "type", "UNASSIGNMENT"), "UNASSIGNMENT",
                "/orders/" + order.getId(), order.getId());
    }

    public void sendBulkOrderAssignmentNotification(User courier, int orderCount) {
        String title = "إسناد طلبات جديدة";
        String body = "تم إسناد " + orderCount + " طلب جديد إليك للتوصيل.";
        sendNotificationToUser(courier, title, body,
                Map.of("orderCount", String.valueOf(orderCount), "type", "BULK_ASSIGNMENT"),
                "BULK_ASSIGNMENT", "/dashboard/courier", null);
    }

    // ==================== Organization Assignment Notifications
    // ====================

    /**
     * إشعار إسناد أوردر فردي من منظمة لأخرى - يوجه للـ shipment-requests
     */
    public void sendSingleOrgAssignmentNotification(Organization targetOrg, Order order) {
        String title = "طلب جديد مسند";
        String body = "تم إسناد طلب جديد رقم " + order.getCode() + " لمكتبكم.";
        sendNotificationToOrganization(targetOrg, title, body,
                Map.of("orderCode", order.getCode(), "type", "ORG_ASSIGNMENT"),
                "ORG_ASSIGNMENT", "/shipment-requests", order.getId());
    }

    /**
     * إشعار إسناد أوردرات مجمعة من منظمة لأخرى - يوجه للـ shipment-requests
     */
    public void sendBulkOrgAssignmentNotification(Organization targetOrg, int orderCount) {
        String title = "طلبات جديدة مسندة";
        String body = "تم إسناد " + orderCount + " طلب جديد لمكتبكم.";
        sendNotificationToOrganization(targetOrg, title, body,
                Map.of("count", String.valueOf(orderCount), "type", "BULK_ORG_ASSIGNMENT"),
                "BULK_ORG_ASSIGNMENT", "/shipment-requests", null);
    }

    // ==================== Client/External Order Notifications ====================

    /**
     * إشعار عند استقبال أوردر من عميل أو نظام خارجي - يوجه لطلبات العملاء
     */
    public void sendClientOrderNotification(Organization org, Order order, String sourceName) {
        String title = "طلب جديد من " + sourceName;
        String body = "تم استقبال طلب جديد رقم " + order.getCode();
        sendNotificationToOrganization(org, title, body,
                Map.of("orderCode", order.getCode(), "type", "CLIENT_ORDER"),
                "CLIENT_ORDER", "/org/shipment-requests", order.getId());
    }

    // ==================== Invitation Notifications ====================

    /**
     * إشعار دعوة عمل جديدة - يوجه لصفحة الدعوات
     */
    public void sendInvitationNotification(User targetUser, Organization org, String roleName) {
        String title = "دعوة عمل جديدة";
        String body = String.format("لقد تمت دعوتك للانضمام إلى %s بوظيفة %s", org.getName(), roleName);
        sendNotificationToUser(targetUser, title, body,
                Map.of("type", "WORK_INVITATION", "orgId", org.getId().toString()),
                "WORK_INVITATION", "/members/invitations", null);
    }

    /**
     * إشعار دعوة عميل جديدة - يوجه لصفحة الدعوات
     */
    public void sendClientInvitationNotification(User targetUser, Organization org) {
        String title = "دعوة عميل جديدة";
        String body = String.format("لقد تمت دعوتك كعميل لـ %s", org.getName());
        sendNotificationToUser(targetUser, title, body,
                Map.of("type", "CLIENT_INVITATION", "orgId", org.getId().toString()),
                "CLIENT_INVITATION", "/members/invitations", null);
    }

    /**
     * إشعار عند قبول أو رفض دعوة - يوجه لصفحة الأعضاء
     */
    public void sendInvitationResponseNotification(Organization org, User respondingUser, boolean accepted) {
        String title = accepted ? "تم قبول الدعوة" : "تم رفض الدعوة";
        String body = String.format("%s %s الانضمام إلى %s",
                respondingUser.getFullName(),
                accepted ? "قبل" : "رفض",
                org.getName());
        sendNotificationToOrganization(org, title, body,
                Map.of("type", "INVITATION_RESPONSE", "accepted", String.valueOf(accepted)),
                "INVITATION_RESPONSE", "/members", null);
    }

    // ==================== System Notifications ====================

    /**
     * إرسال إشعار نظامي (من المنصة للمستخدم)
     */
    public void sendSystemNotification(User user, String title, String body) {
        sendNotificationToUser(user, title, body,
                Map.of("type", "SYSTEM"), "SYSTEM", "/payment/subscribe", null);
    }

    // ==================== Query Methods ====================

    /**
     * جلب آخر 10 إشعارات للعرض في الـ dropdown
     */
    public List<NotificationDto> getRecentNotifications(User user) {
        List<AppNotification> notifications = notificationRepository.findTop10ByUserOrderByCreatedAtDesc(user);
        return notifications.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    /**
     * جلب إشعارات مع pagination لصفحة الإشعارات الكاملة
     */
    public Page<NotificationDto> getNotifications(User user, int page, int size) {
        Page<AppNotification> notifPage = notificationRepository.findByUserOrderByCreatedAtDesc(user,
                PageRequest.of(page, size));
        return notifPage.map(this::mapToDto);
    }

    /**
     * عدد الإشعارات غير المقروءة
     */
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsReadFalse(user);
    }

    /**
     * تعليم إشعار كمقروء
     */
    @Transactional
    public void markAsRead(Long notificationId, User user) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUser().getId().equals(user.getId())) {
                notification.setRead(true);
                notificationRepository.save(notification);
            }
        });
    }

    /**
     * تعليم جميع الإشعارات كمقروءة
     */
    @Transactional
    public int markAllAsRead(User user) {
        return notificationRepository.markAllAsReadByUser(user);
    }

    // ==================== Scheduled Cleanup ====================

    /**
     * تنظيف تلقائي للإشعارات القديمة
     * يعمل كل يوم في الساعة 3 صباحاً
     * - حذف الإشعارات المقروءة الأقدم من 15 يوم
     * - حذف جميع الإشعارات الأقدم من 30 يوم
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        try {
            LocalDateTime readCutoff = LocalDateTime.now().minusDays(15);
            int deletedRead = notificationRepository.deleteReadOlderThan(readCutoff);

            LocalDateTime allCutoff = LocalDateTime.now().minusDays(30);
            int deletedAll = notificationRepository.deleteOlderThan(allCutoff);

            if (deletedRead > 0 || deletedAll > 0) {
                System.out.println(
                        String.format("Notification cleanup: deleted %d read (>15d) and %d total (>30d) notifications.",
                                deletedRead, deletedAll));
            }
        } catch (Exception e) {
            System.err.println("Error during notification cleanup: " + e.getMessage());
        }
    }

    // ==================== Helper Methods ====================

    private NotificationDto mapToDto(AppNotification notification) {
        Locale locale = LocaleContextHolder.getLocale();
        String translatedTitle = translateTitle(notification.getTitle(), locale);
        String translatedBody = translateBody(notification.getBody(), locale);
        String translatedTimeAgo = calculateTimeAgo(notification.getCreatedAt(), locale);

        return NotificationDto.builder()
                .id(notification.getId())
                .title(translatedTitle)
                .body(translatedBody)
                .type(notification.getType())
                .linkUrl(notification.getLinkUrl())
                .referenceId(notification.getReferenceId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .timeAgo(translatedTimeAgo)
                .build();
    }

    private String translateTitle(String title, Locale locale) {
        if (!locale.getLanguage().equals("en") || title == null) return title;
        
        return title.replace("تحديث حالة الطلب", "Order Status Update")
                .replace("طلب جديد مسند إليك", "New Order Assigned to You")
                .replace("إلغاء إسناد طلب", "Order Unassigned")
                .replace("إسناد طلبات جديدة", "New Orders Assigned")
                .replace("طلب جديد مسند", "New Order Assigned")
                .replace("طلبات جديدة مسندة", "New Orders Assigned")
                .replace("طلب جديد من ", "New Order from ")
                .replace("دعوة عمل جديدة", "New Work Invitation")
                .replace("دعوة عميل جديدة", "New Client Invitation")
                .replace("تم قبول الدعوة", "Invitation Accepted")
                .replace("تم رفض الدعوة", "Invitation Declined")
                .replace("رحلة شحن في الطريق", "Shipment Trip in Transit")
                .replace("الشاحنة في الرجوع", "Vehicle is Returning");
    }

    private String translateBody(String body, Locale locale) {
        if (!locale.getLanguage().equals("en") || body == null) return body;
        
        String res = body;
        
        // statuses mapping
        if (res.contains("أصبح الآن: ")) {
            res = res.replace("الطلب رقم ", "Order #").replace(" أصبح الآن: ", " is now: ");
            res = res.replace("انتظار", "Waiting")
                     .replace("في الطريق", "In Transit")
                     .replace("تم التسليم", "Delivered")
                     .replace("ملغي", "Cancelled")
                     .replace("رفض الاستلام", "Refused")
                     .replace("مؤجل", "Deferred")
                     .replace("استلام جزئي", "Partial Delivery");
            return res;
        }
        
        if (res.contains("إليك للتوصيل.")) {
            if (res.contains("تم إسناد الطلب رقم ")) {
                res = res.replace("تم إسناد الطلب رقم ", "Order #")
                         .replace(" (العميل: ", " (Client: ")
                         .replace(") إليك للتوصيل.", ") has been assigned to you.");
            } else {
                res = res.replace("تم إسناد ", "")
                         .replace(" طلب جديد إليك للتوصيل.", " new orders assigned to you.");
            }
            return res;
        }

        if (res.contains("منك.")) {
            res = res.replace("تم إلغاء إسناد الطلب رقم ", "Order #")
                     .replace(" (العميل: ", " (Client: ")
                     .replace(") منك.", ") has been unassigned from you.");
            return res;
        }

        if (res.contains("لمكتبكم.")) {
            if (res.contains("طلب جديد رقم ")) {
                res = res.replace("تم إسناد طلب جديد رقم ", "New order #")
                         .replace(" لمكتبكم.", " assigned to your office.");
            } else {
                res = res.replace("تم إسناد ", "")
                         .replace(" طلب جديد لمكتبكم.", " new orders assigned to your office.");
            }
            return res;
        }

        if (res.contains("تم استقبال طلب جديد رقم ")) {
            res = res.replace("تم استقبال طلب جديد رقم ", "New order received: #");
            return res;
        }

        if (res.contains("لقد تمت دعوتك للانضمام إلى ")) {
            res = res.replace("لقد تمت دعوتك للانضمام إلى ", "You have been invited to join ")
                     .replace(" بوظيفة ", " as ");
            return res;
        }
        
        if (res.contains("لقد تمت دعوتك كعميل لـ ")) {
            res = res.replace("لقد تمت دعوتك كعميل لـ ", "You have been invited as a client to ");
            return res;
        }
        
        if (res.contains("قبل الانضمام إلى")) {
            res = res.replace(" قبل الانضمام إلى ", " accepted joining ");
            return res;
        }
        
        if (res.contains("رفض الانضمام إلى")) {
            res = res.replace(" رفض الانضمام إلى ", " declined joining ");
            return res;
        }

        if (res.contains("الشاحنة")) {
            res = res.replace("الشاحنة ", "Vehicle ")
                     .replace(" في طريقها إليكم من ", " is on its way to you from ")
                     .replace(" في طريق الرجوع ", " is on its way back ")
                     .replace(" — كود الرحلة: ", " - Trip Code: ");
            return res;
        }

        return res;
    }

    private String calculateTimeAgo(LocalDateTime dateTime, Locale locale) {
        if (dateTime == null)
            return "";

        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();

        boolean isEn = locale.getLanguage().equals("en");

        if (seconds < 60)
            return isEn ? "Just now" : "الآن";
        if (seconds < 3600)
            return isEn ? (seconds / 60) + " minutes ago" : "منذ " + (seconds / 60) + " دقيقة";
        if (seconds < 86400)
            return isEn ? (seconds / 3600) + " hours ago" : "منذ " + (seconds / 3600) + " ساعة";
        if (seconds < 604800)
            return isEn ? (seconds / 86400) + " days ago" : "منذ " + (seconds / 86400) + " يوم";
        return isEn ? (seconds / 604800) + " weeks ago" : "منذ " + (seconds / 604800) + " أسبوع";
    }
}
