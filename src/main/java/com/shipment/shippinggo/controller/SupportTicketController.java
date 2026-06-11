package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.SupportTicket;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.TicketCategory;
import com.shipment.shippinggo.enums.TicketPriority;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.SupportTicketService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/support")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;
    private final OrganizationService organizationService;

    public SupportTicketController(SupportTicketService supportTicketService,
                                    OrganizationService organizationService) {
        this.supportTicketService = supportTicketService;
        this.organizationService = organizationService;
    }

    @GetMapping
    public String listTickets(@AuthenticationPrincipal User user, Model model) {
        Organization org = organizationService.getOrganizationByUser(user);
        List<SupportTicket> tickets = org != null
                ? supportTicketService.getTicketsByOrganization(org.getId())
                : List.of();

        model.addAttribute("tickets", tickets);
        model.addAttribute("orgName", org != null ? org.getName() : "");
        model.addAttribute("pageTitle", "تذاكر الدعم الفني");
        return "support/tickets";
    }

    @GetMapping("/new")
    public String newTicketForm(Model model) {
        model.addAttribute("priorities", TicketPriority.values());
        model.addAttribute("categories", TicketCategory.values());
        model.addAttribute("pageTitle", "تذكرة دعم جديدة");
        return "support/ticket-form";
    }

    @PostMapping("/create")
    public String createTicket(@AuthenticationPrincipal User user,
                                @RequestParam String subject,
                                @RequestParam String description,
                                @RequestParam TicketPriority priority,
                                @RequestParam TicketCategory category,
                                RedirectAttributes redirectAttributes) {
        try {
            Organization org = organizationService.getOrganizationByUser(user);
            SupportTicket ticket = supportTicketService.createTicket(user, org, subject, description, priority, category);
            redirectAttributes.addFlashAttribute("success", "تم إنشاء تذكرة الدعم بنجاح - " + ticket.getTicketNumber());
            return "redirect:/support/" + ticket.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/support/new";
        }
    }

    @GetMapping("/{id}")
    public String ticketDetail(@PathVariable Long id,
                                @AuthenticationPrincipal User user,
                                Model model) {
        SupportTicket ticket = supportTicketService.getTicketById(id);
        model.addAttribute("ticket", ticket);
        model.addAttribute("replies", supportTicketService.getTicketReplies(id));
        model.addAttribute("pageTitle", "تذكرة دعم - " + ticket.getTicketNumber());
        return "support/ticket-detail";
    }

    @PostMapping("/{id}/reply")
    public String replyToTicket(@PathVariable Long id,
                                 @RequestParam String message,
                                 @AuthenticationPrincipal User user,
                                 RedirectAttributes redirectAttributes) {
        try {
            supportTicketService.replyToTicket(id, user, message, false);
            redirectAttributes.addFlashAttribute("success", "تم إرسال الرد بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/support/" + id;
    }
}
