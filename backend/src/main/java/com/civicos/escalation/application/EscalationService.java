package com.civicos.escalation.application;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.civicos.audit.domain.AuditEvent;
import com.civicos.audit.repository.AuditEventRepository;
import com.civicos.escalation.domain.Escalation;
import com.civicos.escalation.repository.EscalationRepository;
import com.civicos.sla.domain.Sla;

@Service
public class EscalationService {

	private final EscalationRepository escalationRepository;
	private final AuditEventRepository auditEventRepository;

	public EscalationService(
			EscalationRepository escalationRepository,
			AuditEventRepository auditEventRepository) {
		this.escalationRepository = escalationRepository;
		this.auditEventRepository = auditEventRepository;
	}

	public boolean createForBreach(Sla sla, String requestId) {
		if (escalationRepository.existsBySlaIdAndLevel(sla.getId(), 1)) {
			return false;
		}
		String reason = "SLA deadline breached without completion.";
		Escalation escalation = Escalation.create(sla, 1, reason, null);
		escalationRepository.saveAndFlush(escalation);
		Map<String, Object> state = new LinkedHashMap<>();
		state.put("slaId", sla.getId().toString());
		state.put("level", escalation.getLevel());
		state.put("status", escalation.getStatus().name());
		state.put("reason", escalation.getReason());
		auditEventRepository.save(AuditEvent.domainMutation(
				null, "SLA_ESCALATED", "ESCALATION", escalation.getId(),
				null, state, reason, requestId));
		return true;
	}
}
