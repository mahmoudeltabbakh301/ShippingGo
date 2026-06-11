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
import com.shipment.shippinggo.service.ExcelExportService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    private final ExcelExportService excelExportService;

    public AccountController(AccountService accountService,
            OrganizationService organizationService,
            AccountBusinessDayRepository accountBusinessDayRepository,
            OrderRepository orderRepository,
            OrderAssignmentRepository orderAssignmentRepository,
            ExcelExportService excelExportService) {
        this.accountService = accountService;
        this.organizationService = organizationService;
        this.accountBusinessDayRepository = accountBusinessDayRepository;
        this.orderRepository = orderRepository;
        this.orderAssignmentRepository = orderAssignmentRepository;
        this.excelExportService = excelExportService;
    }

    /**
     * الصفحة الرئيسية للحسابات - عرض قائمة أيام العمل
     */
    @GetMapping
    public String showAccountBusinessDays(
            @RequestParam(defaultValue = "0") int page,
            @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            Model model) {

        int pageSize = 10;
        Pageable pageable = PageRequest.of(page, pageSize);
        Page<AccountBusinessDay> accountDaysPage = accountBusinessDayRepository
                .findByOrganizationIdAndBusinessDayIsCustodyFalseOrderByBusinessDayDateDesc(org.getId(), pageable);

        model.addAttribute("accountDays", accountDaysPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", accountDaysPage.getTotalPages());
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

        java.util.Map<Long, String> courierDisplayNames = organizationService.buildCourierDisplayNameMap(org);
        for (AccountSummaryDTO summary : accountSummaries) {
            if ("courier".equals(summary.getType()) && summary.getCourierId() != null) {
                String displayName = courierDisplayNames.get(summary.getCourierId());
                if (displayName != null) {
                    summary.setName(displayName);
                    summary.setCourierName(displayName);
                }
            }
        }

        model.addAttribute("accountSummaries", accountSummaries);
        model.addAttribute("courierDisplayNames", courierDisplayNames);

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

        // عمولة غير مسند
        BigDecimal unassignedCommission = accountSummaries.stream()
                .filter(s -> "unassigned".equals(s.getType()))
                .map(s -> s.getTotalCommissions() != null ? s.getTotalCommissions() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        model.addAttribute("unassignedCommission", unassignedCommission);

        long countUnassignedOrders = accountSummaries.stream()
                .filter(s -> "unassigned".equals(s.getType()))
                .mapToLong(AccountSummaryDTO::getTotalOrders)
                .sum();
        model.addAttribute("countUnassignedOrders", countUnassignedOrders);

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
        model.addAttribute("courierDisplayNames", organizationService.buildCourierDisplayNameMap(sourceOrg));

        String title = "حساب: " + targetOrg.getName();
        if (direction != null && !direction.isEmpty()) {
            title += " (" + (direction.equals("OUTGOING") ? "صادر" : "وارد") + ")";
        }
        model.addAttribute("pageTitle", title);

        return "accounts/organization-detail";
    }

    @GetMapping("/organization/{id}/export")
    public ResponseEntity<InputStreamResource> exportOrganizationAccount(@CurrentOrganization Organization sourceOrg,
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(required = false) Long businessDayId,
            @RequestParam(required = false) String direction) {
        try {
            Organization targetOrg = organizationService.getById(id);
            if (sourceOrg == null || targetOrg == null) {
                return ResponseEntity.badRequest().build();
            }

            List<Order> orders;
            AccountSummaryDTO summary;
            if (businessDayId != null) {
                summary = accountService.getOrganizationAccountSummaryByBusinessDay(sourceOrg, targetOrg, businessDayId,
                        direction);
                orders = accountService.getOrdersAssignedToOrganizationByBusinessDay(sourceOrg, targetOrg,
                        businessDayId, direction);
            } else {
                summary = accountService.getOrganizationAccountSummary(sourceOrg, targetOrg, direction, null);
                orders = accountService.getOrdersAssignedToOrganization(sourceOrg, targetOrg, direction);
            }

            java.io.ByteArrayInputStream in = excelExportService.exportOrganizationAccountToExcel(orders, sourceOrg,
                    targetOrg, direction, summary);

            HttpHeaders headers = new HttpHeaders();
            String fileName = "organization-account-" + targetOrg.getName() + ".xlsx";
            headers.add("Content-Disposition", "attachment; filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(in));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
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
        String courierDisplayName = organizationService.getCourierDisplayName(courier, org);
        model.addAttribute("courierDisplayName", courierDisplayName);
        model.addAttribute("pageTitle", "حساب: " + courierDisplayName);

        return "accounts/courier-detail";
    }

    @GetMapping("/courier/{id}/export")
    public ResponseEntity<InputStreamResource> exportCourierAccount(@CurrentOrganization Organization org,
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestParam(required = false) Long businessDayId) {
        try {
            User courier = organizationService.getUserById(id);
            if (org == null || courier == null) {
                return ResponseEntity.badRequest().build();
            }

            List<Order> orders;
            AccountSummaryDTO summary;
            if (businessDayId != null) {
                summary = accountService.getCourierAccountSummaryByBusinessDay(org, courier, businessDayId);
                orders = accountService.getOrdersAssignedToCourierByBusinessDay(courier, businessDayId);
            } else {
                summary = accountService.getCourierAccountSummary(org, courier);
                orders = accountService.getOrdersAssignedToCourier(courier);
            }

            java.io.ByteArrayInputStream in = excelExportService.exportCourierAccountToExcel(orders, courier, summary);

            HttpHeaders headers = new HttpHeaders();
            String fileName = "courier-account-" + courier.getFullName() + ".xlsx";
            headers.add("Content-Disposition", "attachment; filename=" + java.net.URLEncoder.encode(fileName, "UTF-8"));

            return ResponseEntity
                    .ok()
                    .headers(headers)
                    .contentType(MediaType
                            .parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(new InputStreamResource(in));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
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

        if (org.getType() == com.shipment.shippinggo.enums.OrganizationType.CLIENT) {
            return "redirect:/dashboard";
        }

        // إعدادات العمولات الحالية
        List<CommissionSetting> commissionSettings = accountService.getCommissionSettings(org);
        model.addAttribute("commissionSettings", commissionSettings);

        // المكاتب المرتبطة (للشركات) أو الشركات المرتبطة (للمكاتب)
        List<Organization> linkedOrganizations = organizationService.getLinkedOrganizations(org);
        model.addAttribute("linkedOrganizations", linkedOrganizations);

        // المناديب
        List<User> couriers = organizationService.getCouriersByOrganization(org);
        model.addAttribute("couriers", couriers);
        model.addAttribute("courierDisplayNames", organizationService.buildCourierDisplayNameMap(org));

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
            accountService.clearDashboardCache();
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
            accountService.clearDashboardCache();
            redirectAttributes.addFlashAttribute("success", "تم حفظ عمولة المندوب بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "خطأ: " + e.getMessage());
        }

        return "redirect:/accounts/settings";
    }

    @PostMapping("/update-rejection-payment")
    public String updateRejectionPayment(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            @RequestParam Long orderId,
            @RequestParam(required = false) BigDecimal rejectionPayment,
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

            if (rejectionPayment == null) {
                rejectionPayment = BigDecimal.ZERO;
            }
            order.setRejectionPayment(rejectionPayment);
            orderRepository.save(order);

            // مسح الكاش لضمان ظهور المبلغ المحدث فوراً وعدم التأخير
            accountService.clearDashboardCache();

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
            accountService.clearDashboardCache();
            redirectAttributes.addFlashAttribute("success", "تم حذف إعداد العمولة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "خطأ: " + e.getMessage());
        }

        return "redirect:/accounts/settings";
    }

    /**
     * حفظ عمولة غير مسند
     */
    @PostMapping("/settings/unassigned")
    public String saveUnassignedCommission(@CurrentOrganization Organization sourceOrg,
            @AuthenticationPrincipal User user,
            @RequestParam CommissionType commissionType,
            @RequestParam BigDecimal commissionValue,
            @RequestParam(required = false) BigDecimal rejectionCommission,
            @RequestParam(required = false) BigDecimal cancellationCommission,
            @RequestParam(required = false) com.shipment.shippinggo.enums.Governorate governorate,
            RedirectAttributes redirectAttributes) {
        try {
            accountService.saveUnassignedCommission(sourceOrg, commissionType, commissionValue,
                    rejectionCommission, cancellationCommission, governorate);
            accountService.clearDashboardCache();
            redirectAttributes.addFlashAttribute("success", "تم حفظ عمولة غير مسند بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "خطأ: " + e.getMessage());
        }

        return "redirect:/accounts/settings";
    }

    /**
     * عرض تفاصيل الأوردرات غير المسندة
     */
    @GetMapping("/unassigned")
    public String showUnassignedOrders(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
            @RequestParam(required = false) Long businessDayId,
            Model model) {

        if (org == null) {
            return "redirect:/accounts";
        }

        List<com.shipment.shippinggo.entity.Order> orders;
        AccountSummaryDTO summary;

        if (businessDayId != null) {
            orders = orderRepository.findUnassignedOrdersByBusinessDay(org.getId(), businessDayId);
        } else {
            orders = orderRepository.findUnassignedOrdersByOrganization(org.getId());
        }

        // حساب الملخص عبر الـ service المحقون (بدلاً من إنشاء instance جديد)
        summary = accountService.getUnassignedOrdersSummary(org, orders);

        model.addAttribute("summary", summary);
        model.addAttribute("orders", orders);
        model.addAttribute("businessDayId", businessDayId);
        model.addAttribute("organization", org);
        model.addAttribute("courierDisplayNames", organizationService.buildCourierDisplayNameMap(org));
        model.addAttribute("pageTitle", "حساب: غير مسند");

        return "accounts/unassigned-detail";
    }

    // ==========================================
    // مكافآت وتارجت (Rewards & Targets)
    // ==========================================

    @GetMapping("/rewards")
    public String showRewardsSettings(@CurrentOrganization Organization org, @AuthenticationPrincipal User user, Model model) {
        if (org == null) return "redirect:/";

        List<TargetSetting> settings = accountService.getTargetSettings(org);
        model.addAttribute("settings", settings);
        model.addAttribute("organization", org);

        // For selection dropdowns
        List<Organization> relatedOrgs = organizationService.getLinkedOrganizations(org);
        List<User> couriers = organizationService.getCouriersByOrganization(org);
        
        model.addAttribute("relatedOrgs", relatedOrgs);
        model.addAttribute("couriers", couriers);
        model.addAttribute("pageTitle", "إعدادات التارجت والمكافآت");
        
        return "accounts/rewards";
    }

    @PostMapping("/rewards/save")
    public String saveRewardSetting(
            @CurrentOrganization Organization sourceOrg,
            @RequestParam(required = false) Long targetOrganizationId,
            @RequestParam(required = false) Long courierId,
            @RequestParam BigDecimal targetAmount,
            @RequestParam CommissionType rewardType,
            @RequestParam BigDecimal rewardValue,
            RedirectAttributes redirectAttributes) {
        
        try {
            if (targetOrganizationId != null) {
                Organization targetOrg = organizationService.findById(targetOrganizationId);
                if(targetOrg == null) throw new RuntimeException("المنظمة غير موجودة");
                accountService.saveOrganizationTarget(sourceOrg, targetOrg, targetAmount, rewardType, rewardValue);
            } else if (courierId != null) {
                User courier = organizationService.getUserById(courierId);
                if (courier == null) throw new RuntimeException("المندوب غير موجود");
                accountService.saveCourierTarget(sourceOrg, courier, targetAmount, rewardType, rewardValue);
            } else {
                throw new RuntimeException("يجب اختيار مندوب أو منظمة");
            }
            redirectAttributes.addFlashAttribute("success", "تم حفظ إعداد التارجت بنجاح. سيبدأ حساب التارجت من اليوم.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "خطأ: " + e.getMessage());
        }
        
        return "redirect:/accounts/rewards";
    }

    @PostMapping("/rewards/delete")
    public String deleteRewardSetting(
            @CurrentOrganization Organization org,
            @RequestParam Long settingId,
            RedirectAttributes redirectAttributes) {
        try {
            TargetSetting setting = accountService.getTargetSettingById(settingId)
                    .orElseThrow(() -> new RuntimeException("الإعداد غير موجود"));

            if (!setting.getSourceOrganization().getId().equals(org.getId())) {
                redirectAttributes.addFlashAttribute("error", "غير مصرح لك بحذف هذا الإعداد");
                return "redirect:/accounts/rewards";
            }

            accountService.deleteTargetSetting(settingId);
            redirectAttributes.addFlashAttribute("success", "تم إيقاف وحذف التارجت بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "خطأ: " + e.getMessage());
        }

        return "redirect:/accounts/rewards";
    }

    @GetMapping("/rewards/{id}/details")
    public String showRewardDetails(
            @PathVariable Long id,
            @CurrentOrganization Organization org,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        TargetSetting setting = accountService.getTargetSettingById(id).orElse(null);
        if (setting == null || !setting.getSourceOrganization().getId().equals(org.getId())) {
            redirectAttributes.addFlashAttribute("error", "الإعداد غير موجود أو غير مصرح لك بعرضه");
            return "redirect:/accounts/rewards";
        }

        java.time.LocalDateTime[] period = accountService.getCurrentMonthPeriod(setting);
        BigDecimal totalDelivered = BigDecimal.ZERO;
        List<com.shipment.shippinggo.dto.DailyTargetStatsDto> dailyStats = new java.util.ArrayList<>();

        if (setting.getCourier() != null) {
            totalDelivered = accountService.getCourierDeliveredAmount(setting.getCourier(), period[0], period[1]);
            dailyStats = accountService.getDailyCourierDeliveredAmount(setting.getCourier(), period[0], period[1]);
        } else if (setting.getTargetOrganization() != null) {
            totalDelivered = accountService.getOrganizationDeliveredAmount(org, setting.getTargetOrganization(), period[0], period[1]);
            dailyStats = accountService.getDailyOrganizationDeliveredAmount(org, setting.getTargetOrganization(), period[0], period[1]);
        }

        int progress = accountService.calculateTargetProgress(totalDelivered, setting.getTargetAmount());
        java.math.BigDecimal rewardAmount = accountService.calculateReward(setting, totalDelivered);

        model.addAttribute("setting", setting);
        model.addAttribute("periodStart", period[0]);
        model.addAttribute("periodEnd", period[1]);
        model.addAttribute("totalDelivered", totalDelivered);
        model.addAttribute("dailyStats", dailyStats);
        model.addAttribute("progress", progress);
        model.addAttribute("rewardAmount", rewardAmount);
        model.addAttribute("pageTitle", "تفاصيل تارجت - " + (setting.getCourier() != null ? setting.getCourier().getFullName() : setting.getTargetOrganization().getName()));

        return "accounts/reward-details";
    }
}