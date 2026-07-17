package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.entity.Vehicle;
import com.shipment.shippinggo.enums.OrganizationType;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.enums.VehicleStatus;
import com.shipment.shippinggo.enums.VehicleType;
import com.shipment.shippinggo.service.QrCodeService;
import com.shipment.shippinggo.service.TripService;
import com.shipment.shippinggo.service.VehicleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final TripService tripService;
    private final QrCodeService qrCodeService;

    public VehicleController(VehicleService vehicleService, TripService tripService, QrCodeService qrCodeService) {
        this.vehicleService = vehicleService;
        this.tripService = tripService;
        this.qrCodeService = qrCodeService;
    }

    /**
     * عرض قائمة الشاحنات
     */
    @GetMapping
    public String listVehicles(@CurrentOrganization Organization org, @AuthenticationPrincipal User user, Model model) {
        if (org.getType() == OrganizationType.CLIENT) {
            return "redirect:/dashboard";
        }

        List<Vehicle> vehicles = vehicleService.getVehiclesByOrganization(org.getId());
        model.addAttribute("vehicles", vehicles);
        model.addAttribute("organization", org);
        model.addAttribute("vehicleTypes", VehicleType.values());
        model.addAttribute("vehicleStatuses", VehicleStatus.values());

        // إحصائيات
        model.addAttribute("totalVehicles", vehicleService.countActiveVehicles(org.getId()));
        model.addAttribute("availableCount", vehicleService.countByStatus(org.getId(), VehicleStatus.AVAILABLE));
        model.addAttribute("onTripCount", vehicleService.countByStatus(org.getId(), VehicleStatus.ON_TRIP));
        model.addAttribute("maintenanceCount", vehicleService.countByStatus(org.getId(), VehicleStatus.MAINTENANCE));

        return "vehicles/list";
    }

    /**
     * إنشاء شاحنة جديدة
     */
    @PostMapping("/create")
    public String createVehicle(@CurrentOrganization Organization org, @AuthenticationPrincipal User user,
                                @RequestParam String plateNumber,
                                @RequestParam VehicleType vehicleType,
                                @RequestParam(required = false) String model,
                                @RequestParam(required = false) String color,
                                @RequestParam(required = false) Integer capacity,
                                @RequestParam(required = false) String driverName,
                                @RequestParam(required = false) String driverPhone,
                                @RequestParam(required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح: المشرف أو المدير فقط يمكنه إضافة شاحنات");
            return "redirect:/vehicles";
        }

        try {
            vehicleService.createVehicle(org, plateNumber, vehicleType, model, color, capacity,
                    driverName, driverPhone, notes);
            redirectAttributes.addFlashAttribute("success", "تم إضافة الشاحنة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vehicles";
    }

    /**
     * تعديل بيانات شاحنة
     */
    @PostMapping("/{id}/update")
    public String updateVehicle(@PathVariable Long id,
                                @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
                                @RequestParam String plateNumber,
                                @RequestParam VehicleType vehicleType,
                                @RequestParam(required = false) String model,
                                @RequestParam(required = false) String color,
                                @RequestParam(required = false) Integer capacity,
                                @RequestParam(required = false) String driverName,
                                @RequestParam(required = false) String driverPhone,
                                @RequestParam(required = false) String notes,
                                RedirectAttributes redirectAttributes) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح");
            return "redirect:/vehicles";
        }

        try {
            vehicleService.updateVehicle(id, org.getId(), plateNumber, vehicleType, model, color, capacity,
                    driverName, driverPhone, notes);
            redirectAttributes.addFlashAttribute("success", "تم تحديث بيانات الشاحنة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vehicles";
    }

    /**
     * حذف شاحنة (Soft Delete)
     */
    @PostMapping("/{id}/delete")
    public String deleteVehicle(@PathVariable Long id,
                                @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
                                RedirectAttributes redirectAttributes) {
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.MANAGER) {
            redirectAttributes.addFlashAttribute("error", "غير مصرح");
            return "redirect:/vehicles";
        }

        try {
            vehicleService.deleteVehicle(id, org.getId());
            redirectAttributes.addFlashAttribute("success", "تم حذف الشاحنة بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vehicles";
    }

    /**
     * تغيير حالة الشاحنة
     */
    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam VehicleStatus status,
                               @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttributes) {
        try {
            vehicleService.updateStatus(id, org.getId(), status);
            redirectAttributes.addFlashAttribute("success", "تم تحديث حالة الشاحنة");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vehicles";
    }

    /**
     * طباعة QR Code لشاحنة
     */
    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> getVehicleQrCode(@PathVariable Long id,
                                                    @CurrentOrganization Organization org) {
        Vehicle vehicle = vehicleService.getById(id);
        if (vehicle == null || !vehicle.getOrganization().getId().equals(org.getId())) {
            return ResponseEntity.notFound().build();
        }

        byte[] qrImage = qrCodeService.generateQrCodeImage(vehicle.getCode(), 300, 300);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("filename", "vehicle_" + vehicle.getCode() + ".png");

        return ResponseEntity.ok().headers(headers).body(qrImage);
    }

    /**
     * عرض تفاصيل شاحنة وتقاريرها
     */
    @GetMapping("/{id}")
    public String viewVehicle(@PathVariable Long id,
                              @CurrentOrganization Organization org, @AuthenticationPrincipal User user,
                              Model model) {
        Vehicle vehicle = vehicleService.getById(id);
        if (vehicle == null || !vehicle.getOrganization().getId().equals(org.getId())) {
            return "redirect:/vehicles";
        }

        model.addAttribute("vehicle", vehicle);
        model.addAttribute("organization", org);
        model.addAttribute("vehicleTypes", VehicleType.values());
        model.addAttribute("vehicleStatuses", VehicleStatus.values());

        // تقارير الشاحنة
        model.addAttribute("vehicleTrips", tripService.getTripsByVehicle(id));
        model.addAttribute("totalTrips", tripService.getCompletedTripsByVehicle(id));
        model.addAttribute("totalOrdersTransported", tripService.getOrdersTransportedByVehicle(id));

        return "vehicles/view";
    }
}
