package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.dto.NotificationDto;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * صفحة الإشعارات الكاملة مع pagination
     */
    @GetMapping
    public String notificationsPage(@AuthenticationPrincipal User user,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     Model model) {
        if (user == null) return "redirect:/login";

        Page<NotificationDto> notificationPage = notificationService.getNotifications(user, page, size);
        long unreadCount = notificationService.getUnreadCount(user);

        model.addAttribute("notifications", notificationPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notificationPage.getTotalPages());
        model.addAttribute("totalElements", notificationPage.getTotalElements());
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("pageTitle", "الإشعارات");

        return "dashboard/notifications";
    }

    /**
     * تعليم إشعار كمقروء والانتقال لرابطه
     */
    @GetMapping("/go/{id}")
    public String markReadAndRedirect(@AuthenticationPrincipal User user,
                                       @PathVariable Long id) {
        if (user == null) return "redirect:/login";

        notificationService.markAsRead(id, user);

        // البحث عن الإشعار للحصول على linkUrl
        var notifications = notificationService.getRecentNotifications(user);
        String linkUrl = notifications.stream()
                .filter(n -> n.getId().equals(id))
                .map(NotificationDto::getLinkUrl)
                .findFirst()
                .orElse(null);

        if (linkUrl != null && !linkUrl.isEmpty()) {
            return "redirect:" + linkUrl;
        }
        return "redirect:/dashboard/notifications";
    }

    /**
     * تعليم إشعار واحد كمقروء
     */
    @PostMapping("/mark-read/{id}")
    public String markAsRead(@AuthenticationPrincipal User user,
                              @PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        if (user == null) return "redirect:/login";
        
        notificationService.markAsRead(id, user);
        return "redirect:/dashboard/notifications";
    }

    /**
     * تعليم جميع الإشعارات كمقروءة
     */
    @PostMapping("/mark-all-read")
    public String markAllAsRead(@AuthenticationPrincipal User user,
                                 RedirectAttributes redirectAttributes) {
        if (user == null) return "redirect:/login";
        
        int count = notificationService.markAllAsRead(user);
        redirectAttributes.addFlashAttribute("success", "تم تعليم " + count + " إشعار كمقروء");
        return "redirect:/dashboard/notifications";
    }
}
