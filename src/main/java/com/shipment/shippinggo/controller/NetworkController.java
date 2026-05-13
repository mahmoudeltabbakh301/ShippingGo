package com.shipment.shippinggo.controller;

import com.shipment.shippinggo.entity.Company;
import com.shipment.shippinggo.entity.Membership;
import com.shipment.shippinggo.entity.Office;
import com.shipment.shippinggo.entity.Store;
import com.shipment.shippinggo.entity.Organization;
import com.shipment.shippinggo.entity.OrganizationRelation;
import com.shipment.shippinggo.entity.User;
import com.shipment.shippinggo.enums.OrganizationType;
import com.shipment.shippinggo.enums.RelationStatus;
import com.shipment.shippinggo.enums.RelationType;
import com.shipment.shippinggo.repository.OrganizationRelationRepository;
import com.shipment.shippinggo.service.OrganizationService;
import org.hibernate.Hibernate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/network")
public class NetworkController {

    private final OrganizationService organizationService;
    private final OrganizationRelationRepository organizationRelationRepository;

    public NetworkController(OrganizationService organizationService,
            OrganizationRelationRepository organizationRelationRepository) {
        this.organizationService = organizationService;
        this.organizationRelationRepository = organizationRelationRepository;
    }

    @GetMapping
    public String showNetwork(@AuthenticationPrincipal User user, Model model) {
        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return "redirect:/members/invitations";
        }

        model.addAttribute("organization", org);

        List<Membership> memberRequests = organizationService.getPendingMemberships(org.getId());
        model.addAttribute("memberRequests", memberRequests);

        if (org.getType() == OrganizationType.OFFICE) {
            List<Company> joinedCompanies = organizationService.getCompaniesByOffice(org.getId());
            model.addAttribute("joinedCompanies", joinedCompanies);

            List<OrganizationRelation> pendingCompanyRequests = organizationRelationRepository
                    .findByChildOrganizationAndStatus(org, RelationStatus.PENDING);
            model.addAttribute("pendingLinkRequests", pendingCompanyRequests);

            List<Office> linkedOffices = organizationService.getLinkedOffices(org.getId());
            model.addAttribute("linkedOffices", linkedOffices);

            List<OrganizationRelation> incomingOfficeRequests = organizationService
                    .getIncomingOfficeRequests(org.getId());
            model.addAttribute("incomingOfficeRequests", incomingOfficeRequests);

            List<OrganizationRelation> outgoingOfficeRequests = organizationService
                    .getOutgoingOfficeRequests(org.getId());
            model.addAttribute("outgoingOfficeRequests", outgoingOfficeRequests);

            List<OrganizationRelation> officeRelations = organizationRelationRepository.findPeerRelations(
                    org, RelationStatus.ACCEPTED, RelationType.OFFICE_TO_OFFICE);
            model.addAttribute("officeRelations", officeRelations);

            List<OrganizationRelation> companyRelations = organizationRelationRepository
                    .findByChildOrganizationAndStatusAndRelationType(org, RelationStatus.ACCEPTED,
                            RelationType.OFFICE_TO_COMPANY);
            model.addAttribute("companyRelations", companyRelations);

            List<OrganizationRelation> incomingStoreRequests = organizationRelationRepository
                    .findByParentOrganizationAndStatusAndRelationType(org, RelationStatus.PENDING, RelationType.STORE_TO_OFFICE)
                    .stream()
                    .filter(r -> r.getInitiatedBy() == null || !r.getInitiatedBy().getId().equals(org.getId()))
                    .collect(Collectors.toList());
            model.addAttribute("incomingStoreRequests", incomingStoreRequests);

            List<OrganizationRelation> outgoingStoreRequests = organizationRelationRepository
                    .findByParentOrganizationAndStatusAndRelationType(org, RelationStatus.PENDING, RelationType.STORE_TO_OFFICE)
                    .stream()
                    .filter(r -> r.getInitiatedBy() != null && r.getInitiatedBy().getId().equals(org.getId()))
                    .collect(Collectors.toList());
            model.addAttribute("outgoingStoreRequests", outgoingStoreRequests);

            List<OrganizationRelation> storeRelations = organizationRelationRepository
                    .findByParentOrganizationAndStatusAndRelationType(org, RelationStatus.ACCEPTED,
                            RelationType.STORE_TO_OFFICE);
            model.addAttribute("storeRelations", storeRelations);

        } else if (org.getType() == OrganizationType.STORE) {
            // المتجر يشترك مع شركات الشحن
            List<Company> joinedCompanies = organizationService.getCompaniesByStore(org.getId());
            model.addAttribute("joinedCompanies", joinedCompanies);

            List<OrganizationRelation> pendingCompanyRequests = organizationRelationRepository
                    .findByChildOrganizationAndStatus(org, RelationStatus.PENDING).stream()
                    .filter(r -> r.getInitiatedBy() != null ? r.getInitiatedBy().getId().equals(org.getId()) : true)
                    .collect(Collectors.toList());
            model.addAttribute("pendingLinkRequests", pendingCompanyRequests);

            List<OrganizationRelation> incomingRequests = organizationRelationRepository
                    .findByChildOrganizationAndStatus(org, RelationStatus.PENDING).stream()
                    .filter(r -> r.getInitiatedBy() != null && !r.getInitiatedBy().getId().equals(org.getId()))
                    .collect(Collectors.toList());
            model.addAttribute("incomingStoreRequests", incomingRequests);

            List<OrganizationRelation> companyRelations = organizationRelationRepository
                    .findByChildOrganizationAndStatusAndRelationType(org, RelationStatus.ACCEPTED,
                            RelationType.STORE_TO_COMPANY);
            model.addAttribute("companyRelations", companyRelations);

            List<Office> joinedOffices = organizationService.getOfficesByStore(org.getId());
            model.addAttribute("joinedOffices", joinedOffices);

            List<OrganizationRelation> officeRelations = organizationRelationRepository
                    .findByChildOrganizationAndStatusAndRelationType(org, RelationStatus.ACCEPTED,
                            RelationType.STORE_TO_OFFICE);
            model.addAttribute("officeRelations", officeRelations);

            model.addAttribute("linkRequests", java.util.Collections.emptyList());
        } else {
            List<OrganizationRelation> totalRelations = organizationRelationRepository.findByParentOrganizationAndStatus(org,
                    RelationStatus.ACCEPTED);
            
            // Offices
            List<OrganizationRelation> officeRelations = totalRelations.stream()
                    .filter(r -> r.getChildOrganization().getType() == OrganizationType.OFFICE)
                    .collect(Collectors.toList());
            List<Office> joinedOffices = officeRelations.stream()
                    .map(r -> (Office) Hibernate.unproxy(r.getChildOrganization()))
                    .collect(Collectors.toList());
            model.addAttribute("joinedOffices", joinedOffices);
            model.addAttribute("officeRelations", officeRelations);

            // Stores
            List<OrganizationRelation> storeRelations = totalRelations.stream()
                    .filter(r -> r.getChildOrganization().getType() == OrganizationType.STORE)
                    .collect(Collectors.toList());
            List<Store> joinedStores = storeRelations.stream()
                    .map(r -> (Store) Hibernate.unproxy(r.getChildOrganization()))
                    .collect(Collectors.toList());
            model.addAttribute("joinedStores", joinedStores);
            model.addAttribute("storeRelations", storeRelations);

            List<OrganizationRelation> linkRequests = organizationRelationRepository
                    .findByParentOrganizationAndStatus(org, RelationStatus.PENDING).stream()
                    .filter(r -> r.getInitiatedBy() == null || !r.getInitiatedBy().getId().equals(org.getId()))
                    .collect(Collectors.toList());
            model.addAttribute("linkRequests", linkRequests);

            List<OrganizationRelation> outgoingLinkRequests = organizationRelationRepository
                    .findByParentOrganizationAndStatus(org, RelationStatus.PENDING).stream()
                    .filter(r -> r.getInitiatedBy() != null && r.getInitiatedBy().getId().equals(org.getId()))
                    .collect(Collectors.toList());
            model.addAttribute("outgoingStoreRequests", outgoingLinkRequests);
        }

        return "network/list";
    }

    @GetMapping("/companies")
    public String browseCompanies(@AuthenticationPrincipal User user,
            @RequestParam(required = false) String query,
            Model model) {
        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return "redirect:/members/invitations";
        }
        model.addAttribute("organization", org);

        List<Company> allAvailable = java.util.Collections.emptyList();

        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim().toLowerCase();
            
            Set<Long> excludedCompanyIds = new java.util.HashSet<>();
            if (org.getType() == OrganizationType.OFFICE || org.getType() == OrganizationType.STORE) {
                List<Company> joinedCompanies = (org.getType() == OrganizationType.STORE)
                        ? organizationService.getCompaniesByStore(org.getId())
                        : organizationService.getCompaniesByOffice(org.getId());
                List<OrganizationRelation> pendingCompanyRequests = organizationRelationRepository
                        .findByChildOrganizationAndStatus(org, RelationStatus.PENDING);
                
                excludedCompanyIds.addAll(joinedCompanies.stream().map(Company::getId).collect(Collectors.toSet()));
                excludedCompanyIds.addAll(pendingCompanyRequests.stream()
                        .map(r -> r.getParentOrganization().getId()).toList());
            } else {
                excludedCompanyIds.add(org.getId());
            }

            allAvailable = organizationService.getAllCompanies().stream()
                    .filter(c -> !excludedCompanyIds.contains(c.getId()))
                    .filter(c -> c.getName().toLowerCase().contains(q) || 
                                 String.valueOf(c.getId()).equals(q) || 
                                 (c.getPhone() != null && c.getPhone().contains(q)))
                    .toList();
                    
            model.addAttribute("query", query);
        }

        model.addAttribute("allCompanies", allAvailable);
        return "network/companies";
    }

    @GetMapping("/offices")
    public String browseOffices(@AuthenticationPrincipal User user,
            @RequestParam(required = false) String query,
            Model model) {
        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return "redirect:/members/invitations";
        }
        model.addAttribute("organization", org);

        List<Office> allAvailable = java.util.Collections.emptyList();

        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim().toLowerCase();
            
            Set<Long> excludedOfficeIds = new java.util.HashSet<>();
            if (org.getType() == OrganizationType.OFFICE) {
                List<Office> linkedOffices = organizationService.getLinkedOffices(org.getId());
                List<OrganizationRelation> incomingOfficeRequests = organizationService
                        .getIncomingOfficeRequests(org.getId());
                List<OrganizationRelation> outgoingOfficeRequests = organizationService
                        .getOutgoingOfficeRequests(org.getId());

                excludedOfficeIds.addAll(linkedOffices.stream().map(Office::getId).collect(Collectors.toSet()));
                excludedOfficeIds.add(org.getId());
                excludedOfficeIds.addAll(incomingOfficeRequests.stream()
                        .map(r -> r.getParentOrganization().getId()).toList());
                excludedOfficeIds.addAll(outgoingOfficeRequests.stream()
                        .map(r -> r.getChildOrganization().getId()).toList());
            } else if (org.getType() == OrganizationType.STORE) {
                List<Office> joinedOffices = organizationService.getOfficesByStore(org.getId());
                List<OrganizationRelation> pendingOfficeRequests = organizationRelationRepository
                        .findByChildOrganizationAndStatus(org, RelationStatus.PENDING);

                excludedOfficeIds.addAll(joinedOffices.stream().map(Office::getId).collect(Collectors.toSet()));
                excludedOfficeIds.addAll(pendingOfficeRequests.stream()
                        .map(r -> r.getParentOrganization().getId()).toList());
            } else {
                List<OrganizationRelation> relations = organizationRelationRepository
                        .findByParentOrganizationAndStatus(org, RelationStatus.ACCEPTED);
                excludedOfficeIds.addAll(relations.stream()
                        .map(r -> r.getChildOrganization().getId()).collect(Collectors.toSet()));
                List<OrganizationRelation> linkRequests = organizationRelationRepository
                        .findByParentOrganizationAndStatus(org, RelationStatus.PENDING);
                excludedOfficeIds.addAll(linkRequests.stream()
                        .map(r -> r.getChildOrganization().getId()).toList());
            }

            allAvailable = organizationService.getAllOffices().stream()
                    .filter(o -> !excludedOfficeIds.contains(o.getId()))
                    .filter(o -> o.getName().toLowerCase().contains(q) || 
                                 String.valueOf(o.getId()).equals(q) || 
                                 (o.getPhone() != null && o.getPhone().contains(q)))
                    .toList();
                    
            model.addAttribute("query", query);
        }

        model.addAttribute("allOffices", allAvailable);
        return "network/offices";
    }

    @GetMapping("/stores")
    public String browseStores(@AuthenticationPrincipal User user,
            @RequestParam(required = false) String query,
            Model model) {
        Organization org = organizationService.getOrganizationByUser(user);
        if (org == null) {
            return "redirect:/members/invitations";
        }
        model.addAttribute("organization", org);

        List<com.shipment.shippinggo.entity.Store> allAvailable = java.util.Collections.emptyList();

        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim().toLowerCase();
            
            Set<Long> excludedStoreIds = new java.util.HashSet<>();
            if (org.getType() == OrganizationType.COMPANY || org.getType() == OrganizationType.OFFICE) {
                List<com.shipment.shippinggo.entity.Store> linkedStores = (org.getType() == OrganizationType.COMPANY)
                        ? organizationService.getStoresByCompany(org.getId())
                        : organizationService.getStoresByOffice(org.getId());
                
                List<OrganizationRelation> pendingStoreRequests = organizationRelationRepository
                        .findByParentOrganizationAndStatus(org, RelationStatus.PENDING).stream()
                        .filter(r -> r.getChildOrganization().getType() == OrganizationType.STORE)
                        .toList();

                excludedStoreIds.addAll(linkedStores.stream().map(com.shipment.shippinggo.entity.Store::getId).collect(Collectors.toSet()));
                excludedStoreIds.addAll(pendingStoreRequests.stream()
                        .map(r -> r.getChildOrganization().getId()).toList());
            } else if (org.getType() == OrganizationType.STORE) {
                excludedStoreIds.add(org.getId());
            }

            allAvailable = organizationService.getAllStores().stream()
                    .filter(s -> !excludedStoreIds.contains(s.getId()))
                    .filter(s -> s.getName().toLowerCase().contains(q) || 
                                 String.valueOf(s.getId()).equals(q) || 
                                 (s.getPhone() != null && s.getPhone().contains(q)))
                    .toList();
                    
            model.addAttribute("query", query);
        }

        model.addAttribute("allStores", allAvailable);
        return "network/stores";
    }

    @PostMapping("/join")
    public String joinCompany(@AuthenticationPrincipal User user,
            @RequestParam Long companyId,
            RedirectAttributes redirectAttributes) {
        Organization office = organizationService.getOrganizationByAdmin(user);
        if (office == null) {
            return "redirect:/members/invitations";
        }

        try {
            organizationService.requestLinkToCompany(office, companyId);
            redirectAttributes.addFlashAttribute("success", "تم إرسال طلب الانضمام بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/network";
    }

    @PostMapping("/join-office")
    public String joinOffice(@AuthenticationPrincipal User user,
            @RequestParam Long officeId,
            RedirectAttributes redirectAttributes) {
        Organization office = organizationService.getOrganizationByAdmin(user);
        if (office == null) {
            return "redirect:/members/invitations";
        }

        try {
            organizationService.requestLinkToOffice(office, officeId);
            redirectAttributes.addFlashAttribute("success", "تم إرسال طلب الانضمام للمكتب بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/network";
    }

    @PostMapping("/join-store")
    public String joinStore(@AuthenticationPrincipal User user,
            @RequestParam Long storeId,
            RedirectAttributes redirectAttributes) {
        Organization requestingOrg = organizationService.getOrganizationByAdmin(user);
        if (requestingOrg == null) {
            return "redirect:/members/invitations";
        }

        try {
            organizationService.requestLinkToStore(requestingOrg, storeId);
            redirectAttributes.addFlashAttribute("success", "تم إرسال طلب الانضمام للمتجر بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/network";
    }

    @PostMapping("/cancel-request")
    public String cancelRequest(@AuthenticationPrincipal User user,
            @RequestParam Long relationId,
            RedirectAttributes redirectAttributes) {
        try {
            organizationService.cancelRequest(relationId, user);
            redirectAttributes.addFlashAttribute("success", "تم إلغاء الطلب بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/network";
    }

    @PostMapping("/remove")
    public String removeRelation(@AuthenticationPrincipal User user,
            @RequestParam Long relationId,
            RedirectAttributes redirectAttributes) {
        try {
            organizationService.removeRelation(relationId, user);
            redirectAttributes.addFlashAttribute("success", "تم إزالة الارتباط بنجاح");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/network";
    }

    @PostMapping("/requests/member/process")
    public String processMemberRequest(@RequestParam Long membershipId,
            @RequestParam boolean accept,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        try {
            if (!accept) {
                organizationService.cancelInvitation(membershipId);
            }
            redirectAttributes.addFlashAttribute("success", accept ? "تم قبول الطلب" : "تم إلغاء الدعوة");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/network";
    }

    @PostMapping("/requests/link/process")
    public String processLinkRequest(@RequestParam Long relationId,
            @RequestParam boolean accept,
            RedirectAttributes redirectAttributes) {
        try {
            organizationService.processLinkRequest(relationId, accept);
            redirectAttributes.addFlashAttribute("success", accept ? "تم قبول الطلب" : "تم رفض الطلب");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/network";
    }
}
