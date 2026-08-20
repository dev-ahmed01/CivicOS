package com.civicos.admin.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.civicos.admin.application.AdminConfigurationService;
import com.civicos.admin.application.AdminGovernanceService;
import com.civicos.agency.domain.Agency;
import com.civicos.common.web.ApiIdempotencyService;
import com.civicos.common.web.CorrelationIdFilter;
import com.civicos.common.web.PageRequestFactory;
import com.civicos.common.web.PagedResponse;
import com.civicos.user.api.UserResponse;
import com.civicos.user.domain.User;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration")
public class AdminGovernanceController {

	private final AdminGovernanceService service;
	private final AdminConfigurationService configurationService;
	private final ApiIdempotencyService idempotencyService;
	private final PageRequestFactory pageRequestFactory;

	public AdminGovernanceController(
			AdminGovernanceService service,
			AdminConfigurationService configurationService,
			ApiIdempotencyService idempotencyService,
			PageRequestFactory pageRequestFactory) {
		this.service = service; this.configurationService = configurationService;
		this.idempotencyService = idempotencyService; this.pageRequestFactory = pageRequestFactory;
	}

	@GetMapping("/agencies")
	public PagedResponse<AgencyAdminResponse> agencies(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size,
			@RequestParam(defaultValue = "name,asc") String sort,
			HttpServletRequest request) {
		return PagedResponse.from(service.agencies(pageRequestFactory.create(
				page, size, sort, Set.of("name", "code", "type", "active", "createdAt"))),
				CorrelationIdFilter.requestId(request));
	}

	@GetMapping("/roles")
	public List<RoleAdminResponse> roles() { return service.roles(); }

	@GetMapping("/permissions")
	public List<PermissionAdminResponse> permissions() { return service.permissions(); }

	@GetMapping("/configuration")
	public Map<String, Object> configuration() { return configurationService.safeSnapshot(); }

	@PostMapping("/users")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse invite(
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody UserAdminRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "ADMIN_USER_INVITE", body, UserResponse.class,
				() -> service.inviteUser(body.command(User.Status.INVITED), CorrelationIdFilter.requestId(request)));
	}

	@PatchMapping("/users/{userId}")
	public UserResponse updateUser(
			@PathVariable UUID userId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody UserAdminRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "ADMIN_USER_UPDATE:" + userId, body,
				UserResponse.class, () -> service.updateUser(userId, body.command(body.status()),
						CorrelationIdFilter.requestId(request)));
	}

	@PostMapping("/agencies")
	@ResponseStatus(HttpStatus.CREATED)
	public AgencyAdminResponse createAgency(
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody AgencyAdminRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "ADMIN_AGENCY_CREATE", body,
				AgencyAdminResponse.class, () -> service.createAgency(body.command(),
						CorrelationIdFilter.requestId(request)));
	}

	@PatchMapping("/agencies/{agencyId}")
	public AgencyAdminResponse updateAgency(
			@PathVariable UUID agencyId,
			@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody AgencyAdminRequest body,
			HttpServletRequest request) {
		return idempotencyService.execute(idempotencyKey, "ADMIN_AGENCY_UPDATE:" + agencyId, body,
				AgencyAdminResponse.class, () -> service.updateAgency(agencyId, body.command(),
						CorrelationIdFilter.requestId(request)));
	}

	public record UserAdminRequest(
			@NotBlank String fullName, @NotBlank @Email String email, String phone,
			String externalReference, UUID agencyId, User.Status status,
			@NotEmpty Set<String> roleCodes, @NotBlank String reason) {
		AdminGovernanceService.UserCommand command(User.Status effectiveStatus) {
			return new AdminGovernanceService.UserCommand(fullName, email, phone, externalReference,
					agencyId, effectiveStatus, roleCodes, reason);
		}
	}

	public record AgencyAdminRequest(
			@NotBlank String code, @NotBlank String name, @NotNull Agency.Type type,
			String jurisdiction, @Email String contactEmail, String contactPhone,
			boolean active, @NotBlank String reason) {
		AdminGovernanceService.AgencyCommand command() {
			return new AdminGovernanceService.AgencyCommand(code, name, type, jurisdiction,
					contactEmail, contactPhone, active, reason);
		}
	}
}
