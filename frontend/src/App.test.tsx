import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('CivicOS application shell', () => {
  it('identifies the product and current implementation phase', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', {
        name: /coordinate before the road is cut again/i,
      }),
    ).toBeInTheDocument()
    expect(screen.getByText(/phase 1 foundation/i)).toBeInTheDocument()
  })
})
