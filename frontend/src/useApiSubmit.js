import { useState } from 'react'

/**
 * Shared form-submission state: whether a request is in flight, a general error message, and
 * per-field errors from the backend's validation (e.g. { name: "must not be blank" }).
 */
export default function useApiSubmit() {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [fieldErrors, setFieldErrors] = useState({})

  /** Runs `action` (an async API call). Returns its result, or null if it failed. */
  async function run(action) {
    setSubmitting(true)
    setError(null)
    setFieldErrors({})
    try {
      return await action()
    } catch (err) {
      const fields = err.fieldErrors ?? {}
      setFieldErrors(fields)
      setError(Object.keys(fields).length > 0 ? 'Please fix the highlighted fields.' : err.message)
      return null
    } finally {
      setSubmitting(false)
    }
  }

  return { run, submitting, error, fieldErrors }
}
