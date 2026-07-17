package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.dto.BusinessDayInvoiceSummaryDTO;
import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.BusinessDayService;
import com.shipment.shippinggo.service.InvoiceService;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.PdfService;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final OrganizationService organizationService;
    private final PdfService pdfService;
    private final BusinessDayService businessDayService;

    public InvoiceController(InvoiceService invoiceService,
                             OrganizationService organizationService,
                             PdfService pdfService,
                             BusinessDayService businessDayService) {
        this.invoiceService = invoiceService;
        this.organizationService = organizationService;
        this.pdfService = pdfService;
        this.businessDayService = businessDayService;
    }

    /**
     * صفحة الفواتير - عرض ملخصات أيام العمل فقط (بدون تحميل الفواتير)
     */
    @GetMapping
    public String listInvoices(@CurrentOrganization Organization org,
                               @AuthenticationPrincipal User user,
                               Model model) {
        if (org == null) {
            org = organizationService.getOrganizationByUser(user);
        }
        if (org == null) {
            return "redirect:/dashboard";
        }

        List<BusinessDayInvoiceSummaryDTO> summaries = invoiceService.getBusinessDaySummaries(org.getId());

        model.addAttribute("summaries", summaries);
        model.addAttribute("organization", org);

        return "invoices/list";
    }

    private static final int PAGE_SIZE = 50;

    /**
     * عرض فواتير يوم عمل محدد (أول 50 فاتورة)
     */
    @GetMapping("/day/{businessDayId}")
    public String listInvoicesByDay(@PathVariable Long businessDayId,
                                     @RequestParam(required = false) String code,
                                     @RequestParam(required = false) Long courierId,
                                     @CurrentOrganization Organization org,
                                     @AuthenticationPrincipal User user,
                                     Model model) {
        if (org == null) {
            org = organizationService.getOrganizationByUser(user);
        }
        if (org == null) {
            return "redirect:/dashboard";
        }

        BusinessDay businessDay = businessDayService.getById(businessDayId);
        if (businessDay == null) {
            return "redirect:/invoices";
        }

        List<Invoice> invoices;
        boolean hasMore = false;
        long totalCount = 0;

        String searchCode = (code != null && !code.trim().isEmpty()) ? code.trim() : null;
        
        Page<Invoice> page = invoiceService.searchInvoicesPaged(org.getId(), businessDayId, searchCode, courierId, 0, PAGE_SIZE);
        invoices = page.getContent();
        hasMore = page.hasNext();
        totalCount = page.getTotalElements();

        model.addAttribute("invoices", invoices);
        model.addAttribute("businessDay", businessDay);
        model.addAttribute("organization", org);
        model.addAttribute("code", searchCode);
        model.addAttribute("courierId", courierId);
        model.addAttribute("couriers", organizationService.buildCourierDisplayNameMap(org));
        model.addAttribute("hasMore", hasMore);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("currentPage", 0);

        return "invoices/day";
    }

    /**
     * REST endpoint - تحميل المزيد من الفواتير (Infinite Scroll)
     */
    @GetMapping("/day/{businessDayId}/load-more")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loadMoreInvoices(
            @PathVariable Long businessDayId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Long courierId,
            @CurrentOrganization Organization org,
            @AuthenticationPrincipal User user) {
        if (org == null) {
            org = organizationService.getOrganizationByUser(user);
        }
        if (org == null) {
            return ResponseEntity.status(403).build();
        }

        String searchCode = (code != null && !code.trim().isEmpty()) ? code.trim() : null;

        Page<Invoice> invoicePage = invoiceService.searchInvoicesPaged(
                org.getId(), businessDayId, searchCode, courierId, page, PAGE_SIZE);

        Map<Long, String> couriersMap = organizationService.buildCourierDisplayNameMap(org);

        List<Map<String, Object>> invoiceDataList = new ArrayList<>();
        for (Invoice invoice : invoicePage.getContent()) {
            Map<String, Object> invoiceData = new LinkedHashMap<>();
            invoiceData.put("id", invoice.getId());
            invoiceData.put("invoiceNumber", invoice.getInvoiceNumber());
            invoiceData.put("totalAmount", invoice.getTotalAmount());
            invoiceData.put("createdAt", invoice.getCreatedAt() != null ?
                    invoice.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");

            // Order info
            if (invoice.getOrder() != null) {
                invoiceData.put("orderCode", invoice.getOrder().getCode());
                invoiceData.put("recipientName", invoice.getOrder().getRecipientName());
                invoiceData.put("recipientPhone", invoice.getOrder().getRecipientPhone());
                if (invoice.getOrder().getAssignedToCourier() != null) {
                    Long cId = invoice.getOrder().getAssignedToCourier().getId();
                    invoiceData.put("courierName", couriersMap.getOrDefault(cId, invoice.getOrder().getAssignedToCourier().getFullName()));
                } else {
                    invoiceData.put("courierName", "غير محدد");
                }
            }

            invoiceDataList.add(invoiceData);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("invoices", invoiceDataList);
        response.put("hasMore", invoicePage.hasNext());
        response.put("currentPage", page);
        response.put("totalPages", invoicePage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    /**
     * إنشاء فواتير من أوردرات محددة (من صفحة يوم العمل)
     */
    @PostMapping("/generate-for-orders")
    public String generateForOrders(@RequestParam Long businessDayId,
                                     @RequestParam String orderIds,
                                     @RequestParam(required = false) String redirectUrl,
                                     @CurrentOrganization Organization org,
                                     @AuthenticationPrincipal User user,
                                     RedirectAttributes redirectAttributes) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER && user.getRole() != Role.ACCOUNTANT) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح بإنشاء الفواتير");
            if (redirectUrl != null && !redirectUrl.isEmpty()) return "redirect:" + redirectUrl;
            return "redirect:/business-days/" + businessDayId;
        }

        if (org == null) {
            org = organizationService.getOrganizationByUser(user);
        }

        BusinessDay businessDay = businessDayService.getById(businessDayId);
        if (businessDay == null) {
            redirectAttributes.addFlashAttribute("error", "يوم العمل غير موجود");
            if (redirectUrl != null && !redirectUrl.isEmpty()) return "redirect:" + redirectUrl;
            return "redirect:/business-days";
        }

        try {
            List<Long> ids = Arrays.stream(orderIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            List<Invoice> invoices = invoiceService.generateInvoicesForOrders(org, businessDay, ids, user);
            redirectAttributes.addFlashAttribute("success",
                    "تم إنشاء " + invoices.size() + " فاتورة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "فشل إنشاء الفواتير: " + e.getMessage());
        }

        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/business-days/" + businessDayId;
    }

    /**
     * طباعة فاتورة (PDF في نافذة جديدة)
     */
    @GetMapping("/{id}/print")
    public ResponseEntity<byte[]> printInvoice(@PathVariable Long id,
                                                @CurrentOrganization Organization org,
                                                @AuthenticationPrincipal User user) {
        Invoice invoice = invoiceService.findById(id);
        if (invoice == null) {
            return ResponseEntity.notFound().build();
        }

        if (org == null) {
            org = organizationService.getOrganizationByUser(user);
        }

        byte[] pdfBytes = pdfService.generateInvoicePdf(invoice, List.of(invoice.getOrder()), org);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=Invoice-" + invoice.getInvoiceNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * طباعة فواتير مجمعة (عدة فواتير في PDF واحد)
     */
    @GetMapping("/bulk-print")
    public ResponseEntity<byte[]> bulkPrint(@RequestParam String invoiceIds,
                                             @CurrentOrganization Organization org,
                                             @AuthenticationPrincipal User user) {
        if (org == null) {
            org = organizationService.getOrganizationByUser(user);
        }

        List<Long> ids = Arrays.stream(invoiceIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());

        List<Invoice> invoices = new ArrayList<>();
        for (Long invoiceId : ids) {
            Invoice invoice = invoiceService.findById(invoiceId);
            if (invoice != null) {
                invoices.add(invoice);
            }
        }

        if (invoices.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdfBytes = pdfService.generateBulkInvoicePdf(invoices, org);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=Invoices-Bulk.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * تحميل فاتورة PDF (للتوافق مع الكود القديم)
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id,
                                                   @CurrentOrganization Organization org,
                                                   @AuthenticationPrincipal User user) {
        return printInvoice(id, org, user);
    }
}