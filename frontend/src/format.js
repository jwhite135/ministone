const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

export function formatMoney(amount) {
  return money.format(amount)
}

export function formatHours(hours) {
  return `${Number(hours).toFixed(1)} h`
}

// Dates travel as "yyyy-MM-dd" strings. We build Date objects in *local* time (never
// new Date("2026-10-12"), which is UTC midnight and can show as the previous day in the US).

function fromIso(isoDate) {
  const [y, m, d] = isoDate.split('-').map(Number)
  return new Date(y, m - 1, d)
}

function toIso(date) {
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function todayIso() {
  return toIso(new Date())
}

export function addDays(isoDate, days) {
  const date = fromIso(isoDate)
  date.setDate(date.getDate() + days)
  return toIso(date)
}

/** "2026-10-12" -> "Mon, Oct 12" */
export function formatDay(isoDate) {
  return fromIso(isoDate).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })
}

/** "2026-10-12" -> "Mon" */
export function formatWeekday(isoDate) {
  return fromIso(isoDate).toLocaleDateString('en-US', { weekday: 'short' })
}

/** "2026-10-12" -> "Oct 12, 2026" */
export function formatLongDate(isoDate) {
  return fromIso(isoDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
