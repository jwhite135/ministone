const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(API_URL + path, {
      ...options,
      // A Content-Type header on GETs would trigger a CORS preflight for every request.
      headers: options.body ? { 'Content-Type': 'application/json' } : {},
    })
  } catch {
    // Server down and CORS-blocked look identical from JavaScript.
    throw new Error(
      `Can't reach the server at ${API_URL}. Is the backend running? ` +
        `If it is, check the browser console for a CORS error.`,
    )
  }

  const body = response.status === 204 ? null : await response.json().catch(() => null)
  if (!response.ok) {
    const error = new Error(body?.detail ?? `Request failed (${response.status})`)
    error.fieldErrors = body?.fieldErrors ?? {}
    throw error
  }
  return body
}

export function getWeek(date, position) {
  const params = new URLSearchParams()
  if (date) params.set('date', date)
  if (position) params.set('position', position)
  return request(`/api/schedule/week?${params}`)
}

export function getEmployees() {
  return request('/api/employees')
}

export function createEmployee(employee) {
  return request('/api/employees', { method: 'POST', body: JSON.stringify(employee) })
}

export function createShift(shift) {
  return request('/api/shifts', { method: 'POST', body: JSON.stringify(shift) })
}

export function updateShift(id, shift) {
  return request(`/api/shifts/${id}`, { method: 'PUT', body: JSON.stringify(shift) })
}

export function deleteShift(id) {
  return request(`/api/shifts/${id}`, { method: 'DELETE' })
}
