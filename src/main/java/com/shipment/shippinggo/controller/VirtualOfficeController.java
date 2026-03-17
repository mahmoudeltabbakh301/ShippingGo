package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.VirtualOffice;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrganizationType;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.repository.VirtualOfficeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Controller
@RequestMapping("/virtual-offices")
public class VirtualOfficeController {

    @Autowired
    private VirtualOfficeRepository virtualOfficeRepository;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private com.shipment.shippinggo.service.AccountService accountService;

    @GetMapping
    public String listVirtualOffices(Model model, @AuthenticationPrincipal User currentUser) {
        Organization currentOrg = organizationService.getOrganizationByUser(currentUser);
        if (currentOrg == null || currentOrg.getType() == OrganizationType.STORE) {
            return "redirect:/dashboard";
        }

        List<VirtualOffice> virtualOffices = virtualOfficeRepository.findByParentOrganizationId(currentOrg.getId());
        model.addAttribute("virtualOffices", virtualOffices);
        model.addAttribute("currentOrg", currentOrg);

        return "virtual-offices/list";
    }

    @PostMapping("/create")
    @Transactional
    public String createVirtualOffice(@RequestParam String name,
            @RequestParam(required = false) com.shipment.shippinggo.enums.CommissionType commissionType,
            @RequestParam(required = false) java.math.BigDecimal commissionValue,
            @RequestParam(required = false) java.math.BigDecimal rejectionCommission,
            @RequestParam(required = false) java.math.BigDecimal cancellationCommission,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {
        Organization currentOrg = organizationService.getOrganizationByUser(currentUser);
        if (currentOrg == null || currentOrg.getType() == OrganizationType.STORE) {
            return "redirect:/dashboard";
        }

        if (virtualOfficeRepository.existsByNameAndParentOrganizationId(name, currentOrg.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "اسم المكتب الافتراضي موجود بالفعل داخل منظمتك");
            return "redirect:/virtual-offices";
        }

        String uniqueSuffix = System.currentTimeMillis() + "" + (int) (Math.random() * 1000);
        VirtualOffice virtualOffice = VirtualOffice.builder()
                .name(name)
                .address(currentOrg.getAddress())
                .phone("VO" + uniqueSuffix) // Ensure phone uniqueness
                .email("vo_" + uniqueSuffix + "@shippinggo.local") // Ensure email uniqueness
                .parentOrganization(currentOrg)
                .admin(currentUser)
                .build();
        virtualOffice.setVirtual(true);

        virtualOffice = virtualOfficeRepository.save(virtualOffice);

        // Save commissions if provided
        if (commissionType != null && commissionValue != null) {
            if (rejectionCommission == null)
                rejectionCommission = java.math.BigDecimal.ZERO;
            if (cancellationCommission == null)
                cancellationCommission = java.math.BigDecimal.ZERO;

            accountService.saveOrganizationCommission(currentOrg, virtualOffice,
                    commissionType, commissionValue, rejectionCommission, cancellationCommission, null);
        }

        redirectAttributes.addFlashAttribute("successMessage", "تم إنشاء المكتب الافتراضي بنجاح");
        return "redirect:/virtual-offices";
    }

    @PostMapping("/delete/{id}")
    public String deleteVirtualOffice(@PathVariable Long id,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {
        Organization currentOrg = organizationService.getOrganizationByUser(currentUser);
        if (currentOrg != null && currentOrg.getType() == OrganizationType.STORE) {
            return "redirect:/dashboard";
        }
        VirtualOffice vo = virtualOfficeRepository.findById(id).orElse(null);

        if (vo != null && currentOrg != null && vo.getParentOrganization().getId().equals(currentOrg.getId())) {
            virtualOfficeRepository.delete(vo);
            redirectAttributes.addFlashAttribute("successMessage", "تم حذف المكتب الافتراضي بنجاح");
        }

        return "redirect:/virtual-offices";
    }

    @PostMapping("/update/{id}")
    @Transactional
    public String updateVirtualOffice(@PathVariable Long id,
            @RequestParam String name,
            @RequestParam(required = false) com.shipment.shippinggo.enums.CommissionType commissionType,
            @RequestParam(required = false) java.math.BigDecimal commissionValue,
            @RequestParam(required = false) java.math.BigDecimal rejectionCommission,
            @RequestParam(required = false) java.math.BigDecimal cancellationCommission,
            @AuthenticationPrincipal User currentUser,
            RedirectAttributes redirectAttributes) {
        Organization currentOrg = organizationService.getOrganizationByUser(currentUser);
        if (currentOrg == null || currentOrg.getType() == OrganizationType.STORE) {
            return "redirect:/dashboard";
        }

        VirtualOffice vo = virtualOfficeRepository.findById(id).orElse(null);
        if (vo == null || !vo.getParentOrganization().getId().equals(currentOrg.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "المكتب الافتراضي غير موجود أو لا يتبع منظمتك");
            return "redirect:/virtual-offices";
        }

        // التحقق من تكرار الاسم داخل نفس المنظمة فقط (يسمح بالتكرار بين منظمات مختلفة)
        if (!vo.getName().equals(name)
                && virtualOfficeRepository.existsByNameAndParentOrganizationId(name, currentOrg.getId())) {
            redirectAttributes.addFlashAttribute("errorMessage", "اسم المكتب الافتراضي موجود بالفعل داخل منظمتك");
            return "redirect:/virtual-offices";
        }

        vo.setName(name);
        virtualOfficeRepository.save(vo);

        // تحديث العمولات إذا تم توفيرها
        if (commissionType != null && commissionValue != null) {
            if (rejectionCommission == null)
                rejectionCommission = java.math.BigDecimal.ZERO;
            if (cancellationCommission == null)
                cancellationCommission = java.math.BigDecimal.ZERO;

            accountService.saveOrganizationCommission(currentOrg, vo,
                    commissionType, commissionValue, rejectionCommission, cancellationCommission, null);
        }

        redirectAttributes.addFlashAttribute("successMessage", "تم تحديث المكتب الافتراضي بنجاح");
        return "redirect:/virtual-offices";
    }
}
