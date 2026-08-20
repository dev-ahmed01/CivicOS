package com.civicos.evidence.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.auth.application.ScopedAuthorizationService;
import com.civicos.auth.domain.PermissionCode;
import com.civicos.auth.domain.SystemRole;
import com.civicos.auth.security.CivicPrincipal;
import com.civicos.casefile.domain.CitizenObservation;
import com.civicos.casefile.repository.CitizenObservationRepository;
import com.civicos.common.domain.DomainValidationException;
import com.civicos.common.domain.StaleEntityVersionException;
import com.civicos.common.validation.ValidationRules;
import com.civicos.evidence.domain.Evidence;
import com.civicos.evidence.repository.EvidenceRepository;
import com.civicos.evidence.storage.FileStorageService;
import com.civicos.evidence.storage.StoredFile;
import com.civicos.intervention.domain.Intervention;
import com.civicos.intervention.repository.InterventionRepository;
import com.civicos.user.domain.User;
import com.civicos.user.repository.UserRepository;

@Service
public class EvidenceService {

	private static final GeometryFactory WGS84 = new GeometryFactory(new PrecisionModel(), 4326);
	private static final Set<Evidence.Type> AGENCY_TYPES = Set.of(
			Evidence.Type.BEFORE_WORK, Evidence.Type.DURING_WORK,
			Evidence.Type.COMPLETION, Evidence.Type.RESTORATION, Evidence.Type.DOCUMENT);

	private final EvidenceRepository evidenceRepository;
	private final InterventionRepository interventionRepository;
	private final CitizenObservationRepository observationRepository;
	private final UserRepository userRepository;
	private final AuditEventRepository auditEventRepository;
	private final ScopedAuthorizationService authorizationService;
	private final EvidenceValidator validator;
	private final EvidenceMetadataService metadataService;
	private final FileStorageService fileStorageService;
	private final Clock clock;

	public EvidenceService(
			EvidenceRepository evidenceRepository,
			InterventionRepository interventionRepository,
			CitizenObservationRepository observationRepository,
			UserRepository userRepository,
			AuditEventRepository auditEventRepository,
			ScopedAuthorizationService authorizationService,
			EvidenceValidator validator,
			EvidenceMetadataService metadataService,
			FileStorageService fileStorageService,
			Clock clock) {
		this.evidenceRepository = evidenceRepository;
		this.interventionRepository = interventionRepository;
		this.observationRepository = observationRepository;
		this.userRepository = userRepository;
		this.auditEventRepository = auditEventRepository;
		this.authorizationService = authorizationService;
		this.validator = validator;
		this.metadataService = metadataService;
		this.fileStorageService = fileStorageService;
		this.clock = clock;
	}

	@Transactional
	public EvidenceResult upload(EvidenceUploadCommand command, byte[] content, String requestId) {
		String targetType = validator.validate(command, content);
		Target target = target(targetType, command.targetId());
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.EVIDENCE_UPLOAD,
				target.agencyId(),
				SystemRole.CITIZEN, SystemRole.AGENCY_OFFICER,
				SystemRole.COORDINATOR, SystemRole.INSPECTOR);
		validateUploader(principal, target, command.type());
		User uploader = actor(principal);

		StoredFile storedFile = fileStorageService.store(content);
		deleteIfTransactionRollsBack(storedFile.fileReference());
		Map<String, Object> metadata = metadataService.trustedMetadata(
				command.metadata(), targetType, command.targetId(), uploader,
				provenance(principal), command.type(), normalizedMimeType(command.mimeType()),
				storedFile.sizeBytes(), storedFile.sha256Checksum(), command.capturedAt());
		Evidence evidence = Evidence.uploaded(
				targetType,
				command.targetId(),
				uploader,
				command.type(),
				storedFile.fileReference(),
				cleanFilename(command.originalFilename()),
				normalizedMimeType(command.mimeType()),
				storedFile.sizeBytes(),
				storedFile.sha256Checksum(),
				command.capturedAt(),
				command.latitude(),
				command.longitude(),
				point(command.latitude(), command.longitude()),
				metadata);
		evidenceRepository.saveAndFlush(evidence);
		Instant occurredAt = clock.instant();
		auditEventRepository.save(AuditEvent.domainMutation(
				uploader, "EVIDENCE_UPLOADED", "EVIDENCE", evidence.getId(),
				null, state(evidence), "Evidence validated and associated with its target.", requestId));
		return result(evidence, occurredAt);
	}

	@Transactional
	public EvidenceResult submitForReview(
			UUID evidenceId, long expectedVersion, String reason, String requestId) {
		Evidence evidence = evidenceForUpdate(evidenceId);
		Target target = target(evidence.getTargetType(), evidence.getTargetId());
		CivicPrincipal principal = authorizationService.authorize(
				PermissionCode.EVIDENCE_UPLOAD, target.agencyId(),
				SystemRole.CITIZEN, SystemRole.AGENCY_OFFICER,
				SystemRole.COORDINATOR, SystemRole.INSPECTOR);
		if (!evidence.getUploadedBy().getId().equals(principal.userId())) {
			throw new AccessDeniedException("Only the uploader can submit evidence for review.");
		}
		assertVersion(evidence, expectedVersion);
		Map<String, Object> before = state(evidence);
		evidence.submitForReview();
		evidenceRepository.saveAndFlush(evidence);
		Instant occurredAt = clock.instant();
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), "EVIDENCE_SUBMITTED_FOR_REVIEW", "EVIDENCE", evidence.getId(),
				before, state(evidence), reason, requestId));
		return result(evidence, occurredAt);
	}

	@Transactional
	public EvidenceResult review(UUID evidenceId, EvidenceReviewCommand command, String requestId) {
		ValidationRules.required(command, "Evidence review command");
		Evidence evidence = evidenceForUpdate(evidenceId);
		Target target = target(evidence.getTargetType(), evidence.getTargetId());
		PermissionCode permission = reviewPermission(command.decision());
		CivicPrincipal principal = authorizationService.authorize(
				permission, target.agencyId(), SystemRole.INSPECTOR);
		if (evidence.getUploadedBy().getId().equals(principal.userId())) {
			throw new com.civicos.workflow.application.SeparationOfDutiesException();
		}
		if (command.decision() == Evidence.Status.REJECTED
				&& (command.reason() == null || command.reason().isBlank())) {
			throw new DomainValidationException("Evidence rejection reason is required.");
		}
		assertVersion(evidence, command.expectedVersion());
		Map<String, Object> before = state(evidence);
		evidence.review(command.decision());
		evidenceRepository.saveAndFlush(evidence);
		Instant occurredAt = clock.instant();
		auditEventRepository.save(AuditEvent.domainMutation(
				actor(principal), "EVIDENCE_REVIEWED", "EVIDENCE", evidence.getId(),
				before, state(evidence), command.reason(), requestId));
		return result(evidence, occurredAt);
	}

	private void validateUploader(CivicPrincipal principal, Target target, Evidence.Type type) {
		Set<String> roles = principal.roles();
		if ("CITIZEN_OBSERVATION".equals(target.type())) {
			if (!roles.contains(SystemRole.CITIZEN.name()) || !principal.userId().equals(target.ownerId())) {
				throw new AccessDeniedException("Citizens may upload evidence only to their own observation.");
			}
			if (type != Evidence.Type.CITIZEN_VALIDATION && type != Evidence.Type.DOCUMENT) {
				throw new DomainValidationException("Citizen observation evidence has an invalid evidence type.");
			}
			return;
		}
		if (roles.contains(SystemRole.INSPECTOR.name())) {
			if (type != Evidence.Type.INSPECTION && type != Evidence.Type.DOCUMENT) {
				throw new DomainValidationException("Inspectors may upload inspection evidence or documents.");
			}
			return;
		}
		if (!AGENCY_TYPES.contains(type)) {
			throw new DomainValidationException("The actor cannot upload this evidence type.");
		}
	}

	private Target target(String targetType, UUID targetId) {
		return switch (targetType) {
			case "INTERVENTION" -> {
				Intervention intervention = interventionRepository.findById(targetId)
						.orElseThrow(() -> new NoSuchElementException("Intervention not found: " + targetId));
				yield new Target(targetType, intervention.getAgency().getId(), intervention.getCreatedBy().getId());
			}
			case "CITIZEN_OBSERVATION" -> {
				CitizenObservation observation = observationRepository.findById(targetId)
						.orElseThrow(() -> new NoSuchElementException("Citizen observation not found: " + targetId));
				yield new Target(targetType, null, observation.getSubmittedBy().getId());
			}
			default -> throw new DomainValidationException("Unsupported evidence target type: " + targetType);
		};
	}

	private PermissionCode reviewPermission(Evidence.Status decision) {
		if (decision == null) {
			throw new DomainValidationException("Evidence review decision is required.");
		}
		return switch (decision) {
			case ACCEPTED -> PermissionCode.EVIDENCE_ACCEPT;
			case REJECTED -> PermissionCode.EVIDENCE_REJECT;
			default -> throw new DomainValidationException(
					"Evidence review decision must be ACCEPTED or REJECTED.");
		};
	}

	private String provenance(CivicPrincipal principal) {
		if (principal.roles().contains(SystemRole.INSPECTOR.name())) {
			return "INSPECTOR_VERIFIED";
		}
		if (principal.roles().contains(SystemRole.CITIZEN.name())) {
			return "CITIZEN_SUBMITTED";
		}
		return "AGENCY_SUBMITTED";
	}

	private Point point(BigDecimal latitude, BigDecimal longitude) {
		if (latitude == null) {
			return null;
		}
		Point point = WGS84.createPoint(new Coordinate(longitude.doubleValue(), latitude.doubleValue()));
		point.setSRID(4326);
		return point;
	}

	private String cleanFilename(String originalFilename) {
		if (originalFilename == null || originalFilename.isBlank()) {
			return null;
		}
		String normalized = originalFilename.replace('\\', '/');
		String filename = normalized.substring(normalized.lastIndexOf('/') + 1).strip();
		return filename.isEmpty() ? null : filename;
	}

	private String normalizedMimeType(String mimeType) {
		return mimeType.strip().toLowerCase(Locale.ROOT);
	}

	private void deleteIfTransactionRollsBack(String fileReference) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == STATUS_ROLLED_BACK) {
					fileStorageService.delete(fileReference);
				}
			}
		});
	}

	private Evidence evidenceForUpdate(UUID id) {
		return evidenceRepository.findForUpdate(id)
				.orElseThrow(() -> new NoSuchElementException("Evidence not found: " + id));
	}

	private void assertVersion(Evidence evidence, long expectedVersion) {
		if (evidence.getVersion() != expectedVersion) {
			throw new StaleEntityVersionException("Evidence", expectedVersion, evidence.getVersion());
		}
	}

	private User actor(CivicPrincipal principal) {
		return userRepository.findById(principal.userId())
				.orElseThrow(() -> new NoSuchElementException(
						"Authenticated user not found: " + principal.userId()));
	}

	private Map<String, Object> state(Evidence evidence) {
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("targetType", evidence.getTargetType());
		state.put("targetId", evidence.getTargetId().toString());
		state.put("type", evidence.getType().name());
		state.put("status", evidence.getStatus().name());
		state.put("uploaderId", evidence.getUploadedBy().getId().toString());
		state.put("fileReference", evidence.getFileReference());
		state.put("checksum", evidence.getChecksum());
		state.put("version", evidence.getVersion());
		return state;
	}

	private EvidenceResult result(Evidence evidence, Instant occurredAt) {
		return new EvidenceResult(
				evidence.getId(), evidence.getTargetType(), evidence.getTargetId(), evidence.getType(),
				evidence.getStatus(), evidence.getFileReference(), evidence.getChecksum(),
				evidence.getFileSizeBytes(), evidence.getVersion(), occurredAt);
	}

	private record Target(String type, UUID agencyId, UUID ownerId) {
	}
}
