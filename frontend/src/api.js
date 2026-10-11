// All calls to the Spring Boot backend go through this file.
// The URL can be overridden with a VITE_API_URL entry in frontend/.env.local.
const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(API_URL + path, {
      ...options,
      // Only declare JSON when we send a body. On a GET this header would turn a "simple" request
      // into one that needs a CORS preflight (an extra OPTIONS round trip) every time.
      headers: options.body ? { 'Content-Type': 'application/json' } : {},
    })
  } catch {
    // fetch only throws when there is no usable response: the server is down, or the browser
    // blocked it because of CORS. For security, the browser doesn't tell JavaScript which one.
    throw new Error(
      `Can't reach the server at ${API_URL}. Is the backend running? ` +
        `If it is, check the browser console for a CORS error.`,
    )
  }

  const body = response.status === 204 ? null : await response.json().catch(() => null)
  if (!response.ok) {
    // The backend sends errors as problem details: { status, title, detail, fieldErrors? }
    const error = new Error(body?.detail ?? `Request failed (${response.status})`)
    error.status = response.status
    error.fieldErrors = body?.fieldErrors ?? {}
    throw error
  }
  return body
}

/**
 * The Monday-to-Sunday week containing `date` (a "yyyy-MM-dd" string), or this week if omitted.
 * `position` (optional) limits the shifts and totals to one position.
 */
export function getWeek(date, position) {
  const params = new URLSearchParams()
  if (date) params.set('date', date)
  if (position) params.set('position', position)
  return request(`/api/schedule/week?${params}`)
}

export function getEmployees() {
  return request('/api/employees')
}

/** `employee` is { name, position, hourlyRate }. */
export function createEmployee(employee) {
  return request('/api/employees', { method: 'POST', body: JSON.stringify(employee) })
}

/** `shift` is { employeeId, date: "yyyy-MM-dd", startTime: "HH:mm", endTime: "HH:mm" }. */
export function createShift(shift) {
  return request('/api/shifts', { method: 'POST', body: JSON.stringify(shift) })
}

export function updateShift(id, shift) {
  return request(`/api/shifts/${id}`, { method: 'PUT', body: JSON.stringify(shift) })
}

export function deleteShift(id) {
  return request(`/api/shifts/${id}`, { method: 'DELETE' })
}
