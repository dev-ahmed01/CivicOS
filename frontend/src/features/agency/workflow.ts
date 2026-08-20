export type AgencyWorkflowAction = {
  action: string
  label: string
  permission: string
  reasonPrompt: string
  kind: 'transition' | 'approval-request'
}

const actionsByStatus: Readonly<Record<string, AgencyWorkflowAction>> = {
  DRAFT: { action: 'submit', label: 'Submit for review', permission: 'INTERVENTION_SUBMIT', reasonPrompt: 'Why is this intervention ready for review?', kind: 'transition' },
  COORDINATION_COMPLETE: { action: 'request-approval', label: 'Request approval', permission: 'APPROVAL_REQUEST', reasonPrompt: 'Why is this intervention ready for approval?', kind: 'approval-request' },
  APPROVED: { action: 'schedule', label: 'Schedule work', permission: 'INTERVENTION_SCHEDULE', reasonPrompt: 'Confirm why this work can now be scheduled.', kind: 'transition' },
  SCHEDULED: { action: 'start', label: 'Start work', permission: 'INTERVENTION_START', reasonPrompt: 'Confirm that field work is starting.', kind: 'transition' },
  IN_PROGRESS: { action: 'complete', label: 'Complete primary work', permission: 'INTERVENTION_COMPLETE', reasonPrompt: 'Summarize the completed primary work.', kind: 'transition' },
  RESTORATION: { action: 'complete-restoration', label: 'Complete restoration', permission: 'INTERVENTION_COMPLETE', reasonPrompt: 'Summarize the completed restoration.', kind: 'transition' },
  EVIDENCE_PENDING: { action: 'submit-evidence', label: 'Request verification', permission: 'INTERVENTION_COMPLETE', reasonPrompt: 'Confirm that required accepted evidence is ready.', kind: 'transition' },
  ON_HOLD: { action: 'resume', label: 'Resume work', permission: 'INTERVENTION_RESUME', reasonPrompt: 'Explain why the hold can be lifted.', kind: 'transition' },
  REJECTED: { action: 'revise-rejected', label: 'Revise rejected work', permission: 'INTERVENTION_REOPEN', reasonPrompt: 'Explain how the rejected intervention will be revised.', kind: 'transition' },
  REOPENED: { action: 'begin-corrective-action', label: 'Begin corrective action', permission: 'INTERVENTION_REOPEN', reasonPrompt: 'Describe the corrective action being started.', kind: 'transition' },
}

export function primaryAction(status: string, permissions: readonly string[]) {
  const action = actionsByStatus[status]
  return action && permissions.includes(action.permission) ? action : null
}

export function actionableStatuses(permissions: readonly string[]) {
  return Object.entries(actionsByStatus)
    .filter(([, action]) => permissions.includes(action.permission))
    .map(([status]) => status)
}
