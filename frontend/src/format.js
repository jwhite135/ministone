const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

export function formatMoney(amount) {
  return money.format(amount)
}

export function formatHours(hours) {
  return `${Number(hours).toFixed(1)} h`
}

/** "2026-10-12" -> "Mon, Oct 12". Parsed as local noon to avoid time-zone shifts to the previous day. */
export function formatDay(isoDate) {
  return new Date(`${isoDate}T12:00:00`).toLocaleDateString('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  })
}
