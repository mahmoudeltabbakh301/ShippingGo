package com.shipment.shippinggo.controller.api;

import com.shipment.shippinggo.annotation.CurrentOrganization;
import com.shipment.shippinggo.entity.CustomArea;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.service.GovernorateZoneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API لتوفير بيانات المناطق الجغرافية للـ Frontend (AJAX).
 * الهيكل: محافظة → مركز → منطقة
 */
@RestController
@RequestMapping("/api/zones")
public class GovernorateZoneApiController {

    private final GovernorateZoneService governorateZoneService;

    public GovernorateZoneApiController(GovernorateZoneService governorateZoneService) {
        this.governorateZoneService = governorateZoneService;
    }

    /**
     * الحصول على مراكز محافظة معينة
     * GET /api/zones/{governorate} → [{"name": "طنطا", "nameEn": "Tanta"}, ...]
     */
    @GetMapping("/{governorate}")
    public ResponseEntity<List<Map<String, String>>> getCenters(@PathVariable String governorate) {
        List<GovernorateZoneService.DistrictInfo> centers = governorateZoneService.getCenters(governorate);
        List<Map<String, String>> result = centers.stream()
                .map(d -> Map.of("name", d.getName(), "nameEn", d.getNameEn() != null ? d.getNameEn() : ""))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * الحصول على المناطق داخل مركز معين (مع المناطق المخصصة إذا كان المستخدم مسجل)
     * GET /api/zones/{governorate}/{center}?orgId=123
     */
    @GetMapping("/{governorate}/{center}")
    public ResponseEntity<List<Map<String, Object>>> getAreas(
            @PathVariable String governorate, @PathVariable String center,
            @RequestParam(required = false) Long orgId) {
        List<GovernorateZoneService.AreaInfo> areas;
        if (orgId != null) {
            areas = governorateZoneService.getMergedAreas(governorate, center, orgId);
        } else {
            areas = governorateZoneService.getAreas(governorate, center);
        }
        List<Map<String, Object>> result = areas.stream()
                .map(a -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("name", a.getName());
                    map.put("nameEn", a.getNameEn() != null ? a.getNameEn() : "");
                    map.put("custom", a.isCustom());
                    if (a.getCustomId() != null) {
                        map.put("customId", a.getCustomId());
                    }
                    return map;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * إضافة منطقة مخصصة
     * POST /api/zones/{governorate}/{center}/custom
     */
    @PostMapping("/{governorate}/{center}/custom")
    public ResponseEntity<Map<String, Object>> addCustomArea(
            @CurrentOrganization Organization org,
            @PathVariable String governorate,
            @PathVariable String center,
            @RequestBody Map<String, String> body) {
        try {
            if (org == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "لا توجد منظمة"));
            }
            String name = body.get("name");
            String nameEn = body.get("nameEn");

            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "اسم المنطقة مطلوب"));
            }

            Governorate gov = Governorate.valueOf(governorate);
            CustomArea customArea = governorateZoneService.addCustomArea(org, gov, center, name.trim(), nameEn);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", customArea.getId());
            response.put("name", customArea.getName());
            response.put("nameEn", customArea.getNameEn() != null ? customArea.getNameEn() : "");
            response.put("custom", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * حذف منطقة مخصصة
     * DELETE /api/zones/custom/{id}
     */
    @DeleteMapping("/custom/{id}")
    public ResponseEntity<Map<String, Object>> deleteCustomArea(
            @CurrentOrganization Organization org,
            @PathVariable Long id) {
        try {
            if (org == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "لا توجد منظمة"));
            }
            governorateZoneService.deleteCustomArea(id, org.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * البحث عن منطقة
     * GET /api/zones/search?q=طنطا
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, String>>> searchDistricts(@RequestParam String q) {
        List<GovernorateZoneService.SearchResult> results = governorateZoneService.searchDistricts(q);
        List<Map<String, String>> response = results.stream()
                .map(r -> Map.of(
                        "governorateKey", r.getGovernorateKey(),
                        "governorateName", r.getGovernorateArabicName(),
                        "districtName", r.getDistrict().getName(),
                        "districtNameEn", r.getDistrict().getNameEn() != null ? r.getDistrict().getNameEn() : ""
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}
