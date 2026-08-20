import { useEffect, useState } from 'react'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { listConflicts } from './api'
import { CoordinationWorkspace } from './CoordinationWorkspace'
import type { CurrentUser } from './types'

export function CoordinationWorkspacePage({ accessToken, user, conflictId }: { accessToken: string; user: CurrentUser; conflictId?: string }) {
  const [selectedId, setSelectedId] = useState<string | null>(conflictId ?? null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (conflictId || selectedId) return
    let active = true
    listConflicts(accessToken, { status: 'OPEN', size: 1 })
      .then(({ data }) => { if (active) setSelectedId(data[0]?.id ?? '') })
      .catch((failure: unknown) => {
        if (active) setError(failure instanceof Error ? failure.message : 'Coordination work could not be loaded.')
      })
    return () => { active = false }
  }, [accessToken, conflictId, selectedId])

  if (error) return <ErrorState title="Coordination work could not be loaded" description={error} />
  if (selectedId === '') return <EmptyState title="No open coordination work" description="There is no open conflict requiring a coordination decision." />
  if (!selectedId) return <LoadingState label="Selecting the highest-priority open conflict" />
  return <CoordinationWorkspace accessToken={accessToken} user={user} conflictId={selectedId} headingLevel="h1" />
}
