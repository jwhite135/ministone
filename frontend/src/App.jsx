import { useEffect, useState } from 'react'
import { getWeek } from './api.js'
import EmployeeTotalsTable from './components/EmployeeTotalsTable.jsx'
import { formatDay } from './format.js'

export default function App() {
  // Three pieces of state: the data, whether we're waiting for it, and any error.
  const [week, setWeek] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Runs after the first render: fetch this week's schedule from the backend.
  useEffect(() => {
    let ignore = false // set by the cleanup below so a stale response can't overwrite newer state
    getWeek()
      .then((data) => {
        if (!ignore) setWeek(data)
      })
      .catch((err) => {
        if (!ignore) setError(err.message)
      })
      .finally(() => {
        if (!ignore) setLoading(false)
      })
    return () => {
      ignore = true
    }
  }, [])

  return (
    <div className="app">
      <header>
        <h1>Mini ShiftSync</h1>
        {week && (
          <p className="muted">
            Week of {formatDay(week.weekStart)} – {formatDay(week.weekEnd)}
          </p>
        )}
      </header>

      <main>
        {loading && <p className="muted">Loading schedule…</p>}
        {error && (
          <p className="error" role="alert">
            {error}
          </p>
        )}
        {week && (
          <section>
            <h2>Hours &amp; estimated pay</h2>
            <EmployeeTotalsTable
              employees={week.employees}
              totalHours={week.totalHours}
              totalPay={week.totalPay}
            />
          </section>
        )}
      </main>
    </div>
  )
}
