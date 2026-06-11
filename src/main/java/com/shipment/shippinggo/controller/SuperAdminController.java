package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.dto.OrgCreateDto;
import com.shipment.shippinggo.dto.OrgUpdateDto;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.Subscription;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.*;
import com.shipment.shippinggo.service.*;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/super-admin")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final OrganizationService organizationService;
    private final SubscriptionService subscriptionService;
    private final PaymobService paymobService;
    private final PlatformSettingService platformSettingService;
    private final BroadcastService broadcastService;
    private final SupportTicketService supportTicketService;

    public SuperAdminController(SuperAdminService superAdminService,
                                 OrganizationService organizationService,
                                 SubscriptionService subscriptionService,
                                 PaymobService paymobService,
                                 PlatformSettingService platformSettingService,
                                 BroadcastService broadcastService,
                                 SupportTicketService supportTicketService) {
        this.superAdminService = superAdminService;
        this.organizationService = organizationService;
        this.subscriptionService = subscriptionService;
        this.paymobService = paymobService;
        this.platformSettingService = platformSettingService;
        this.broadcastService = broadcastService;
        this.supportTicketService = supportTicketService;
    }

    // ===== Dashboard =====

    @GetMapping
    public String dashboard(Model model) {
        SuperAdminService.PlatformStats stats = superAdminService.getStats();
        model.addAttribute("stats", stats);
        model.addAttribute("organizations", superAdminService.getLatestOrganizations(5));

        SubscriptionService.SubscriptionStats subStats = subscriptionService.getSubscriptionStats();
        model.addAttribute("subStats", subStats);
        model.addAttribute("totalRevenue", paymobService.getTotalRevenue());
        model.addAttribute("monthlyRevenue", paymobService.getMonthlyRevenue());

        // إحصائيات الدعم الفني
        SupportTicketService.TicketStats ticketStats = supportTicketService.getTicketStats();
        model.addAttribute("ticketStats", ticketStats);

        model.addAttribute("pageTitle", "لوحة تحكم المدير العام");
        return "super-admin/dashboard";
    }

    // ===== Organizations - List =====

    @GetMapping("/organizations")
    public String listOrganizations(@RequestParam(required = false) String search,
                                     @RequestParam(required = false, defaultValue = "false") boolean showDeleted,
                                     Model model) {
        java.util.List<Organization> allOrgs = showDeleted
                ? superAdminService.getAllOrganizationsIncludingDeleted()
                : superAdminService.getAllOrganizations();

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            allOrgs = allOrgs.stream()
                    .filter(o -> o.getName().toLowerCase().contains(q)
                            || (o.getPhone() != null && o.getPhone().contains(q))
                            || (o.getEmail() != null && o.getEmail().toLowerCase().contains(q)))
                    .toList();
        }

        model.addAttribute("organizations", allOrgs);
        model.addAttribute("search", search);
        model.addAttribute("showDeleted", showDeleted);
        model.addAttribute("pageTitle", "إدارة المنظمات");
        return "super-admin/organizations";
    }

    // ===== Organizations - Detail =====

    @GetMapping("/organizations/{id}")
    public String organizationDetail(@PathVariable Long id, Model model) {
        Organization org = superAdminService.getOrganizationById(id);
        model.addAttribute("org", org);
        model.addAttribute("admin", org.getAdmin());
        model.addAttribute("members", organizationService.getAcceptedMemberships(id));
        model.addAttribute("pendingMembers", organizationService.getPendingMemberships(id));

        Subscription subscription = subscriptionService.getSubscriptionByOrgIdOrNull(id);
        model.addAttribute("subscription", subscription);
        model.addAttribute("payments", paymobService.getPaymentsByOrganization(id));

        // قائمة المستخدمين لنقل الملكية
        model.addAttribute("allUsers", superAdminService.getAllUsers());

        model.addAttribute("pageTitle", "تفاصيل المنظمة - " + org.getName());
        return "super-admin/organization-detail";
    }

    // ===== Organizations - Create =====

    @GetMapping("/organizations/new")
    public String newOrganizationForm(Model model) {
        model.addAttribute("orgDto", new OrgCreateDto());
        model.addAttribute("orgTypes", new OrganizationType[]{OrganizationType.COMPANY, OrganizationType.OFFICE, OrganizationType.STORE});
        model.addAttribute("governorates", Governorate.values());
        model.addAttribute("editMode", false);
        model.addAttribute("pageTitle", "إنشاء منظمة جديدة");
        return "super-admin/organization-form";
    }

    @PostMapping("/organizations/create")
    public String createOrganization(@ModelAttribute OrgCreateDto dto, RedirectAttributes redirectAttributes) {
        try {
            Organization org = superAdminService.createOrganization(dto);
            redirectAttributes.addFlashAttribute("success", "تم إنشاء المنظمة '" + org.getName() + "' بنجاح");
            return "redirect:/super-admin/organizations/" + org.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/super-admin/organizations/new";
        }
    }

    // ===== Organizations - Edit =====

    @GetMapping("/organizations/{id}/edit")
    public String editOrganizationForm(@PathVariable Long id, Model model) {
        Organization org = superAdminService.getOrganizationById(id);
        OrgUpdateDto dto = new OrgUpdateDto();
        dto.setName(org.getName());
        dto.setAddress(org.getAddress());
        dto.setPhone(org.getPhone());
        dto.setEmail(org.getEmail());
        dto.setGovernorate(org.getGovernorate());
        dto.setAbout(org.getAbout());
        dto.setPickupPolicy(org.getPickupPolicy());
        dto.setReturnPolicy(org.getReturnPolicy());
        dto.setEstimatedDeliveryDays(org.getEstimatedDeliveryDays());
        dto.setPaymentTerms(org.getPaymentTerms());
        dto.setWhatsappNumber(org.getWhatsappNumber());
        dto.setWebsiteUrl(org.getWebsiteUrl());

        model.addAttribute("orgDto", dto);
        model.addAttribute("org", org);
        model.addAttribute("governorates", Governorate.values());
        model.addAttribute("editMode", true);
        model.addAttribute("pageTitle", "تعديل المنظمة - " + org.getName());
        return "super-admin/organization-form";
    }

    @PostMapping("/organizations/{id}/update")
    public String updateOrganization(@PathVariable Long id, @ModelAttribute OrgUpdateDto dto,
                                      RedirectAttributes redirectAttributes) {
        try {
            superAdminService.updateOrganization(id, dto);
            redirectAttributes.addFlashAttribute("success", "تم تحديث بيانات المنظمة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/organizations/" + id;
    }

    // ===== Organizations - Toggle / Delete / Restore / Transfer =====

    @PostMapping("/organizations/{id}/toggle")
    public String toggleOrganization(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            superAdminService.toggleOrganizationActive(id);
            redirectAttributes.addFlashAttribute("success", "تم تحديث حالة المنظمة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/organizations";
    }

    @PostMapping("/organizations/{id}/delete")
    public String deleteOrganization(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            superAdminService.softDeleteOrganization(id);
            redirectAttributes.addFlashAttribute("success", "تم حذف المنظمة (أرشفة) بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/organizations";
    }

    @PostMapping("/organizations/{id}/restore")
    public String restoreOrganization(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            superAdminService.restoreOrganization(id);
            redirectAttributes.addFlashAttribute("success", "تم استعادة المنظمة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/organizations/" + id;
    }

    @PostMapping("/organizations/{id}/transfer")
    public String transferOwnership(@PathVariable Long id,
                                     @RequestParam Long newAdminId,
                                     RedirectAttributes redirectAttributes) {
        try {
            superAdminService.transferOwnership(id, newAdminId);
            redirectAttributes.addFlashAttribute("success", "تم نقل ملكية المنظمة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/organizations/" + id;
    }

    // ===== Users =====

    @GetMapping("/users")
    public String listUsers(@RequestParam(required = false) String search, Model model) {
        java.util.List<User> users = superAdminService.getAllUsers();

        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            users = users.stream()
                    .filter(u -> u.getFullName().toLowerCase().contains(q)
                            || u.getUsername().toLowerCase().contains(q)
                            || u.getEmail().toLowerCase().contains(q)
                            || (u.getPhone() != null && u.getPhone().contains(q)))
                    .toList();
        }

        model.addAttribute("users", users);
        model.addAttribute("search", search);
        model.addAttribute("roles", Role.values());
        model.addAttribute("pageTitle", "إدارة المستخدمين");
        return "super-admin/users";
    }

    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        User user = superAdminService.getUserById(id);
        Organization userOrg = organizationService.getOrganizationByUser(user);
        model.addAttribute("targetUser", user);
        model.addAttribute("userOrg", userOrg);
        model.addAttribute("roles", Role.values());
        model.addAttribute("pageTitle", "تفاصيل المستخدم - " + user.getFullName());
        return "super-admin/user-detail";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            superAdminService.toggleUserEnabled(id);
            redirectAttributes.addFlashAttribute("success", "تم تحديث حالة المستخدم بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeUserRole(@PathVariable Long id,
                                 @RequestParam Role role,
                                 RedirectAttributes redirectAttributes) {
        try {
            superAdminService.changeUserRole(id, role);
            redirectAttributes.addFlashAttribute("success", "تم تغيير صلاحية المستخدم بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/users/" + id;
    }

    // ===== Subscriptions =====

    @GetMapping("/subscriptions")
    public String listSubscriptions(Model model) {
        model.addAttribute("subscriptions", subscriptionService.getAllSubscriptions());
        model.addAttribute("subStats", subscriptionService.getSubscriptionStats());
        model.addAttribute("pageTitle", "إدارة الاشتراكات");
        return "super-admin/subscriptions";
    }

    @PostMapping("/subscriptions/{orgId}/set-price")
    public String setOrganizationPrice(@PathVariable Long orgId,
                                        @RequestParam BigDecimal price,
                                        RedirectAttributes redirectAttributes) {
        try {
            subscriptionService.setOrganizationPrice(orgId, price);
            redirectAttributes.addFlashAttribute("success", "تم تحديث سعر الاشتراك بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/organizations/" + orgId;
    }

    @PostMapping("/subscriptions/{orgId}/activate")
    public String activateSubscription(@PathVariable Long orgId,
                                        @RequestParam(defaultValue = "1") int months,
                                        @RequestParam(required = false) String notes,
                                        RedirectAttributes redirectAttributes) {
        try {
            subscriptionService.manualActivation(orgId, months, notes);
            redirectAttributes.addFlashAttribute("success", "تم تفعيل الاشتراك بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/organizations/" + orgId;
    }

    @PostMapping("/subscriptions/{orgId}/suspend")
    public String suspendSubscription(@PathVariable Long orgId,
                                       @RequestParam(required = false) String notes,
                                       RedirectAttributes redirectAttributes) {
        try {
            subscriptionService.suspendSubscription(orgId, notes);
            redirectAttributes.addFlashAttribute("success", "تم تعليق الاشتراك بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/organizations/" + orgId;
    }

    // ===== Payments =====

    @GetMapping("/payments")
    public String listPayments(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("totalRevenue", paymobService.getTotalRevenue());
        model.addAttribute("monthlyRevenue", paymobService.getMonthlyRevenue());
        model.addAttribute("pageTitle", "سجل المدفوعات");
        return "super-admin/payments";
    }

    // ===== Platform Settings =====

    @GetMapping("/settings")
    public String platformSettings(Model model) {
        model.addAttribute("settings", platformSettingService.getAllSettings());
        model.addAttribute("pageTitle", "إعدادات المنصة");
        return "super-admin/platform-settings";
    }

    @PostMapping("/settings")
    public String updateSettings(@RequestParam java.util.Map<String, String> params,
                                  @AuthenticationPrincipal User currentUser,
                                  RedirectAttributes redirectAttributes) {
        try {
            params.forEach((key, value) -> {
                if (!key.equals("_csrf")) {
                    platformSettingService.setSetting(key, value, currentUser);
                }
            });
            redirectAttributes.addFlashAttribute("success", "تم تحديث الإعدادات بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/settings";
    }

    // ===== Broadcasts =====

    @GetMapping("/broadcasts")
    public String listBroadcasts(Model model) {
        model.addAttribute("broadcasts", broadcastService.getAllBroadcasts());
        model.addAttribute("organizations", superAdminService.getAllOrganizations());
        model.addAttribute("broadcastTypes", BroadcastType.values());
        model.addAttribute("targetTypes", BroadcastTargetType.values());
        model.addAttribute("maintenanceMode", broadcastService.isMaintenanceMode());
        model.addAttribute("pageTitle", "الرسائل والإشعارات");
        return "super-admin/broadcasts";
    }

    @PostMapping("/broadcasts/send")
    public String sendBroadcast(@RequestParam String title,
                                 @RequestParam String body,
                                 @RequestParam BroadcastType type,
                                 @RequestParam BroadcastTargetType targetType,
                                 @RequestParam(required = false) Long targetOrgId,
                                 @AuthenticationPrincipal User currentUser,
                                 RedirectAttributes redirectAttributes) {
        try {
            broadcastService.sendBroadcast(title, body, type, targetType, targetOrgId, currentUser);
            redirectAttributes.addFlashAttribute("success", "تم إرسال الرسالة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/broadcasts";
    }

    @PostMapping("/broadcasts/{id}/delete")
    public String deleteBroadcast(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            broadcastService.deleteBroadcast(id);
            redirectAttributes.addFlashAttribute("success", "تم حذف الرسالة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/broadcasts";
    }

    @PostMapping("/maintenance/toggle")
    public String toggleMaintenance(@RequestParam boolean enabled,
                                     @RequestParam(required = false) String message,
                                     @AuthenticationPrincipal User currentUser,
                                     RedirectAttributes redirectAttributes) {
        try {
            broadcastService.toggleMaintenanceMode(enabled, message, currentUser);
            redirectAttributes.addFlashAttribute("success",
                    enabled ? "تم تفعيل وضع الصيانة" : "تم إلغاء وضع الصيانة");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/broadcasts";
    }

    // ===== Support Tickets (Super Admin Side) =====

    @GetMapping("/support")
    public String listSupportTickets(@RequestParam(required = false) TicketStatus status,
                                      @RequestParam(defaultValue = "0") int page,
                                      Model model) {
        Page<com.shipment.shippinggo.entity.SupportTicket> tickets =
                supportTicketService.getAllTickets(status, page, 20);
        model.addAttribute("tickets", tickets);
        model.addAttribute("ticketStats", supportTicketService.getTicketStats());
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("pageTitle", "تذاكر الدعم الفني");
        return "super-admin/support-tickets";
    }

    @GetMapping("/support/{id}")
    public String supportTicketDetail(@PathVariable Long id, Model model) {
        com.shipment.shippinggo.entity.SupportTicket ticket = supportTicketService.getTicketById(id);
        model.addAttribute("ticket", ticket);
        model.addAttribute("replies", supportTicketService.getTicketReplies(id));
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("pageTitle", "تذكرة دعم - " + ticket.getTicketNumber());
        return "super-admin/support-ticket-detail";
    }

    @PostMapping("/support/{id}/reply")
    public String replyToTicket(@PathVariable Long id,
                                 @RequestParam String message,
                                 @AuthenticationPrincipal User currentUser,
                                 RedirectAttributes redirectAttributes) {
        try {
            supportTicketService.replyToTicket(id, currentUser, message, true);
            redirectAttributes.addFlashAttribute("success", "تم إرسال الرد بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/support/" + id;
    }

    @PostMapping("/support/{id}/status")
    public String updateTicketStatus(@PathVariable Long id,
                                      @RequestParam TicketStatus status,
                                      RedirectAttributes redirectAttributes) {
        try {
            supportTicketService.updateTicketStatus(id, status);
            redirectAttributes.addFlashAttribute("success", "تم تحديث حالة التذكرة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/super-admin/support/" + id;
    }
}
