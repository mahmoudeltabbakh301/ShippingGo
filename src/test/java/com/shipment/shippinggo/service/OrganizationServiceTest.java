package com.shipment.shippinggo.service;

import com.shipment.shippinggo.entity.*;
import com.shipment.shippinggo.enums.MembershipStatus;
import com.shipment.shippinggo.enums.OrganizationType;
import com.shipment.shippinggo.enums.Role;
import com.shipment.shippinggo.exception.DuplicateResourceException;
import com.shipment.shippinggo.exception.ResourceNotFoundException;
import com.shipment.shippinggo.exception.UnauthorizedAccessException;
import com.shipment.shippinggo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private CompanyRepository companyRepository;
    @Mock private OfficeRepository officeRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private OrganizationRelationRepository organizationRelationRepository;
    @Mock private UserRepository userRepository;
    @Mock private VirtualOfficeRepository virtualOfficeRepository;

    private OrganizationService organizationService;
    private User adminUser;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationService(organizationRepository, companyRepository,
                officeRepository, storeRepository, membershipRepository, userRepository,
                organizationRelationRepository, virtualOfficeRepository);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setRole(Role.ADMIN);
        adminUser.setFullName("Admin");
    }

    private Organization createTestOrg() {
        Company org = new Company();
        org.setId(1L);
        org.setName("Test Org");
        return org;
    }

    // === findById ===

    @Test
    void findById_Found() {
        Organization org = createTestOrg();
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(org));

        Organization result = organizationService.findById(1L);
        assertNotNull(result);
    }

    @Test
    void findById_NotFound_ReturnsNull() {
        when(organizationRepository.findById(99L)).thenReturn(Optional.empty());
        assertNull(organizationService.findById(99L));
    }

    // === getAllCompanies ===

    @Test
    void getAllCompanies_ReturnsList() {
        when(companyRepository.findAll()).thenReturn(List.of(new Company()));
        assertEquals(1, organizationService.getAllCompanies().size());
    }

    // === searchOrganizations ===

    @Test
    void searchOrganizations_NullQuery_ReturnsEmpty() {
        assertTrue(organizationService.searchOrganizations(null).isEmpty());
    }

    @Test
    void searchOrganizations_EmptyQuery_ReturnsEmpty() {
        assertTrue(organizationService.searchOrganizations("  ").isEmpty());
    }

    @Test
    void searchOrganizations_ValidQuery_ReturnsList() {
        Company c = new Company();
        when(organizationRepository.searchByNameOrPhone("test")).thenReturn(List.of(c));
        assertEquals(1, organizationService.searchOrganizations("test").size());
    }

    // === inviteMember ===

    @Test
    void inviteMember_UserNotFound_Throws() {
        Organization org = createTestOrg();
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> organizationService.inviteMember(org, "unknown@test.com", Role.DATA_ENTRY, adminUser));
    }

    @Test
    void inviteMember_DuplicateMembership_Throws() {
        Organization org = createTestOrg();
        User target = new User();
        target.setId(5L);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(target));
        when(membershipRepository.existsByUserAndOrganization(target, org)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> organizationService.inviteMember(org, "user@test.com", Role.DATA_ENTRY, adminUser));
    }

    @Test
    void inviteMember_Success_ReturnsMembership() {
        Organization org = createTestOrg();
        User target = new User();
        target.setId(5L);
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.of(target));
        when(membershipRepository.existsByUserAndOrganization(target, org)).thenReturn(false);
        when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));

        Membership result = organizationService.inviteMember(org, "new@test.com", Role.DATA_ENTRY, adminUser);
        assertNotNull(result);
        assertEquals(MembershipStatus.PENDING, result.getStatus());
    }

    // === acceptInvitation ===

    @Test
    void acceptInvitation_WrongUser_Throws() {
        User otherUser = new User();
        otherUser.setId(99L);

        Membership membership = Membership.builder()
                .user(adminUser)
                .status(MembershipStatus.PENDING)
                .build();

        when(membershipRepository.findById(1L)).thenReturn(Optional.of(membership));

        assertThrows(UnauthorizedAccessException.class,
                () -> organizationService.acceptInvitation(1L, otherUser));
    }

    // === getOrganizationByAdmin ===

    @Test
    void getOrganizationByAdmin_NullUser_ReturnsNull() {
        assertNull(organizationService.getOrganizationByAdmin(null));
    }

    // === getOrganizationByUser ===

    @Test
    void getOrganizationByUser_AdminOrg_ReturnsOrg() {
        Company company = new Company();
        company.setId(1L);
        when(companyRepository.findByAdminId(adminUser.getId())).thenReturn(List.of(company));

        Organization result = organizationService.getOrganizationByUser(adminUser);
        assertNotNull(result);
    }

    // === getCouriers ===

    @Test
    void getCouriers_ReturnsCourierUsers() {
        Office org = new Office();
        org.setId(1L);
        org.setType(OrganizationType.OFFICE);

        User courier = new User();
        courier.setId(10L);
        Membership m = Membership.builder().user(courier).build();

        when(membershipRepository.findByOrganizationAndStatusAndAssignedRole(
                org, MembershipStatus.ACCEPTED, Role.COURIER)).thenReturn(List.of(m));

        List<User> couriers = organizationService.getCouriers(org);
        assertEquals(1, couriers.size());
    }

    // === findAvailableOrganizationsForMember ===

    @Test
    void findAvailableOrganizationsForMember_NullMember_ReturnsEmpty() {
        assertTrue(organizationService.findAvailableOrganizationsForMember(null).isEmpty());
    }

    @Test
    void findAvailableOrganizationsForMember_NullGovernorate_ReturnsEmpty() {
        User user = new User();
        user.setGovernorate(null);
        assertTrue(organizationService.findAvailableOrganizationsForMember(user).isEmpty());
    }

    // === changeMemberRole ===

    @Test
    void changeMemberRole_UpdatesRoleInMembershipAndUser() {
        User user = new User();
        user.setId(5L);
        user.setRole(Role.DATA_ENTRY);

        Membership membership = Membership.builder().user(user).assignedRole(Role.DATA_ENTRY).build();
        when(membershipRepository.findById(1L)).thenReturn(Optional.of(membership));

        organizationService.changeMemberRole(1L, Role.COURIER);

        assertEquals(Role.COURIER, membership.getAssignedRole());
        verify(userRepository).save(user);
        verify(membershipRepository).save(membership);
    }

    // === removeMember ===

    @Test
    void removeMember_ResetsRoleAndDeletes() {
        User user = new User();
        user.setId(5L);
        user.setRole(Role.DATA_ENTRY);

        Membership membership = Membership.builder().user(user).build();
        when(membershipRepository.findById(1L)).thenReturn(Optional.of(membership));

        organizationService.removeMember(1L);

        assertEquals(Role.MEMBER, user.getRole());
        verify(userRepository).save(user);
        verify(membershipRepository).delete(membership);
    }
}
