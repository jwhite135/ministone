import { addDays, formatDay, formatHours, formatWeekday, todayIso } from '../format.js'

const POSITION_COLORS = ['#b15d12', '#2f6fd6', '#8a3fb8', '#1e8a5a', '#c2334d', '#0f7f8f']

export default function WeekGrid({ weekStart, shifts, positions, selectedId, onSelect }) {
  const days = Array.from({ length: 7 }, (_, i) => addDays(weekStart, i))
  const today = todayIso()
  const colorFor = (position) => POSITION_COLORS[positions.indexOf(position) % POSITION_COLORS.length]

  // A shift carried over from last Sunday night is shown on Monday.
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
                <li key={s.id}>
                  <ShiftCard
                    shift={s}
                    color={colorFor(s.position)}
                    carriedOver={s.date < weekStart}
                    selected={s.id === selectedId}
                    onSelect={onSelect}
                  />
                </li>
              ))}
            </ul>
          )}
        </div>
      ))}
    </div>
  )
}

function ShiftCard({ shift, color, carriedOver, selected, onSelect }) {
  const partial = Number(shift.hoursInWeek) !== Number(shift.hours)
  return (
    <button
      type="button"
      className={'shift-card' + (selected ? ' selected' : '')}
      style={{ '--position-color': color }}
      aria-pressed={selected}
      title="Click to edit"
      onClick={() => onSelect(shift)}
    >
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
    </button>
  )
}
