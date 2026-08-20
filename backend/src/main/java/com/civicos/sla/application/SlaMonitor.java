package com.civicos.sla.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.civicos.sla.config.SlaPolicyProperties;
import com.civicos.sla.domain.Sla;
import com.civicos.sla.repository.SlaRepository;

@Component
@ConditionalOnProperty(
		prefix = "civicos.sla", name = "monitor-enabled", havingValue = "true", matchIfMissing = true)
public class SlaMonitor {

	private final SlaRepository slaRepository;
	private final SlaService slaService;
	private final SlaPolicyProperties policy;
	private final Clock clock;

	public SlaMonitor(
			SlaRepository slaRepository,
			SlaService slaService,
			SlaPolicyProperties policy,
			Clock clock) {
		this.slaRepository = slaRepository;
		this.slaService = slaService;
		this.policy = policy;
		this.clock = clock;
	}

	@Scheduled(fixedDelayString = "${civicos.sla.monitor-interval-ms:60000}")
	public void monitor() {
		Instant now = clock.instant();
		Instant threshold = now.plus(Duration.ofHours(policy.getAtRiskBeforeHours()));
		List<Sla> due = slaRepository.findByStatusInAndDeadlineBefore(
				List.of(Sla.Status.NORMAL, Sla.Status.AT_RISK, Sla.Status.BREACHED), threshold);
		for (Sla sla : due) {
			slaService.monitorOne(sla.getId(), now, "sla-monitor");
		}
	}
}
