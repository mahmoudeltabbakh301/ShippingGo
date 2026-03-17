package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.CourierDayLog;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrderStatus;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.CourierDayLogService;
import com.shipment.shippinggo.service.OrderService;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final OrganizationService organizationService;
    private final OrderService orderService;
    private final CourierDayLogService courierDayLogService;
    private final com.shipment.shippinggo.service.ReportingService reportingService;
    private final com.shipment.shippinggo.service.AdminDashboardService adminDashboardService;
    private final com.shipment.shippinggo.service.BusinessDayService businessDayService;

    public DashboardController(OrganizationService organizationService,
            OrderService orderService,
            CourierDayLogService courierDayLogService,
            com.shipment.shippinggo.service.ReportingService reportingService,
            com.shipment.shippinggo.service.AdminDashboardService adminDashboardService,
            com.shipment.shippinggo.service.BusinessDayService businessDayService) {
        this.organizationService = organizationService;
        this.orderService = orderService;
        this.courierDayLogService = courierDayLogService;
        this.reportingService = reportingService;
        this.adminDashboardService = adminDashboardService;
        this.businessDayService = businessDayService;
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);

        if (user.getRole() == Role.SUPER_ADMIN) {
            return "redirect:/super-admin";
        }

        if (user.getRole() == Role.COURIER) {
            return "redirect:/dashboard/courier";
        }

        if (user.getRole() == Role.MEMBER) {
            return "redirect:/user/my-orders";
        }

        // Get user's organization
        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            model.addAttribute("noOrganization", true);
            return "dashboard/no-organization";
        }

        model.addAttribute("organization", org);
        model.addAttribute("organizationInactive", !org.isActive());
        model.addAttribute("today", java.time.LocalDate.now());

        // Get active business day
        com.shipment.shippinggo.entity.BusinessDay businessDay = businessDayService.getTodayBusinessDay(org.getId());

        com.shipment.shippinggo.dto.AdminDashboardStats stats;
        if (businessDay == null) {
            stats = com.shipment.shippinggo.dto.AdminDashboardStats.builder().build(); // Empty stats
            model.addAttribute("noActiveBusinessDay", true);
        } else {
            // Dashboard statistics
            stats = adminDashboardService.getDashboardStatsForBusinessDay(org, businessDay.getId());
        }

        model.addAttribute("stats", stats);

        // Prepare chart data (JSON for JavaScript)
        model.addAttribute("chartLabels", new String[]{"تم التسليم", "في الطريق", "انتظار", "مرفوض", "ملغي", "مؤجل", "استلام جزئي"});
        model.addAttribute("chartValues", new long[]{
                stats.getDeliveredCount(),
                stats.getInTransitCount(),
                stats.getWaitingCount(),
                stats.getRefusedCount(),
                stats.getCancelledCount(),
                stats.getDeferredCount(),
                stats.getPartialDeliveryCount()
        });

        // Admin specifically sees pending memberships and shared organizations
        if (user.getRole() == Role.ADMIN || user.getRole() == Role.DATA_ENTRY) {
            model.addAttribute("pendingMemberships", organizationService.getPendingMemberships(org.getId()));
            if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY) {
                model.addAttribute("sharedOffices", organizationService.getOfficesByCompany(org.getId()));
            } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
                model.addAttribute("sharedCompanies", organizationService.getCompaniesByStore(org.getId()));
            } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
                model.addAttribute("sharedCompanies", organizationService.getCompaniesByOffice(org.getId()));
            }
        } else {
            model.addAttribute("pendingMemberships", java.util.List.of());
        }

        return "dashboard/admin";
    }

    @GetMapping("/courier")
    public String courierDashboard(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("today", LocalDate.now());

        Organization org = organizationService.getOrganizationByUser(user);
        model.addAttribute("organizationInactive", org != null && !org.isActive());

        // جلب أوردرات اليوم الحالي فقط (بعد الساعة 12:00 AM)
        var orders = orderService.getOrdersAssignedToCourierToday(user.getId());
        model.addAttribute("orders", orders);

        // تحديث وجلب السجل
        courierDayLogService.updateDayLogStats(user);
        com.shipment.shippinggo.entity.CourierDayLog todayLog = courierDayLogService.getOrCreateTodayLog(user);

        // إحصائيات اليوم فقط
        model.addAttribute("inTransitCount", todayLog.getInTransitCount());
        model.addAttribute("deliveredCount", todayLog.getDeliveredCount());
        model.addAttribute("cancelledCount", todayLog.getCancelledCount());
        model.addAttribute("refusedCount", todayLog.getRefusedCount());

        return "dashboard/courier";
    }

    /**
     * صفحة سجل أيام المندوب
     */
    @GetMapping("/courier/history")
    public String courierHistory(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", user);

        // جلب جميع سجلات الأيام
        List<CourierDayLog> dayLogs = courierDayLogService.getDayLogs(user.getId());
        model.addAttribute("dayLogs", dayLogs);
        model.addAttribute("pageTitle", "سجل الأيام");

        return "dashboard/courier-history";
    }

    /**
     * عرض أوردرات يوم محدد للمندوب
     */
    @GetMapping("/courier/day/{date}")
    public String courierDayView(@AuthenticationPrincipal User user,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Model model) {
        model.addAttribute("user", user);
        model.addAttribute("selectedDate", date);

        // جلب أوردرات اليوم المحدد
        var orders = orderService.getOrdersAssignedToCourierByDate(user.getId(), date);
        model.addAttribute("orders", orders);

        // جلب سجل اليوم إن وجد
        var dayLog = courierDayLogService.getDayLog(user.getId(), date);
        dayLog.ifPresent(log -> model.addAttribute("dayLog", log));

        model.addAttribute("pageTitle", "طلبات يوم " + date.toString());

        return "dashboard/courier-day";
    }

    @GetMapping("/reports")
    public String reports(@AuthenticationPrincipal User user, Model model) {
        if (user.getRole() == Role.COURIER || user.getRole() == Role.MEMBER) {
            return "redirect:/dashboard";
        }

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return "redirect:/dashboard";
        }
        model.addAttribute("organization", org);

        // Pass couriers for Courier Report tab
        model.addAttribute("couriers", organizationService.getCouriers(org));

        // Pass related organizations for Organization Report tab
        if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.COMPANY) {
            model.addAttribute("linkedOrgs", organizationService.getOfficesByCompany(org.getId()));
        } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.STORE) {
            model.addAttribute("linkedOrgs", organizationService.getCompaniesByStore(org.getId()));
        } else if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.OFFICE) {
            model.addAttribute("linkedOrgs", organizationService.getCompaniesByOffice(org.getId()));
        } else {
            model.addAttribute("linkedOrgs", java.util.List.of());
        }

        return "dashboard/reports";
    }

    @GetMapping("/map")
    public String viewMap(@AuthenticationPrincipal User user, Model model) {
        if (user.getRole() == Role.COURIER || user.getRole() == Role.MEMBER) {
            return "redirect:/dashboard";
        }

        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return "redirect:/dashboard";
        }

        model.addAttribute("organization", org);
        java.util.List<User> couriers = organizationService.getCouriers(org);
        model.addAttribute("couriers", couriers);

        return "dashboard/map";
    }
}
