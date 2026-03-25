package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.dto.AccountSummaryDTO;
import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.CommissionType;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.repository.AccountBusinessDayRepository;
import com.shipment.shippinggo.repository.OrderAssignmentRepository;
import com.shipment.shippinggo.repository.OrderRepository;
import com.shipment.shippinggo.service.AccountService;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final OrganizationService organizationService;
    private final AccountBusinessDayRepository accountBusinessDayRepository;
    private final OrderRepository orderRepository;
    private final OrderAssignmentRepository orderAssignmentRepository;

    public AccountController(AccountService accountService,
            OrganizationService organizationService,
            AccountBusinessDayRepository accountBusinessDayRepository,
            OrderRepository orderRepository,
            OrderAssignmentRepository orderAssignmentRepository) {
        this.accountService = accountService;
        this.organizationService = organizationService;
        this.accountBusinessDayRepository = accountBusinessDayRepository;
        this.orderRepository = orderRepository;
        this.orderAssignmentRepository = orderAssignmentRepository;
    }

    /**
     * الصفحة الرئيسية للحسابات - عرض قائمة أيام العمل
     */
    @GetMapping
    public String showAccountBusinessDays(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            Model model) {

        // جلب قائمة أيام الحسابات (بدون أيام العهدة)
        List<AccountBusinessDay> accountDays = accountBusinessDayRepository
                .findByOrganizationIdAndBusinessDayIsCustodyFalseOrderByBusinessDayDateDesc(org.getId());
        model.addAttribute("accountDays", accountDays);
        model.addAttribute("organization", org);
        model.addAttribute("pageTitle", "أيام الحسابات");

        return "accounts/index";
    }

    /**
     * عرض حسابات يوم عمل محدد
     */
    @GetMapping("/day/{id}")
    public String showAccountDayDetail(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            @PathVariable Long id,
            Model model) {

        AccountBusinessDay accountDay = accountBusinessDayRepository.findById(id).orElse(null);
        if (accountDay == null || !accountDay.getOrganization().getId().equals(org.getId())) {
            return "redirect:/accounts";
        }

        model.addAttribute("accountDay", accountDay);
        model.addAttribute("organization", org);
        model.addAttribute("pageTitle", accountDay.getName());

        // المنظمات المرتبطة والمناديب
        List<Organization> linkedOrganizations = organizationService.getLinkedOrganizations(org);
        List<User> couriers = organizationService.getCouriersByOrganization(org);

        // ملخصات الحسابات لهذا اليوم فقط (بناءً على businessDayId)
        Long businessDayId = accountDay.getBusinessDay().getId();
        List<AccountSummaryDTO> accountSummaries = accountService.getAllAccountSummariesByBusinessDay(org,
                linkedOrganizations, couriers, businessDayId);
        model.addAttribute("accountSummaries", accountSummaries);

        // إجمالي العمولات لهذا اليوم
        BigDecimal totalCommission = accountSummaries.stream()
                .map(s -> s.getTotalCommissions() != null ? s.getTotalCommissions() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalCommission", totalCommission);

        // العمولات الصادرة والواردة
        // الصادرة = عمولات المنظمات الصادرة + عمولات المناديب
        BigDecimal outgoingOrgCommission = accountSummaries.stream()
                .filter(s -> "OUTGOING".equals(s.getDirection()))
                .map(s -> s.getTotalCommissions() != null ? s.getTotalCommissions() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal courierCommission = accountSummaries.stream()
                .filter(s -> "courier".equals(s.getType()))
                .map(s -> s.getTotalCommissions() != null ? s.getTotalCommissions() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOutgoingCommission = outgoingOrgCommission.add(courierCommission);
        model.addAttribute("totalOutgoingCommission", totalOutgoingCommission);

        long countOutgoingOrgTransactions = accountSummaries.stream()
                .filter(s -> "OUTGOING".equals(s.getDirection()))
                .mapToLong(AccountSummaryDTO::getTotalOrders)
                .sum();
        long countCourierTransactions = accountSummaries.stream()
                .filter(s -> "courier".equals(s.getType()))
                .mapToLong(AccountSummaryDTO::getTotalOrders)
                .sum();
        long countOutgoingTransactions = countOutgoingOrgTransactions + countCourierTransactions;
        model.addAttribute("countOutgoingTransactions", countOutgoingTransactions);

        BigDecimal totalIncomingCommission = accountSummaries.stream()
                .filter(s -> "INCOMING".equals(s.getDirection()))
                .map(s -> s.getTotalCommissions() != null ? s.getTotalCommissions() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("totalIncomingCommission", totalIncomingCommission);

        long countIncomingTransactions = accountSummaries.stream()
                .filter(s -> "INCOMING".equals(s.getDirection()))
                .mapToLong(AccountSummaryDTO::getTotalOrders)
                .sum();
        model.addAttribute("countIncomingTransactions", countIncomingTransactions);

        // الفرق الصافي = الوارد - الصادر (موجب يعني لصالحنا، سالب يعني علينا)
        BigDecimal commissionDifference = totalIncomingCommission.subtract(totalOutgoingCommission);
        model.addAttribute("commissionDifference", commissionDifference);

        // عدد المعاملات/الأوردرات لهذا اليوم
        long transactionCount = accountSummaries.stream()
                .mapToLong(AccountSummaryDTO::getTotalOrders)
                .sum();
        model.addAttribute("transactionCount", transactionCount);

        // إعدادات العمولات الحالية
        List<CommissionSetting> commissionSettings = accountService.getCommissionSettings(org);
        model.addAttribute("commissionSettings", commissionSettings);

        return "accounts/day";
    }

    @GetMapping("/organization/{id}")
    public String showOrganizationAccount(@CurrentOrganization Organization sourceOrg,
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) String direction,
            Model model) {
        Organization targetOrg = organizationService.getById(id);

        if (sourceOrg == null || targetOrg == null) {
            return "redirect:/accounts";
        }

        // ملخص الحساب (مفلتر حسب يوم العمل إن وجد)
        AccountSummaryDTO summary;
        List<Order> orders;
        if (businessDayId != null) {
            summary = accountService.getOrganizationAccountSummaryByBusinessDay(sourceOrg, targetOrg, businessDayId,
                    direction);
            orders = accountService.getOrdersAssignedToOrganizationByBusinessDay(sourceOrg, targetOrg, businessDayId,
                    direction);
        } else {
            summary = accountService.getOrganizationAccountSummary(sourceOrg, targetOrg, direction, null);
            orders = accountService.getOrdersAssignedToOrganization(sourceOrg, targetOrg, direction);
        }
        model.addAttribute("summary", summary);
        model.addAttribute("orders", orders);
        model.addAttribute("businessDayId", businessDayId);
        model.addAttribute("direction", direction);

        model.addAttribute("organization", sourceOrg);
        model.addAttribute("targetOrganization", targetOrg);

        String title = "حساب: " + targetOrg.getName();
        if (direction != null && !direction.isEmpty()) {
            title += " (" + (direction.equals("OUTGOING") ? "صادر" : "وارد") + ")";
        }
        model.addAttribute("pageTitle", title);

        return "accounts/organization-detail";
    }

    @GetMapping("/courier/{id}")
    public String showCourierAccount(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(required = false) Long businessDayId,
            Model model) {
        User courier = organizationService.getUserById(id);

        if (org == null || courier == null) {
            return "redirect:/accounts";
        }

        // ملخص الحساب (مفلتر حسب يوم العمل إن وجد)
        AccountSummaryDTO summary;
        List<Order> orders;
        if (businessDayId != null) {
            summary = accountService.getCourierAccountSummaryByBusinessDay(org, courier, businessDayId);
            orders = accountService.getOrdersAssignedToCourierByBusinessDay(courier, businessDayId);
        } else {
            summary = accountService.getCourierAccountSummary(org, courier);
            orders = accountService.getOrdersAssignedToCourier(courier);
        }
        model.addAttribute("summary", summary);
        model.addAttribute("orders", orders);
        model.addAttribute("businessDayId", businessDayId);

        model.addAttribute("organization", org);
        model.addAttribute("courier", courier);
        model.addAttribute("pageTitle", "حساب: " + courier.getFullName());

        return "accounts/courier-detail";
    }

    @GetMapping("/transactions")
    public String showTransactions(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            Model model) {

        List<AccountTransaction> transactions = accountService.getOrganizationTransactions(org);
        model.addAttribute("transactions", transactions);
        model.addAttribute("organization", org);
        model.addAttribute("pageTitle", "سجل المعاملات");

        return "accounts/transactions";
    }

    @GetMapping("/settings")
    public String showSettings(@CurrentOrganization Organization org, @AuthenticationPrincipal User user, Model model) {

        // إعدادات العمولات الحالية
        List<CommissionSetting> commissionSettings = accountService.getCommissionSettings(org);
        model.addAttribute("commissionSettings", commissionSettings);

        // المكاتب المرتبطة (للشركات) أو الشركات المرتبطة (للمكاتب)
        List<Organization> linkedOrganizations = organizationService.getLinkedOrganizations(org);
        model.addAttribute("linkedOrganizations", linkedOrganizations);

        // المناديب
        List<User> couriers = organizationService.getCouriersByOrganization(org);
        model.addAttribute("couriers", couriers);

        model.addAttribute("organization", org);
        model.addAttribute("commissionTypes", CommissionType.values());
        model.addAttribute("governorates", com.shipment.shippinggo.enums.Governorate.values());
        model.addAttribute("pageTitle", "إعدادات العمولات");

        return "accounts/settings";
    }

    @PostMapping("/settings/organization")
    public String saveOrganizationCommission(@CurrentOrganization Organization sourceOrg,
            @AuthenticationPrincipal User user,
            @RequestParam Long targetOrganizationId,
            @RequestParam CommissionType commissionType,
            @RequestParam BigDecimal commissionValue,
            @RequestParam(required = false) BigDecimal rejectionCommission,
            @RequestParam(required = false) BigDecimal cancellationCommission,
            @RequestParam(required = false) com.shipment.shippinggo.enums.Governorate governorate,
            RedirectAttributes redirectAttributes) {
        try {
            Organization targetOrg = organizationService.getById(targetOrganizationId);

            if (targetOrg == null) {
                redirectAttributes.addFlashAttribute("error", "المنظمة غير موجودة");
                return "redirect:/accounts/settings";
            }

            accountService.saveOrganizationCommission(sourceOrg, targetOrg, commissionType, commissionValue,
                    rejectionCommission, cancellationCommission, governorate);
            redirectAttributes.addFlashAttribute("success", "تم حفظ إعداد العمولة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "خطأ: " + e.getMessage());
        }

        return "redirect:/accounts/settings";
    }

    @PostMapping("/settings/courier")
    public String saveCourierCommission(@CurrentOrganization Organization sourceOrg, @AuthenticationPrincipal User user,
            @RequestParam Long courierId,
            @RequestParam CommissionType commissionType,
            @RequestParam BigDecimal commissionValue,
            @RequestParam(required = false) BigDecimal rejectionCommission,
            @RequestParam(required = false) BigDecimal cancellationCommission,
            RedirectAttributes redirectAttributes) {
        try {
            User courier = organizationService.getUserById(courierId);

            if (courier == null) {
                redirectAttributes.addFlashAttribute("error", "البيانات غير صحيحة");
                return "redirect:/accounts/settings";
            }

            if (courier.getRole() != Role.COURIER) {
                redirectAttributes.addFlashAttribute("error", "المستخدم ليس مندوباً");
                return "redirect:/accounts/settings";
            }

            // IDOR Fix: Verify courier belongs to this organization
            boolean isCourierInOrg = organizationService.getCouriersByOrganization(sourceOrg).stream()
                    .anyMatch(c -> c.getId().equals(courierId));
            if (!isCourierInOrg) {
                redirectAttributes.addFlashAttribute("error", "غير مصرح: المندوب لا يتبع لمؤسستك");
                return "redirect:/accounts/settings";
            }

            accountService.saveCourierCommission(sourceOrg, courier, commissionType, commissionValue,
                    rejectionCommission, cancellationCommission);
            redirectAttributes.addFlashAttribute("success", "تم حفظ عمولة المندوب بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "خطأ: " + e.getMessage());
        }

        return "redirect:/accounts/settings";
    }

    @PostMapping("/update-rejection-payment")
    public String updateRejectionPayment(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            @RequestParam Long orderId,
            @RequestParam BigDecimal rejectionPayment,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) Long courierId,
            RedirectAttributes redirectAttributes) {
        try {

            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                redirectAttributes.addFlashAttribute("error", "الطلب غير موجود");
                return "redirect:/accounts";
            }

            boolean isAssignee = order.getAssignedToOrganization() != null
                    && order.getAssignedToOrganization().getId().equals(org.getId());
            boolean isOwner = order.getOwnerOrganization().getId().equals(org.getId());

            if (!isAssignee && !isOwner) {
                redirectAttributes.addFlashAttribute("error",
                        "غير مصرح لتعديل مبلغ الرفض");
                return "redirect:/accounts";
            }

            order.setRejectionPayment(rejectionPayment);
            orderRepository.save(order);
            redirectAttributes.addFlashAttribute("success", "تم تحديث مبلغ الرفض بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "خطأ: " + e.getMessage());
        }

        if (courierId != null) {
            String redirectUrl = "redirect:/accounts/courier/" + courierId;
            if (businessDayId != null) {
                redirectUrl += "?businessDayId=" + businessDayId;
            }
            return redirectUrl;
        }
        return "redirect:/accounts";
    }

    @PostMapping("/update-manual-courier-commission")
    public String updateManualCourierCommission(@CurrentOrganization Organization org,
            @AuthenticationPrincipal User user,
            @RequestParam Long orderId,
            @RequestParam(required = false) BigDecimal manualCourierCommission,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) Long courierId,
            @RequestParam(required = false) Long targetOrgId,
            @RequestParam(required = false) String direction,
            RedirectAttributes redirectAttributes) {
        try {

            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                redirectAttributes.addFlashAttribute("error", "الطلب غير موجود");
                return "redirect:/accounts";
            }

            boolean isAssignee = order.getAssignedToOrganization() != null
                    && order.getAssignedToOrganization().getId().equals(org.getId());
            boolean isOwner = order.getOwnerOrganization().getId().equals(org.getId());

            if (!isAssignee && !isOwner) {
                redirectAttributes.addFlashAttribute("error", "غير مصرح لتعديل العمولة");
                return "redirect:/accounts";
            }

            order.setManualCourierCommission(manualCourierCommission);
            orderRepository.save(order);
            
            // تحديث المعاملات المالية المرتبطة ومسح الكاش
            accountService.updateManualCommissionTransactions(order);
            accountService.clearDashboardCache();

            redirectAttributes.addFlashAttribute("success", "تم تحديث العمولة الفردية للمندوب بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "خطأ: " + e.getMessage());
        }

        if (courierId != null) {
            String redirectUrl = "redirect:/accounts/courier/" + courierId;
            if (businessDayId != null) {
                redirectUrl += "?businessDayId=" + businessDayId;
            }
            return redirectUrl;
        }
        return "redirect:/accounts/organization/";
    }

    @PostMapping("/settings/delete")
    public String deleteCommission(
            @CurrentOrganization Organization org,
            @RequestParam Long settingId,
            RedirectAttributes redirectAttributes) {
        try {
            // Security check (IDOR fix)
            CommissionSetting setting = accountService.getCommissionSettingById(settingId)
                    .orElseThrow(() -> new RuntimeException("الإعداد غير موجود"));

            if (!setting.getSourceOrganization().getId().equals(org.getId())) {
                redirectAttributes.addFlashAttribute("error", "غير مصرح لك بحذف هذا الإعداد");
                return "redirect:/accounts/settings";
            }

            accountService.deleteCommissionSetting(settingId);
            redirectAttributes.addFlashAttribute("success", "تم حذف إعداد العمولة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "خطأ: " + e.getMessage());
        }

        return "redirect:/accounts/settings";
    }
}