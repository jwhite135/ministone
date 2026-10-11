// All calls to the Spring Boot backend go through this file.
// The URL can be overridden with a VITE_API_URL entry in frontend/.env.local.
const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(API_URL + path, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...options.headers },
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

/** The Monday-to-Sunday week containing `date` (a "yyyy-MM-dd" string), or this week if omitted. */
export function getWeek(date) {
  return request('/api/schedule/week' + (date ? `?date=${date}` : ''))
}
