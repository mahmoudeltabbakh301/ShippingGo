package com.shipment.shippinggo.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shipment.shippinggo.entity.CustomArea;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.enums.Governorate;
import com.shipment.shippinggo.repository.CustomAreaRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;

/**
 * خدمة المناطق الجغرافية — تحمل بيانات المحافظات والمراكز والمناطق من ملفات JSON
 * وتوفرها للاستخدام في الأوردرات والتوزيع التلقائي والتقارير.
 *
 * الهيكل: محافظة → مركز → منطقة
 */
@Service
public class GovernorateZoneService {

    private static final Logger log = LoggerFactory.getLogger(GovernorateZoneService.class);

    private final CustomAreaRepository customAreaRepository;

    public GovernorateZoneService(CustomAreaRepository customAreaRepository) {
        this.customAreaRepository = customAreaRepository;
    }

    /** المحافظات مع مراكزها (المستوى الأول + الثاني) */
    private Map<String, GovernorateData> governorateMap = new LinkedHashMap<>();

    /** المناطق داخل المراكز (المستوى الثالث): govKey → centerName → List<AreaInfo> */
    private Map<String, Map<String, List<AreaInfo>>> centerAreasMap = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        loadGovernorates();
        loadCenterAreas();
    }

    private void loadGovernorates() {
        try {
            ClassPathResource resource = new ClassPathResource("data/governorate-zones.json");
            try (InputStream is = resource.getInputStream()) {
                ObjectMapper mapper = new ObjectMapper();
                governorateMap = mapper.readValue(is, new TypeReference<LinkedHashMap<String, GovernorateData>>() {});
                int totalCenters = governorateMap.values().stream()
                        .mapToInt(g -> g.getDistricts() != null ? g.getDistricts().size() : 0)
                        .sum();
                log.info("Loaded {} governorates with {} centers from governorate-zones.json",
                        governorateMap.size(), totalCenters);
            }
        } catch (Exception e) {
            log.error("Failed to load governorate zones data: {}", e.getMessage(), e);
        }
    }

    private void loadCenterAreas() {
        try {
            ClassPathResource resource = new ClassPathResource("data/center-areas.json");
            if (!resource.exists()) {
                log.info("No center-areas.json found, areas feature disabled");
                return;
            }
            try (InputStream is = resource.getInputStream()) {
                ObjectMapper mapper = new ObjectMapper();
                centerAreasMap = mapper.readValue(is,
                        new TypeReference<LinkedHashMap<String, Map<String, List<AreaInfo>>>>() {});
                int totalAreas = centerAreasMap.values().stream()
                        .flatMap(m -> m.values().stream())
                        .mapToInt(List::size)
                        .sum();
                log.info("Loaded {} areas across centers from center-areas.json", totalAreas);
            }
        } catch (Exception e) {
            log.error("Failed to load center areas data: {}", e.getMessage(), e);
        }
    }

    // ==================== المراكز (المستوى الثاني) ====================

    /**
     * الحصول على قائمة المراكز لمحافظة معينة (كانت districts سابقاً)
     */
    public List<DistrictInfo> getCenters(String governorateKey) {
        GovernorateData data = governorateMap.get(governorateKey);
        if (data == null || data.getDistricts() == null) {
            return Collections.emptyList();
        }
        return data.getDistricts();
    }

    /** Alias for backward compatibility */
    public List<DistrictInfo> getDistricts(String governorateKey) {
        return getCenters(governorateKey);
    }

    public List<DistrictInfo> getDistricts(com.shipment.shippinggo.enums.Governorate governorate) {
        if (governorate == null) return Collections.emptyList();
        return getCenters(governorate.name());
    }

    // ==================== المناطق (المستوى الثالث) ====================

    /**
     * الحصول على قائمة المناطق داخل مركز معين (بدون مناطق مخصصة)
     */
    public List<AreaInfo> getAreas(String governorateKey, String centerName) {
        Map<String, List<AreaInfo>> centersMap = centerAreasMap.get(governorateKey);
        if (centersMap == null) return Collections.emptyList();
        List<AreaInfo> areas = centersMap.get(centerName);
        return areas != null ? areas : Collections.emptyList();
    }

    public List<AreaInfo> getAreas(com.shipment.shippinggo.enums.Governorate governorate, String centerName) {
        if (governorate == null || centerName == null) return Collections.emptyList();
        return getAreas(governorate.name(), centerName);
    }

    /**
     * الحصول على المناطق الأساسية + المخصصة للمنظمة
     */
    public List<AreaInfo> getMergedAreas(String governorateKey, String centerName, Long organizationId) {
        List<AreaInfo> baseAreas = new ArrayList<>(getAreas(governorateKey, centerName));

        if (organizationId != null) {
            try {
                Governorate gov = Governorate.valueOf(governorateKey);
                List<CustomArea> customAreas = customAreaRepository
                        .findByOrganizationIdAndGovernorateAndCenter(organizationId, gov, centerName);
                for (CustomArea ca : customAreas) {
                    AreaInfo ai = new AreaInfo();
                    ai.setName(ca.getName());
                    ai.setNameEn(ca.getNameEn());
                    ai.setCustom(true);
                    ai.setCustomId(ca.getId());
                    baseAreas.add(ai);
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid governorate key for custom areas: {}", governorateKey);
            }
        }
        return baseAreas;
    }

    // ==================== المناطق المخصصة ====================

    /**
     * إضافة منطقة مخصصة لمنظمة
     */
    @Transactional
    public CustomArea addCustomArea(Organization organization, Governorate governorate,
                                     String center, String name, String nameEn) {
        // التحقق من عدم التكرار
        if (customAreaRepository.existsByOrganizationIdAndGovernorateAndCenterAndName(
                organization.getId(), governorate, center, name)) {
            throw new RuntimeException("المنطقة موجودة بالفعل: " + name);
        }

        CustomArea customArea = CustomArea.builder()
                .organization(organization)
                .governorate(governorate)
                .center(center)
                .name(name)
                .nameEn(nameEn != null && !nameEn.trim().isEmpty() ? nameEn.trim() : null)
                .build();

        return customAreaRepository.save(customArea);
    }

    /**
     * حذف منطقة مخصصة
     */
    @Transactional
    public void deleteCustomArea(Long customAreaId, Long organizationId) {
        CustomArea area = customAreaRepository.findById(customAreaId)
                .orElseThrow(() -> new RuntimeException("المنطقة غير موجودة"));
        if (!area.getOrganization().getId().equals(organizationId)) {
            throw new RuntimeException("لا يمكنك حذف هذه المنطقة");
        }
        customAreaRepository.delete(area);
    }

    /**
     * الحصول على كل المناطق المخصصة لمنظمة
     */
    public List<CustomArea> getCustomAreas(Long organizationId) {
        return customAreaRepository.findByOrganizationIdOrderByGovernorateAscCenterAscNameAsc(organizationId);
    }

    // ==================== بقية الدوال ====================

    public Map<String, GovernorateData> getAllGovernorates() {
        return Collections.unmodifiableMap(governorateMap);
    }

    public List<SearchResult> searchDistricts(String query) {
        if (query == null || query.trim().isEmpty()) return Collections.emptyList();
        String q = query.trim().toLowerCase();

        List<SearchResult> results = new ArrayList<>();
        for (Map.Entry<String, GovernorateData> entry : governorateMap.entrySet()) {
            String govKey = entry.getKey();
            GovernorateData govData = entry.getValue();
            if (govData.getDistricts() == null) continue;

            for (DistrictInfo district : govData.getDistricts()) {
                if (district.getName().contains(q) ||
                    (district.getNameEn() != null && district.getNameEn().toLowerCase().contains(q))) {
                    results.add(new SearchResult(govKey, govData.getArabicName(), district));
                }
            }
        }
        return results;
    }

    public boolean isValidDistrict(String governorateKey, String districtName) {
        List<DistrictInfo> districts = getDistricts(governorateKey);
        return districts.stream().anyMatch(d -> d.getName().equals(districtName));
    }

    // ==================== DTOs ====================

    public static class GovernorateData {
        private String arabicName;
        private List<DistrictInfo> districts;

        public String getArabicName() { return arabicName; }
        public void setArabicName(String arabicName) { this.arabicName = arabicName; }
        public List<DistrictInfo> getDistricts() { return districts; }
        public void setDistricts(List<DistrictInfo> districts) { this.districts = districts; }
    }

    /** معلومات المركز (كان DistrictInfo سابقاً — الاسم محتفظ به للتوافق) */
    public static class DistrictInfo {
        private String name;
        private String nameEn;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getNameEn() { return nameEn; }
        public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    }

    /** معلومات المنطقة داخل المركز */
    public static class AreaInfo {
        private String name;
        private String nameEn;
        private boolean custom;
        private Long customId;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getNameEn() { return nameEn; }
        public void setNameEn(String nameEn) { this.nameEn = nameEn; }
        public boolean isCustom() { return custom; }
        public void setCustom(boolean custom) { this.custom = custom; }
        public Long getCustomId() { return customId; }
        public void setCustomId(Long customId) { this.customId = customId; }
    }

    public static class SearchResult {
        private final String governorateKey;
        private final String governorateArabicName;
        private final DistrictInfo district;

        public SearchResult(String governorateKey, String governorateArabicName, DistrictInfo district) {
            this.governorateKey = governorateKey;
            this.governorateArabicName = governorateArabicName;
            this.district = district;
        }

        public String getGovernorateKey() { return governorateKey; }
        public String getGovernorateArabicName() { return governorateArabicName; }
        public DistrictInfo getDistrict() { return district; }
    }
}
