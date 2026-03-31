package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.service.OrganizationService;
import com.shipment.shippinggo.service.StorageService;
import com.shipment.shippinggo.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;
    private final StorageService storageService;

    private final OrganizationService organizationService;

    public ProfileController(UserService userService, StorageService storageService,
            OrganizationService organizationService) {
        this.userService = userService;
        this.storageService = storageService;
        this.organizationService = organizationService;
    }

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal User user, Model model) {
        // Refresh user from DB to get latest data
        User currentUser = userService.findById(user.getId());
        model.addAttribute("user", currentUser);

        Organization org = organizationService.getOrganizationByUser(user);
        if (org != null) {
            model.addAttribute("organization", org);
        }
        model.addAttribute("governorates", com.shipment.shippinggo.enums.Governorate.values());

        return "profile";
    }

    @GetMapping("/{id}")
    public String viewPublicProfile(@PathVariable Long id, Model model) {
        User targetUser = userService.findById(id);
        if (targetUser == null) {
            return "redirect:/";
        }
        model.addAttribute("targetUser", targetUser);

        Organization org = organizationService.getOrganizationByUser(targetUser);
        if (org != null) {
            model.addAttribute("organization", org);
        }

        return "public-profile";
    }

    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal User user,
            @RequestParam("fullName") String fullName,
            @RequestParam("phone") String phone,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findById(user.getId());
            userService.updateUser(currentUser, fullName, phone);
            redirectAttributes.addFlashAttribute("success", "تم تحديث الملف الشخصي بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "فشل تحديث الملف الشخصي: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/update-organization")
    public String updateOrganizationProfile(@AuthenticationPrincipal User user,
            @RequestParam String about,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) com.shipment.shippinggo.enums.Governorate governorate,
            @RequestParam(defaultValue = "false") boolean acceptsInternal,
            @RequestParam(defaultValue = "false") boolean acceptsExternal,
            RedirectAttributes redirectAttributes) {
        try {
            Organization org = organizationService.getOrganizationByUser(user);
            if (org == null) {
                throw new RuntimeException("لا توجد منظمة مرتبطة بحسابك");
            }

            // Only admin can update organization profile
            if (!org.getAdmin().getId().equals(user.getId())) {
                throw new RuntimeException("فقط مدير المنظمة يمكنه تحديث هذه البيانات");
            }

            org.setAbout(about);
            if (latitude != null)
                org.setLatitude(latitude);
            if (longitude != null)
                org.setLongitude(longitude);

            // New Settings
            if (governorate != null)
                org.setGovernorate(governorate);
            org.setAcceptsInternalShipments(acceptsInternal);
            org.setAcceptsExternalShipments(acceptsExternal);

            organizationService.saveOrganization(org);
            redirectAttributes.addFlashAttribute("success", "تم تحديث بيانات المنظمة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "فشل تحديث بيانات المنظمة: " + e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/upload-image")
    public String uploadImage(@AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            String filename = storageService.store(file);
            User currentUser = userService.findById(user.getId());
            userService.updateProfilePicture(currentUser, filename);
            redirectAttributes.addFlashAttribute("success", "تم تحديث الصورة الشخصية بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "فشل رفع الصورة: " + e.getMessage());
        }
        return "redirect:/profile";
    }
}
