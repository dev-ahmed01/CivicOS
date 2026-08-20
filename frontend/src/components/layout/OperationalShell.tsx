import type { ReactNode } from 'react'
import { workspaces, type ResolvedWorkspace } from '../../config/workspaces'

type OperationalShellProps = {
  route: ResolvedWorkspace
  children: ReactNode
}

export function OperationalShell({ route, children }: OperationalShellProps) {
  const { workspace } = route

  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">Skip to main content</a>
      <aside className="app-sidebar" aria-label={`${workspace.label} workspace navigation`}>
        <a className="brand" href={workspace.homePath} aria-label="CivicOS home">
          <span className="brand-mark" aria-hidden="true">CO</span>
          <span className="brand-copy">
            <strong>CivicOS</strong>
            <span>Coordination layer</span>
          </span>
        </a>

        <p className="workspace-label">{workspace.label} workspace</p>
        <nav className="primary-nav" aria-label="Primary navigation">
          <ul>
            {workspace.routes.map((item) => (
              <li key={item.path}>
                <a
                  className="nav-link"
                  href={item.path}
                  aria-current={route.found && item.path === route.route.path ? 'page' : undefined}
                >
                  <span className="nav-symbol" aria-hidden="true">{item.symbol}</span>
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="role-switcher">
          <p className="workspace-label">Workspace preview</p>
          <nav className="role-nav" aria-label="Role workspaces">
            <ul>
              {workspaces.map((item) => (
                <li key={item.role}>
                  <a
                    className="role-link"
                    href={item.homePath}
                    aria-current={item.role === workspace.role ? 'true' : undefined}
                  >
                    {item.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
        </div>

        <p className="integration-note">
          External MARCS-style records are simulated unless a verified live connection is configured.
        </p>
      </aside>

      <div className="app-frame">
        <header className="app-header">
          <p className="breadcrumb">
            {workspace.label} <span aria-hidden="true">/</span> <strong>{route.route.label}</strong>
          </p>
          <span className="environment-badge">API-backed workspace</span>
        </header>
        <main id="main-content" className="page-content" tabIndex={-1}>
          {children}
        </main>
      </div>
    </div>
  )
}
