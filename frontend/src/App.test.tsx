import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('CivicOS operational shell', () => {
  it('opens the coordinator command center as the product-first workspace', () => {
    render(<App pathname="/" />)

    expect(screen.getByRole('heading', { name: /command center/i })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: /primary navigation/i })).toBeInTheDocument()
    expect(screen.getByText(/foundation ready/i)).toBeInTheDocument()
    expect(screen.getByText(/simulated unless a verified live connection/i)).toBeInTheDocument()
  })

  it('renders role-specific navigation from the route definition', () => {
    render(<App pathname="/app/agency/interventions" />)

    expect(screen.getByRole('heading', { name: /move assigned work through governed execution/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Interventions' })).toHaveAttribute('aria-current', 'page')
    expect(screen.queryByRole('link', { name: 'Conflicts' })).not.toBeInTheDocument()
  })

  it('provides text semantics for status and workflow information', () => {
    render(<App pathname="/app/inspector/verification" />)

    expect(screen.getByRole('status')).toHaveTextContent('Foundation ready')
    expect(screen.getByText('Determined by backend assignment')).toBeInTheDocument()
    expect(screen.getByText('Validated and executed by backend')).toBeInTheDocument()
  })
})
