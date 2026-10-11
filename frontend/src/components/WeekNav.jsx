import { formatLongDate } from '../format.js'

export default function WeekNav({ weekStart, weekEnd, onPrev, onToday, onNext }) {
  return (
    <nav className="week-nav" aria-label="Choose week">
      <button type="button" className="secondary" onClick={onPrev}>
        ← Prev
      </button>
      <button type="button" className="secondary" onClick={onToday}>
        This week
      </button>
      <button type="button" className="secondary" onClick={onNext}>
        Next →
      </button>
      <span className="week-range">
        {formatLongDate(weekStart)} – {formatLongDate(weekEnd)}
      </span>
    </nav>
  )
}
