import { addDays, formatDay, formatHours, formatWeekday, todayIso } from '../format.js'

const POSITION_COLORS = ['#b15d12', '#2f6fd6', '#8a3fb8', '#1e8a5a', '#c2334d', '#0f7f8f']

/** `positions` is every position name; colors are handed out in alphabetical order so each differs. */
export default function WeekGrid({ weekStart, shifts, positions }) {
  const days = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i))
  const today = todayIso()
  const sortedPositions = [...new Set(positions)].sort()
  const colorFor = (position) => POSITION_COLORS[sortedPositions.indexOf(position) % POSITION_COLORS.length]

  // Group shifts by day. A shift from last Sunday night that runs into Monday shows on Monday.
  // ISO date strings sort and compare correctly as text.
  const shiftsByDay = Object.fromEntries(days.map((d) => [d, []]))
  for (const shift of shifts) {
    shiftsByDay[shift.date < weekStart ? weekStart : shift.date].push(shift)
  }

  return (
    <div className="week-grid">
      {days.map((day) => (
        <div key={day} className={'day' + (day === today ? ' today' : '')}>
          <h3>{formatDay(day)}</h3>
          {shiftsByDay[day].length === 0 ? (
            <p className="muted small">No shifts</p>
          ) : (
            <ul>
              {shiftsByDay[day].map((s) => (
                <ShiftCard key={s.id} shift={s} color={colorFor(s.position)} carriedOver={s.date < weekStart} />
              ))}
            </ul>
          )}
        </div>
      ))}
    </div>
  )
}

function ShiftCard({ shift, color, carriedOver }) {
  const partial = Number(shift.hoursInWeek) !== Number(shift.hours)
  return (
    <li className="shift-card" style={{ '--position-color': color }}>
      <strong>{shift.employeeName}</strong>
      <span className="position">{shift.position}</span>
      <span className="time">
        {carriedOver && `${formatWeekday(shift.date)} `}
        {shift.startTime}–{shift.endTime}
        {shift.endsNextDay && (
          <abbr className="next-day" title="Ends the next day">
            +1
          </abbr>
        )}
      </span>
      <span className="muted small">
        {partial ? `${formatHours(shift.hoursInWeek)} this week of ${formatHours(shift.hours)}` : formatHours(shift.hours)}
      </span>
    </li>
  )
}
