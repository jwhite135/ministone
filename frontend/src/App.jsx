import { useEffect, useState } from 'react'
import { getEmployees, getWeek } from './api.js'
import AddEmployeeForm from './components/AddEmployeeForm.jsx'
import EmployeeTotalsTable from './components/EmployeeTotalsTable.jsx'
import ShiftForm from './components/ShiftForm.jsx'
import WeekGrid from './components/WeekGrid.jsx'
import WeekNav from './components/WeekNav.jsx'
import { addDays, todayIso } from './format.js'

export default function App() {
  const [weekDate, setWeekDate] = useState(todayIso)
  const [position, setPosition] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const [week, setWeek] = useState(null)
  const [employees, setEmployees] = useState([])
  const [editing, setEditing] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let ignore = false
    Promise.all([getWeek(weekDate, position), getEmployees()])
      .then(([weekData, employeeData]) => {
        if (!ignore) {
          setWeek(weekData)
          setEmployees(employeeData)
          setError(null)
        }
      })
      .catch((err) => {
        if (!ignore) setError(err.message)
      })
    return () => {
      ignore = true
    }
  }, [weekDate, position, refreshKey])

  const refresh = () => setRefreshKey((k) => k + 1)
  const positions = [...new Set(employees.map((e) => e.position))].sort()
  const today = todayIso()

  function handleShiftSaved(shift) {
    setEditing(null)
    setWeekDate(shift.date)
    refresh()
  }

  function handleShiftDeleted() {
    setEditing(null)
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
                onToday={() => setWeekDate(today)}
                onNext={() => setWeekDate(addDays(week.weekStart, 7))}
              />
            </div>
            <div className="filter">
              <label htmlFor="position-filter">Filter by position</label>
              <select id="position-filter" value={position} onChange={(e) => setPosition(e.target.value)}>
                <option value="">All positions</option>
                {positions.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
              <span className="muted small">Click a shift to edit or delete it.</span>
            </div>
            <WeekGrid
              weekStart={week.weekStart}
              shifts={week.shifts}
              positions={positions}
              selectedId={editing?.id}
              onSelect={setEditing}
            />
          </section>

          <div className="forms">
            <section>
              <ShiftForm
                key={editing?.id ?? `new-${week.weekStart}`}
                employees={employees}
                shift={editing}
                defaultDate={today >= week.weekStart && today <= week.weekEnd ? today : week.weekStart}
                onSaved={handleShiftSaved}
                onDeleted={handleShiftDeleted}
                onCancel={() => setEditing(null)}
              />
            </section>
            <section>
              <AddEmployeeForm onCreated={refresh} />
            </section>
          </div>

          <section aria-labelledby="totals-heading">
            <h2 id="totals-heading">Hours &amp; estimated pay{position && ` (${position} only)`}</h2>
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
