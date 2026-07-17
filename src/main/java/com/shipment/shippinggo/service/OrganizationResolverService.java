package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.MembershipStatus;
import com.shipment.shippinggo.repository.*;
import org.hibernate.Hibernate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * خدمة مركزية لاستنتاج المنظمة التابع لها المستخدم والتحقق من العضويات.
 * تُوفر نقطة واحدة موحدة بدلاً من تكرار المنطق في عدة services.
 * النتائج تُخزن مؤقتاً في Caffeine Cache لتسريع العمليات المتكررة.
 */
@Service
public class OrganizationResolverService {

    private final CompanyRepository companyRepository;
    private final OfficeRepository officeRepository;
    private final StoreRepository storeRepository;
    private final VirtualOfficeRepository virtualOfficeRepository;
    private final ClientOrgRepository clientOrgRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public OrganizationResolverService(
            CompanyRepository companyRepository,
            OfficeRepository officeRepository,
            StoreRepository storeRepository,
            VirtualOfficeRepository virtualOfficeRepository,
            ClientOrgRepository clientOrgRepository,
            MembershipRepository membershipRepository,
            UserRepository userRepository,
            OrganizationRepository organizationRepository) {
        this.companyRepository = companyRepository;
        this.officeRepository = officeRepository;
        this.storeRepository = storeRepository;
        this.virtualOfficeRepository = virtualOfficeRepository;
        this.clientOrgRepository = clientOrgRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    /**
     * إيجاد ID المنظمة الأساسية للمستخدم وحفظه في الكاش.
     */
    @Cacheable(value = "userOrganizations", key = "#user.id")
    public Long resolveUserOrganizationId(User user) {
        if (user == null) return null;

        // Fast path: primaryOrganization محدد مسبقاً
        if (user.getPrimaryOrganization() != null) {
            return user.getPrimaryOrganization().getId();
        }

        // Slow fallback: البحث القديم في 5 repos
        Organization resolved = resolveByLegacySearch(user);

        // Lazy-fill: حفظ النتيجة لتسريع الاستدعاءات المستقبلية
        if (resolved != null) {
            user.setPrimaryOrganization(resolved);
            userRepository.save(user);
            return resolved.getId();
        }

        return null;
    }

    /**
     * استرجاع المنظمة ككيان مرتبط بالـ Session الحالية لتفادي مشاكل LazyInitializationException
     */
    public Organization resolveUserOrganization(User user) {
        Long orgId = resolveUserOrganizationId(user);
        if (orgId != null) {
            return organizationRepository.findById(orgId).orElse(null);
        }
        return null;
    }

    /**
     * البحث التقليدي عن منظمة المستخدم — يبحث في كل الـ repositories.
     */
    private Organization resolveByLegacySearch(User user) {
        Organization org = companyRepository.findByAdminId(user.getId()).stream().findFirst()
                .map(c -> (Organization) c)
                .orElseGet(() -> officeRepository.findByAdminId(user.getId()).stream().findFirst()
                        .map(o -> (Organization) o)
                        .orElseGet(() -> virtualOfficeRepository.findByAdminId(user.getId()).stream().findFirst()
                                .map(vo -> (Organization) vo)
                                .orElseGet(() -> storeRepository.findByAdminId(user.getId()).stream().findFirst()
                                        .map(s -> (Organization) s)
                                        .orElseGet(() -> clientOrgRepository.findByAdminId(user.getId()).stream()
                                                .findFirst()
                                                .map(c -> (Organization) c)
                                                .orElse(null)))));

        if (org == null) {
            org = membershipRepository.findByUserAndStatus(user, MembershipStatus.ACCEPTED)
                    .stream().findFirst()
                    .map(Membership::getOrganization)
                    .orElse(null);
        }

        return org;
    }

    /**
     * التحقق من عضوية المستخدم في منظمة معينة.
     * يدعم VirtualOffice بشكل تلقائي (يتحقق من المنظمة الأم).
     * النتيجة تُخزن مؤقتاً في Caffeine Cache.
     */
    @Cacheable(value = "membershipChecks", key = "#user.id + '_' + #org.id")
    public boolean isUserMemberOfOrganization(User user, Organization org) {
        if (org == null) return false;

        Organization unproxiedOrg = (Organization) Hibernate.unproxy(org);
        if (unproxiedOrg instanceof VirtualOffice) {
            VirtualOffice vo = (VirtualOffice) unproxiedOrg;
            if (vo.getParentOrganization() != null) {
                if (isUserMemberOfOrganization(user, vo.getParentOrganization())) {
                    return true;
                }
            }
        }

        if (companyRepository.existsByAdminIdAndId(user.getId(), org.getId()) ||
                officeRepository.existsByAdminIdAndId(user.getId(), org.getId()) ||
                storeRepository.existsByAdminIdAndId(user.getId(), org.getId()) ||
                clientOrgRepository.existsByAdminIdAndId(user.getId(), org.getId())) {
            return true;
        }
        return membershipRepository.existsByUserAndOrganizationAndStatus(
                user, org, MembershipStatus.ACCEPTED);
    }

    /**
     * مسح الكاش — يُستدعى عند تغيير العضويات أو المنظمات.
     */
    @CacheEvict(value = {"userOrganizations", "membershipChecks"}, allEntries = true)
    public void evictCache() {
        // يتم مسح الكاش تلقائياً بواسطة الأنوتيشن
    }

    /**
     * مسح كاش مستخدم معين.
     */
    @CacheEvict(value = "userOrganizations", key = "#userId")
    public void evictUserCache(Long userId) {
        // يتم مسح الكاش تلقائياً
    }
}
