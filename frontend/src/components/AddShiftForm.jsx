import { useState } from 'react'
import { createShift } from '../api.js'
import { formatDay } from '../format.js'
import useApiSubmit from '../useApiSubmit.js'
import Field from './Field.jsx'
import { fieldProps } from './fieldProps.js'

export default function AddShiftForm({ employees, defaultDate, onCreated }) {
  const [form, setForm] = useState({ employeeId: '', date: defaultDate, startTime: '', endTime: '' })
  const [success, setSuccess] = useState(null)
  const { run, submitting, error, fieldErrors } = useApiSubmit()

  function handleChange(event) {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  // Derived from state on every render, so the hint updates as the user types.
  // "HH:mm" strings compare correctly as text: "06:00" < "22:00".
  const endsNextDay = form.startTime && form.endTime && form.endTime < form.startTime
  const endHint = endsNextDay ? 'Ends the next day (overnight shift)' : null

  async function handleSubmit(event) {
    event.preventDefault()
    setSuccess(null)
    const created = await run(() =>
      createShift({
        employeeId: form.employeeId === '' ? null : Number(form.employeeId),
        date: form.date || null,
        startTime: form.startTime || null,
        endTime: form.endTime || null,
      }),
    )
    if (created) {
      const employee = employees.find((e) => e.employeeId === created.employeeId)
      setSuccess(`Added a shift for ${employee?.name ?? 'employee'} on ${formatDay(created.date)}.`)
      setForm((prev) => ({ ...prev, startTime: '', endTime: '' }))
      onCreated(created)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <h2>Add shift</h2>

      <Field id="shift-employee" label="Employee" error={fieldErrors.employeeId}>
        <select
          name="employeeId"
          value={form.employeeId}
          onChange={handleChange}
          {...fieldProps('shift-employee', fieldErrors.employeeId)}
        >
          <option value="">Choose an employee…</option>
          {employees.map((e) => (
            <option key={e.employeeId} value={e.employeeId}>
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

      <button type="submit" disabled={submitting}>
        {submitting ? 'Adding…' : 'Add shift'}
      </button>

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
