import { useEffect, useRef, useState } from 'react'
import { createShift, deleteShift, updateShift } from '../api.js'
import { formatDay } from '../format.js'
import useApiSubmit from '../useApiSubmit.js'
import Field from './Field.jsx'
import { fieldProps } from './fieldProps.js'

// Rendered with a key per shift (or per week when adding), so it remounts with fresh state.
export default function ShiftForm({ employees, shift, defaultDate, onSaved, onDeleted, onCancel }) {
  const editing = shift != null
  const [form, setForm] = useState(() =>
    editing
      ? { employeeId: String(shift.employeeId), date: shift.date, startTime: shift.startTime, endTime: shift.endTime }
      : { employeeId: '', date: defaultDate, startTime: '', endTime: '' },
  )
  const [success, setSuccess] = useState(null)
  const { run, submitting, error, fieldErrors } = useApiSubmit()
  const formRef = useRef(null)
  const firstFieldRef = useRef(null)

  useEffect(() => {
    if (editing) {
      formRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' })
      firstFieldRef.current.focus({ preventScroll: true })
    }
  }, [editing])

  function handleChange(event) {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  const endsNextDay = form.startTime && form.endTime && form.endTime < form.startTime
  const endHint = endsNextDay ? 'Ends the next day (overnight shift)' : null
  const nameOf = (employeeId) => employees.find((e) => e.id === employeeId)?.name ?? 'employee'

  async function handleSubmit(event) {
    event.preventDefault()
    setSuccess(null)
    const body = {
      employeeId: form.employeeId === '' ? null : Number(form.employeeId),
      date: form.date || null,
      startTime: form.startTime || null,
      endTime: form.endTime || null,
    }
    const saved = await run(() => (editing ? updateShift(shift.id, body) : createShift(body)))
    if (saved) {
      if (!editing) {
        setSuccess(`Added a shift for ${nameOf(saved.employeeId)} on ${formatDay(saved.date)}.`)
        setForm((prev) => ({ ...prev, startTime: '', endTime: '' }))
      }
      onSaved(saved)
    }
  }

  async function handleDelete() {
    const label = `${shift.employeeName}'s shift on ${formatDay(shift.date)}, ${shift.startTime}–${shift.endTime}`
    if (!window.confirm(`Delete ${label}?`)) return
    const deleted = await run(() => deleteShift(shift.id).then(() => true))
    if (deleted) onDeleted()
  }

  return (
    <form ref={formRef} onSubmit={handleSubmit} noValidate>
      <h2>{editing ? 'Edit shift' : 'Add shift'}</h2>

      <Field id="shift-employee" label="Employee" error={fieldErrors.employeeId}>
        <select
          ref={firstFieldRef}
          name="employeeId"
          value={form.employeeId}
          onChange={handleChange}
          {...fieldProps('shift-employee', fieldErrors.employeeId)}
        >
          <option value="">Choose an employee…</option>
          {employees.map((e) => (
            <option key={e.id} value={e.id}>
              {e.name} ({e.position})
            </option>
          ))}
        </select>
      </Field>

      <Field id="shift-date" label="Date" error={fieldErrors.date}>
        <input
          name="date"
          type="date"
          value={form.date}
          onChange={handleChange}
          {...fieldProps('shift-date', fieldErrors.date)}
        />
      </Field>

      <div className="field-row">
        <Field id="shift-start" label="Start" error={fieldErrors.startTime}>
          <input
            name="startTime"
            type="time"
            value={form.startTime}
            onChange={handleChange}
            {...fieldProps('shift-start', fieldErrors.startTime)}
          />
        </Field>
        <Field id="shift-end" label="End" error={fieldErrors.endTime} hint={endHint}>
          <input
            name="endTime"
            type="time"
            value={form.endTime}
            onChange={handleChange}
            {...fieldProps('shift-end', fieldErrors.endTime, endHint)}
          />
        </Field>
      </div>

      <div className="form-actions">
        <button type="submit" disabled={submitting}>
          {submitting ? 'Saving…' : editing ? 'Save changes' : 'Add shift'}
        </button>
        {editing && (
          <>
            <button type="button" className="secondary" onClick={onCancel} disabled={submitting}>
              Cancel
            </button>
            <button type="button" className="danger" onClick={handleDelete} disabled={submitting}>
              Delete
            </button>
          </>
        )}
      </div>

      {error && (
        <p className="error" role="alert">
          {error}
        </p>
      )}
      {success && (
        <p className="success" role="status">
          {success}
        </p>
      )}
    </form>
  )
}
