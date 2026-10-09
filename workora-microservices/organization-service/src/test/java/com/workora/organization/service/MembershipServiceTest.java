package com.workora.organization.service;

import com.workora.organization.dto.ChangeRoleRequest;
import com.workora.organization.entity.CompanyRole;
import com.workora.organization.entity.Department;
import com.workora.organization.entity.DepartmentMembership;
import com.workora.organization.entity.DepartmentRole;
import com.workora.organization.entity.Membership;
import com.workora.organization.entity.MembershipStatus;
import com.workora.organization.exception.ForbiddenException;
import com.workora.organization.mapper.OrganizationMapper;
import com.workora.organization.repository.DepartmentRepository;
import com.workora.organization.repository.DepartmentMembershipRepository;
import com.workora.organization.repository.MembershipRepository;
import com.workora.organization.repository.CompanyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock
    private MembershipRepository membershipRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private DepartmentMembershipRepository departmentMembershipRepository;
    @Mock
    private OrganizationMapper mapper;
    @Mock
    private CompanyRepository companyRepository;

    private MembershipService service;

    private final UUID companyId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MembershipService(membershipRepository, departmentRepository, mapper);
    }

    private Membership membership(UUID userId, CompanyRole role) {
        return new Membership(companyId, userId, userId + "@workora.test", role, null, userId);
    }

    @Test
    void requireOwnerOrAdmin_rejectsNonAdminRole() {
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, actorId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership(actorId, CompanyRole.DEVELOPER)));

        assertThatThrownBy(() -> service.requireOwnerOrAdmin(companyId, actorId))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getCode())
                .isEqualTo("COMPANY_ADMIN_REQUIRED");
    }

    @Test
    void requireOwnerOrAdmin_allowsOwner() {
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, actorId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership(actorId, CompanyRole.OWNER)));

        Membership result = service.requireOwnerOrAdmin(companyId, actorId);

        assertThat(result.getRole()).isEqualTo(CompanyRole.OWNER);
    }

    @Test
    void requireActiveMembership_rejectsNonMember() {
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, actorId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireActiveMembership(companyId, actorId))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getCode())
                .isEqualTo("NOT_A_COMPANY_MEMBER");
    }

    @Test
    void requireActiveMembership_blocksSuspendedCompanyBeforeCheckingMembership() {
        service = new MembershipService(
                membershipRepository,
                departmentRepository,
                null,
                null,
                mapper,
                companyRepository);
        when(companyRepository.findByIdAndStatus(companyId, com.workora.organization.entity.CompanyStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireActiveMembership(companyId, actorId))
                .isInstanceOf(ForbiddenException.class)
                .extracting(error -> ((ForbiddenException) error).getCode())
                .isEqualTo("COMPANY_ACCESS_SUSPENDED");
        verifyNoInteractions(membershipRepository);
    }

    @Test
    void listMembers_excludesOwnersFromUnfilteredResults() {
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, actorId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership(actorId, CompanyRole.ADMIN)));
        Membership member = membership(targetId, CompanyRole.DEVELOPER);
        when(membershipRepository.findByCompanyIdAndStatusAndRoleNotOrderByCreatedAtAsc(
                companyId, MembershipStatus.ACTIVE, CompanyRole.OWNER))
                .thenReturn(List.of(member));
        when(mapper.toResponse(member)).thenReturn(null);

        service.listMembers(companyId, actorId, null);

        verify(membershipRepository).findByCompanyIdAndStatusAndRoleNotOrderByCreatedAtAsc(
                companyId, MembershipStatus.ACTIVE, CompanyRole.OWNER);
        verify(membershipRepository, never()).findByCompanyIdAndStatusOrderByCreatedAtAsc(
                companyId, MembershipStatus.ACTIVE);
    }

    @Test
    void assignToDepartment_supportsMultipleDepartmentRolesAndRemovalKeepsRemainingAssignment() {
        UUID firstDepartmentId = UUID.randomUUID();
        UUID secondDepartmentId = UUID.randomUUID();
        Membership actor = membership(actorId, CompanyRole.OWNER);
        Membership target = membership(targetId, CompanyRole.DEVELOPER);
        ReflectionTestUtils.setField(target, "id", UUID.randomUUID());
        Department firstDepartment = new Department(companyId, "Engineering");
        Department secondDepartment = new Department(companyId, "Quality");

        service = new MembershipService(
                membershipRepository,
                departmentRepository,
                departmentMembershipRepository,
                null,
                null,
                mapper,
                null);
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, actorId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(actor));
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, targetId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(target));
        when(departmentRepository.findById(firstDepartmentId)).thenReturn(Optional.of(firstDepartment));
        when(departmentRepository.findById(secondDepartmentId)).thenReturn(Optional.of(secondDepartment));
        when(departmentMembershipRepository.findByMembership_IdAndDepartmentId(
                target.getId(), firstDepartmentId)).thenReturn(Optional.empty());
        when(departmentMembershipRepository.findByMembership_IdAndDepartmentId(
                target.getId(), secondDepartmentId)).thenReturn(Optional.empty());

        service.assignToDepartment(firstDepartmentId, targetId, actorId, DepartmentRole.DEVELOPER);
        service.assignToDepartment(secondDepartmentId, targetId, actorId, DepartmentRole.TESTER);

        assertThat(target.getDepartmentMemberships())
                .extracting(DepartmentMembership::getRole)
                .containsExactlyInAnyOrder(DepartmentRole.DEVELOPER, DepartmentRole.TESTER);
        assertThat(target.getDepartmentId()).isEqualTo(firstDepartmentId);

        when(departmentMembershipRepository.findByMembership_IdAndDepartmentId(
                target.getId(), firstDepartmentId))
                .thenReturn(target.getDepartmentMemberships().stream()
                        .filter(assignment -> assignment.getDepartmentId().equals(firstDepartmentId))
                        .findFirst());
        service.removeFromDepartment(firstDepartmentId, targetId, actorId);

        assertThat(target.getDepartmentMemberships())
                .extracting(DepartmentMembership::getDepartmentId)
                .containsExactly(secondDepartmentId);
        assertThat(target.getDepartmentId()).isEqualTo(secondDepartmentId);
        verify(departmentMembershipRepository).delete(any(DepartmentMembership.class));
    }

    @Test
    void listMembers_excludesOwnersFromSearchResults() {
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, actorId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership(actorId, CompanyRole.ADMIN)));
        Membership member = membership(targetId, CompanyRole.DEVELOPER);
        when(membershipRepository.findByCompanyIdAndStatusAndRoleNotAndEmailContainingIgnoreCase(
                companyId, MembershipStatus.ACTIVE, CompanyRole.OWNER, "member"))
                .thenReturn(List.of(member));
        when(mapper.toResponse(member)).thenReturn(null);

        service.listMembers(companyId, actorId, "member");

        verify(membershipRepository).findByCompanyIdAndStatusAndRoleNotAndEmailContainingIgnoreCase(
                companyId, MembershipStatus.ACTIVE, CompanyRole.OWNER, "member");
        verify(membershipRepository, never()).findByCompanyIdAndStatusAndEmailContainingIgnoreCase(
                companyId, MembershipStatus.ACTIVE, "member");
    }

    @Test
    void getMember_requiresActiveCallerMembershipAndFindsTargetInSameCompany() {
        Membership caller = membership(actorId, CompanyRole.DEVELOPER);
        Membership target = membership(targetId, CompanyRole.TESTER);
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, actorId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(caller));
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, targetId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(target));

        service.getMember(companyId, targetId, actorId);

        verify(mapper).toResponse(target);
    }

    @Test
    void getMember_deniesLookupWhenCallerIsNotAnActiveCompanyMember() {
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, actorId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMember(companyId, targetId, actorId))
                .isInstanceOf(ForbiddenException.class)
                .extracting(error -> ((ForbiddenException) error).getCode())
                .isEqualTo("NOT_A_COMPANY_MEMBER");

        verify(membershipRepository, never())
                .findByCompanyIdAndUserIdAndStatus(companyId, targetId, MembershipStatus.ACTIVE);
    }

    @Test
    void removeMember_protectsFinalOwner() {
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, actorId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership(actorId, CompanyRole.ADMIN)));
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, targetId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership(targetId, CompanyRole.OWNER)));
        when(membershipRepository.countByCompanyIdAndRoleAndStatus(companyId, CompanyRole.OWNER, MembershipStatus.ACTIVE))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.removeMember(companyId, targetId, actorId))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getCode())
                .isEqualTo("LAST_OWNER_PROTECTED");

        verify(membershipRepository, never()).save(any());
    }

    @Test
    void removeMember_allowsRemovingMemberWhenOwnerIsNotTarget() {
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, actorId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership(actorId, CompanyRole.OWNER)));
        Membership target = membership(targetId, CompanyRole.DEVELOPER);
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, targetId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(target));
        when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));

        service.removeMember(companyId, targetId, actorId);

        assertThat(target.getStatus()).isEqualTo(MembershipStatus.REMOVED);
        assertThat(target.getRemovedAt()).isNotNull();
        verify(membershipRepository).save(target);
    }

    @Test
    void changeRole_protectsDemotingFinalOwner() {
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, actorId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership(actorId, CompanyRole.ADMIN)));
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, targetId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership(targetId, CompanyRole.OWNER)));
        when(membershipRepository.countByCompanyIdAndRoleAndStatus(companyId, CompanyRole.OWNER, MembershipStatus.ACTIVE))
                .thenReturn(1L);

        ChangeRoleRequest request = new ChangeRoleRequest(CompanyRole.DEVELOPER, null);

        assertThatThrownBy(() -> service.changeRole(companyId, targetId, actorId, request))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getCode())
                .isEqualTo("LAST_OWNER_PROTECTED");
    }

    @Test
    void changeRole_updatesRoleAndDepartment() {
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, actorId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(membership(actorId, CompanyRole.OWNER)));
        Membership target = membership(targetId, CompanyRole.DEVELOPER);
        when(membershipRepository.findByCompanyIdAndUserIdAndStatus(companyId, targetId, MembershipStatus.ACTIVE))
                .thenReturn(Optional.of(target));
        UUID departmentId = UUID.randomUUID();
        when(departmentRepository.findByIdAndCompanyId(departmentId, companyId))
                .thenReturn(Optional.of(new com.workora.organization.entity.Department(companyId, "Engineering")));
        when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(mapper.toResponse(any(Membership.class))).thenReturn(null);

        ChangeRoleRequest request = new ChangeRoleRequest(CompanyRole.PROJECT_MANAGER, departmentId);
        service.changeRole(companyId, targetId, actorId, request);

        assertThat(target.getRole()).isEqualTo(CompanyRole.PROJECT_MANAGER);
        assertThat(target.getDepartmentId()).isEqualTo(departmentId);
    }
}
