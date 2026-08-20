import { useEffect, useState } from 'react'
import { EmptyState } from '../../components/feedback/EmptyState'
import { ErrorState } from '../../components/feedback/ErrorState'
import { LoadingState } from '../../components/feedback/LoadingState'
import { listNotifications } from './api'
import { formatDate } from './formatters'
import type { NotificationItem } from './types'

export function CitizenNotificationsPage({ accessToken }: { accessToken: string }) {
  const [notifications, setNotifications] = useState<NotificationItem[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    listNotifications(accessToken)
      .then((response) => { if (active) setNotifications(response.data) })
      .catch((failure: unknown) => {
        if (active) setError(failure instanceof Error ? failure.message : 'Notifications could not be loaded.')
      })
    return () => { active = false }
  }, [accessToken])

  return (
    <>
      <header className="citizen-page-heading">
        <div><p className="eyebrow">Citizen updates</p><h1>Notifications</h1><p>Updates and requests that need your attention.</p></div>
      </header>
      <section className="panel" aria-labelledby="notifications-title">
        <div className="panel-header"><h2 id="notifications-title">Recent notifications</h2></div>
        {error ? <ErrorState title="Notifications could not be loaded" description={error} /> : null}
        {!error && notifications === null ? <LoadingState label="Loading notifications" /> : null}
        {!error && notifications?.length === 0 ? <EmptyState title="No notifications" description="Updates about your reports will appear here." /> : null}
        {notifications && notifications.length > 0 ? (
          <ul className="notification-list">
            {notifications.map((item) => (
              <li key={item.notificationId} data-unread={!item.readAt}>
                <div><strong>{item.title}</strong><p>{item.message}</p></div>
                <time dateTime={item.createdAt}>{formatDate(item.createdAt)}</time>
              </li>
            ))}
          </ul>
        ) : null}
      </section>
    </>
  )
}
