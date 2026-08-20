package com.civicos.admin.application;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.civicos.admin.api.AgencyAdminResponse;
import com.civicos.admin.api.PermissionAdminResponse;
import com.civicos.admin.api.RoleAdminResponse;
import com.civicos.agency.domain.Agency;
import com.civicos.agency.repository.AgencyRepository;
import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.application.AuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.Role;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.repository.PermissionRepository;
import com.civicos.auth.repository.RoleRepository;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.common.domain.DomainConflictException;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.user.api.UserResponse;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

@Service
public class AdminGovernanceService {

	private final UserRepository userRepository;
	private final AgencyRepository agencyRepository;
	private final RoleRepository roleRepository;
	private final PermissionRepository permissionRepository;
	private final AuditEventRepository auditEventRepository;
	private final AuthorizationService authorizationService;

	public AdminGovernanceService(
			UserRepository userRepository,
			AgencyRepository agencyRepository,
			RoleRepository roleRepository,
			PermissionRepository permissionRepository,
			AuditEventRepository auditEventRepository,
			AuthorizationService authorizationService) {
		this.userRepository = userRepository;
		this.agencyRepository = agencyRepository;
		this.roleRepository = roleRepository;
		this.permissionRepository = permissionRepository;
		this.auditEventRepository = auditEventRepository;
		this.authorizationService = authorizationService;
	}

	@Transactional(readOnly = true)
	public Page<AgencyAdminResponse> agencies(Pageable pageable) {
		admin(PermissionCode.POLICY_VIEW);
		return agencyRepository.findAll(pageable).map(AgencyAdminResponse::from);
	}

	@Transactional(readOnly = true)
	public List<RoleAdminResponse> roles() {
		admin(PermissionCode.POLICY_VIEW);
		return roleRepository.findAllByOrderByCodeAsc().stream().map(RoleAdminResponse::from).toList();
	}

	@Transactional(readOnly = true)
	public List<PermissionAdminResponse> permissions() {
		admin(PermissionCode.POLICY_VIEW);
		return permissionRepository.findAllByOrderByCodeAsc().stream()
				.map(PermissionAdminResponse::from).toList();
	}

	@Transactional
	public UserResponse inviteUser(UserCommand command, String requestId) {
		CivicPrincipal principal = admin(PermissionCode.USER_CREATE);
		authorizationService.authorize(PermissionCode.ROLE_ASSIGN);
		authorizationService.authorize(PermissionCode.AGENCY_ASSIGN);
		if (userRepository.findByEmailIgnoreCase(command.email()).isPresent()) {
			throw new DomainConflictException("A user with this email already exists.");
		}
		Set<Role> roles = roles(command.roleCodes());
		User user = User.invite(agency(command.agencyId()), command.fullName(), command.email(),
				command.phone(), command.externalReference(), roles);
		userRepository.saveAndFlush(user);
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), "USER_INVITED", "USER", user.getId(), null, userState(user),
				command.reason(), requestId));
		return UserResponse.from(user);
	}

	@Transactional
	public UserResponse updateUser(UUID userId, UserCommand command, String requestId) {
		CivicPrincipal principal = admin(PermissionCode.USER_UPDATE);
		authorizationService.authorize(PermissionCode.ROLE_ASSIGN);
		authorizationService.authorize(PermissionCode.AGENCY_ASSIGN);
		User user = userRepository.findWithAuthoritiesById(userId)
				.orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
		if (user.getId().equals(principal.userId()) && command.status() != User.Status.ACTIVE) {
			throw new DomainConflictException("Administrators cannot disable their own active session account.");
		}
		Map<String, Object> before = userState(user);
		user.applyAdministrativeProfile(agency(command.agencyId()), command.fullName(), command.email(),
				command.phone(), command.externalReference(), command.status(), roles(command.roleCodes()));
		userRepository.saveAndFlush(user);
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), "USER_ADMIN_UPDATED", "USER", user.getId(), before, userState(user),
				command.reason(), requestId));
		return UserResponse.from(user);
	}

	@Transactional
	public AgencyAdminResponse createAgency(AgencyCommand command, String requestId) {
		CivicPrincipal principal = admin(PermissionCode.POLICY_MANAGE);
		if (agencyRepository.findByCodeIgnoreCase(command.code()).isPresent()) {
			throw new DomainConflictException("An agency with this code already exists.");
		}
		Agency agency = Agency.create(command.code(), command.name(), command.type(), command.jurisdiction(),
				command.contactEmail(), command.contactPhone());
		agencyRepository.saveAndFlush(agency);
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), "AGENCY_CREATED", "AGENCY", agency.getId(), null, agencyState(agency),
				command.reason(), requestId));
		return AgencyAdminResponse.from(agency);
	}

	@Transactional
	public AgencyAdminResponse updateAgency(UUID agencyId, AgencyCommand command, String requestId) {
		CivicPrincipal principal = admin(PermissionCode.POLICY_MANAGE);
		Agency agency = agencyRepository.findById(agencyId)
				.orElseThrow(() -> new NoSuchElementException("Agency not found: " + agencyId));
		if (!agency.getCode().equalsIgnoreCase(command.code())) {
			throw new DomainValidationException("Agency code is immutable after creation.");
		}
		Map<String, Object> before = agencyState(agency);
		agency.applyAdministrativeUpdate(command.name(), command.type(), command.jurisdiction(),
				command.contactEmail(), command.contactPhone(), command.active());
		agencyRepository.saveAndFlush(agency);
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), "AGENCY_ADMIN_UPDATED", "AGENCY", agency.getId(), before,
				agencyState(agency), command.reason(), requestId));
		return AgencyAdminResponse.from(agency);
	}

	private CivicPrincipal admin(PermissionCode permission) {
		authorizationService.authorize(permission);
		CivicPrincipal principal = authorizationService.currentPrincipal();
		if (!principal.roles().contains(SystemRole.ADMIN.name())) {
			throw new AccessDeniedException("Only administrators can manage system governance.");
		}
		return principal;
	}

	private Agency agency(UUID agencyId) {
		return agencyId == null ? null : agencyRepository.findById(agencyId)
				.orElseThrow(() -> new NoSuchElementException("Agency not found: " + agencyId));
	}

	private Set<Role> roles(Set<String> codes) {
		if (codes == null || codes.isEmpty()) throw new DomainValidationException("At least one role is required.");
		List<Role> roles = roleRepository.findByCodeIn(codes);
		if (roles.size() != codes.size()) throw new DomainValidationException("One or more role codes are invalid.");
		return Set.copyOf(roles);
	}

	private User actor(CivicPrincipal principal) {
		return userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException("Authenticated administrator not found."));
	}

	private Map<String, Object> userState(User user) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("email", user.getEmail()); state.put("status", user.getStatus().name());
		state.put("agencyId", user.getAgency() == null ? null : user.getAgency().getId().toString());
		state.put("roles", user.getRoles().stream().map(Role::getCode).sorted().toList());
		return state;
	}

	private Map<String, Object> agencyState(Agency agency) {
		return Map.of("code", agency.getCode(), "name", agency.getName(),
				"type", agency.getType().name(), "active", agency.isActive());
	}

	public record UserCommand(
			String fullName, String email, String phone, String externalReference,
			UUID agencyId, User.Status status, Set<String> roleCodes, String reason) {
	}

	public record AgencyCommand(
			String code, String name, Agency.Type type, String jurisdiction,
			String contactEmail, String contactPhone, boolean active, String reason) {
	}
}
