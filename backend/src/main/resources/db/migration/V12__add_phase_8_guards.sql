CREATE UNIQUE INDEX uq_sla_instances_open_target_type
    ON sla_instances (target_type, target_id, sla_type)
    WHERE status IN ('NORMAL', 'AT_RISK', 'BREACHED', 'PAUSED');

CREATE UNIQUE INDEX uq_escalations_sla_level
    ON escalations (sla_id, level);
