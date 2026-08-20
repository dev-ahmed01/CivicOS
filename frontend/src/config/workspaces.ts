export type WorkspaceRole = 'citizen' | 'agency' | 'coordinator' | 'inspector' | 'admin'

export type WorkspaceRoute = {
  label: string
  path: string
  symbol: string
  summary: string
}

export type Workspace = {
  role: WorkspaceRole
  label: string
  homePath: string
  routes: readonly WorkspaceRoute[]
}

export type ResolvedWorkspace = {
  workspace: Workspace
  route: WorkspaceRoute
  requestedPath: string
  found: boolean
}

export const workspaces: readonly Workspace[] = [
  {
    role: 'citizen',
    label: 'Citizen',
    homePath: '/app/citizen/report',
    routes: [
      { label: 'Report issue', path: '/app/citizen/report', symbol: '01', summary: 'Report a road issue with location, evidence, and a clear description.' },
      { label: 'My reports', path: '/app/citizen/reports', symbol: '02', summary: 'Track submitted observations and their public-safe lifecycle updates.' },
      { label: 'Notifications', path: '/app/citizen/notifications', symbol: '03', summary: 'Review updates that need citizen attention or validation.' },
    ],
  },
  {
    role: 'agency',
    label: 'Agency',
    homePath: '/app/agency/dashboard',
    routes: [
      { label: 'Dashboard', path: '/app/agency/dashboard', symbol: '01', summary: 'See assigned work, deadlines, approvals, and evidence actions.' },
      { label: 'Interventions', path: '/app/agency/interventions', symbol: '02', summary: 'Manage the agency interventions contained within coordinated cases.' },
      { label: 'Approvals', path: '/app/agency/approvals', symbol: '03', summary: 'Review approval requests, conditions, dependencies, and risks.' },
      { label: 'Evidence', path: '/app/agency/evidence', symbol: '04', summary: 'Submit and review provenance-preserving completion evidence.' },
    ],
  },
  {
    role: 'coordinator',
    label: 'Coordinator',
    homePath: '/app/coordinator/dashboard',
    routes: [
      { label: 'Command center', path: '/app/coordinator/dashboard', symbol: '01', summary: 'Prioritize conflicts, SLA risk, approvals, verification, and upcoming work.' },
      { label: 'Conflicts', path: '/app/coordinator/conflicts', symbol: '02', summary: 'Understand deterministic conflicts and their affected interventions.' },
      { label: 'Coordination', path: '/app/coordinator/coordination', symbol: '03', summary: 'Sequence cross-agency work and record accountable human decisions.' },
      { label: 'Map', path: '/app/coordinator/map', symbol: '04', summary: 'Explore spatial work context with an equivalent operational list.' },
      { label: 'SLA', path: '/app/coordinator/sla', symbol: '05', summary: 'Act on at-risk and breached service-level commitments.' },
    ],
  },
  {
    role: 'inspector',
    label: 'Inspector',
    homePath: '/app/inspector/inspections',
    routes: [
      { label: 'Inspections', path: '/app/inspector/inspections', symbol: '01', summary: 'Complete assigned field inspections with a mobile-friendly checklist.' },
      { label: 'Evidence', path: '/app/inspector/evidence', symbol: '02', summary: 'Review evidence and its provenance before recording findings.' },
      { label: 'Verification', path: '/app/inspector/verification', symbol: '03', summary: 'Verify completion or return work for corrective action.' },
    ],
  },
  {
    role: 'admin',
    label: 'Admin',
    homePath: '/app/admin/users',
    routes: [
      { label: 'Users', path: '/app/admin/users', symbol: '01', summary: 'Manage user access without weakening backend authorization.' },
      { label: 'Roles', path: '/app/admin/roles', symbol: '02', summary: 'Review roles and explicit permission assignments.' },
      { label: 'Agencies', path: '/app/admin/agencies', symbol: '03', summary: 'Maintain participating agencies and their governance metadata.' },
      { label: 'Configuration', path: '/app/admin/configuration', symbol: '04', summary: 'Manage governed system configuration and deterministic rules.' },
      { label: 'Audit', path: '/app/admin/audit', symbol: '05', summary: 'Trace authoritative actions through immutable audit history.' },
    ],
  },
] as const

const defaultWorkspace = workspaces.find(({ role }) => role === 'coordinator') ?? workspaces[0]

export function resolveWorkspace(pathname: string): ResolvedWorkspace {
  for (const workspace of workspaces) {
    const route = workspace.routes.find(({ path }) => path === pathname)
    if (route) {
      return { workspace, route, requestedPath: pathname, found: true }
    }

    if (pathname.startsWith(`/app/${workspace.role}/`)) {
      if (workspace.role === 'citizen' && pathname.startsWith('/app/citizen/reports/')) {
        return {
          workspace,
          route: workspace.routes.find(({ path }) => path === '/app/citizen/reports') ?? workspace.routes[0],
          requestedPath: pathname,
          found: true,
        }
      }
      return { workspace, route: workspace.routes[0], requestedPath: pathname, found: false }
    }
  }

  return {
    workspace: defaultWorkspace,
    route: defaultWorkspace.routes[0],
    requestedPath: pathname,
    found: pathname === '/' || pathname === '/app',
  }
}
