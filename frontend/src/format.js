const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

export function formatMoney(amount) {
  return money.format(amount)
}

export function formatHours(hours) {
  return `${Number(hours).toFixed(1)} h`
}

// new Date('2026-10-12') is UTC midnight, which is the previous day in US time zones,
// so ISO dates are parsed into local time instead.

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

export function formatDay(isoDate) {
  return fromIso(isoDate).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })
}

export function formatWeekday(isoDate) {
  return fromIso(isoDate).toLocaleDateString('en-US', { weekday: 'short' })
}

export function formatLongDate(isoDate) {
  return fromIso(isoDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}
