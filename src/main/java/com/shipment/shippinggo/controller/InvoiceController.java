package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.service.InvoiceService;
import com.shipment.shippinggo.service.OrganizationService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import com.shipment.shippinggo.entity.Order;
import com.shipment.shippinggo.entity.Invoice;
import com.shipment.shippinggo.service.PdfService;

@Controller
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final OrganizationService organizationService;
    private final PdfService pdfService;

    public InvoiceController(InvoiceService invoiceService, OrganizationService organizationService, PdfService pdfService) {
        this.invoiceService = invoiceService;
        this.organizationService = organizationService;
        this.pdfService = pdfService;
    }

    @GetMapping
    public String listInvoices(@CurrentOrganization Organization org, @AuthenticationPrincipal User user, Model model) {
        if (org == null) {
            org = organizationService.getOrganizationByUser(user);
        }
        
        if (org == null) {
            return "redirect:/dashboard";
        }

        model.addAttribute("invoices", invoiceService.getInvoicesByOrganization(org.getId()));
        model.addAttribute("organization", org);

        return "invoices/list";
    }

    @PostMapping("/generate")
    public String generateInvoice(@CurrentOrganization Organization org, @AuthenticationPrincipal User user, RedirectAttributes redirectAttributes) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER && user.getRole() != Role.ACCOUNTANT) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح بإنشاء الفواتير");
            return "redirect:/invoices";
        }

        if (org == null) {
            org = organizationService.getOrganizationByUser(user);
        }

        try {
            invoiceService.generateInvoice(org);
            redirectAttributes.addFlashAttribute("success", "تم إنشاء الفاتورة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "فشل إنشاء الفاتورة: " + e.getMessage());
        }

        return "redirect:/invoices";
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable Long id, @CurrentOrganization Organization org, @AuthenticationPrincipal User user) {
        Invoice invoice = invoiceService.findById(id);
        if (invoice == null) {
            return ResponseEntity.notFound().build();
        }

        // Security check: Only the organization owner or site admins can download
        if (org == null) {
            org = organizationService.getOrganizationByUser(user);
        }
        
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.SUPER_ADMIN && (org == null || !invoice.getOrganization().getId().equals(org.getId()))) {
            return ResponseEntity.status(403).build();
        }

        List<Order> orders = invoiceService.getOrdersByInvoiceId(id);
        byte[] pdfBytes = pdfService.generateInvoicePdf(invoice, orders);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Invoice-" + invoice.getInvoiceNumber() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/{id}/pay")
    public String markAsPaid(@PathVariable Long id, @CurrentOrganization Organization org, @AuthenticationPrincipal User user, RedirectAttributes redirectAttributes) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER && user.getRole() != Role.ACCOUNTANT) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح بتعديل الفواتير");
            return "redirect:/invoices";
        }

        Invoice invoice = invoiceService.findById(id);
        if (invoice != null) {
            invoice.setStatus("PAID");
            invoiceService.saveInvoice(invoice);
            redirectAttributes.addFlashAttribute("success", "تم تحديث حالة الفاتورة بنجاح");
        } else {
            redirectAttributes.addFlashAttribute("error", "الفاتورة غير موجودة");
        }
        
        return "redirect:/invoices";
    }
}
