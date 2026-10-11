import { useEffect, useState } from 'react'
import { getWeek } from './api.js'
import AddEmployeeForm from './components/AddEmployeeForm.jsx'
import AddShiftForm from './components/AddShiftForm.jsx'
import EmployeeTotalsTable from './components/EmployeeTotalsTable.jsx'
import WeekGrid from './components/WeekGrid.jsx'
import WeekNav from './components/WeekNav.jsx'
import { addDays, todayIso } from './format.js'

export default function App() {
  // Any date inside the week being viewed; the backend works out Monday-Sunday from it.
  const [weekDate, setWeekDate] = useState(todayIso)
  // Bumped after every change to force a reload of the current week.
  const [refreshKey, setRefreshKey] = useState(0)
  const [week, setWeek] = useState(null)
  const [error, setError] = useState(null)

  // Re-runs whenever the viewed week changes or something was added.
  useEffect(() => {
    let ignore = false // set by the cleanup below so a stale response can't overwrite newer state
    getWeek(weekDate)
      .then((data) => {
        if (!ignore) {
          setWeek(data)
          setError(null)
        }
      })
      .catch((err) => {
        if (!ignore) setError(err.message)
      })
    return () => {
      ignore = true
    }
  }, [weekDate, refreshKey])

  const refresh = () => setRefreshKey((k) => k + 1)

  function handleShiftCreated(shift) {
    setWeekDate(shift.date) // jump to the week the new shift is in
    refresh()
  }

  return (
    <div className="app">
      <header>
        <h1>Mini ShiftSync</h1>
        <p className="muted">Weekly staff schedule, hours, and estimated pay</p>
      </header>

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {!week && !error && <p className="muted">Loading schedule…</p>}

      {week && (
        <main>
          <section aria-labelledby="schedule-heading">
            <div className="section-header">
              <h2 id="schedule-heading">Schedule</h2>
              <WeekNav
                weekStart={week.weekStart}
                weekEnd={week.weekEnd}
                onPrev={() => setWeekDate(addDays(week.weekStart, -7))}
                onToday={() => setWeekDate(todayIso())}
                onNext={() => setWeekDate(addDays(week.weekStart, 7))}
              />
            </div>
            <WeekGrid
              weekStart={week.weekStart}
              shifts={week.shifts}
              positions={week.employees.map((e) => e.position)}
            />
          </section>

          <div className="forms">
            <section>
              <AddShiftForm employees={week.employees} defaultDate={todayIso()} onCreated={handleShiftCreated} />
            </section>
            <section>
              <AddEmployeeForm onCreated={refresh} />
            </section>
          </div>

          <section aria-labelledby="totals-heading">
            <h2 id="totals-heading">Hours &amp; estimated pay</h2>
            <EmployeeTotalsTable
              employees={week.employees}
              totalHours={week.totalHours}
              totalPay={week.totalPay}
            />
          </section>
        </main>
      )}
    </div>
  )
}
