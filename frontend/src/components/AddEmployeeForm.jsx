import { useState } from 'react'
import { createEmployee } from '../api.js'
import useApiSubmit from '../useApiSubmit.js'
import Field from './Field.jsx'
import { fieldProps } from './fieldProps.js'

const EMPTY = { name: '', position: '', hourlyRate: '' }
const POSITION_HINT = 'e.g. Server, Cook, Host'

export default function AddEmployeeForm({ onCreated }) {
  const [form, setForm] = useState(EMPTY)
  const [success, setSuccess] = useState(null)
  const { run, submitting, error, fieldErrors } = useApiSubmit()

  function handleChange(event) {
    const { name, value } = event.target
    setForm((prev) => ({ ...prev, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSuccess(null)
    const created = await run(() =>
      createEmployee({
        name: form.name,
        position: form.position,
        hourlyRate: form.hourlyRate === '' ? null : Number(form.hourlyRate),
      }),
    )
    if (created) {
      setForm(EMPTY)
      setSuccess(`Added ${created.name}.`)
      onCreated(created)
    }
  }

  return (
    <form onSubmit={handleSubmit} noValidate>
      <h2>Add employee</h2>

      <Field id="emp-name" label="Name" error={fieldErrors.name}>
        <input name="name" value={form.name} onChange={handleChange} {...fieldProps('emp-name', fieldErrors.name)} />
      </Field>

      <Field id="emp-position" label="Position" error={fieldErrors.position} hint={POSITION_HINT}>
        <input
          name="position"
          value={form.position}
          onChange={handleChange}
          {...fieldProps('emp-position', fieldErrors.position, POSITION_HINT)}
        />
      </Field>

      <Field id="emp-rate" label="Hourly rate ($)" error={fieldErrors.hourlyRate}>
        <input
          name="hourlyRate"
          type="number"
          inputMode="decimal"
          step="0.01"
          value={form.hourlyRate}
          onChange={handleChange}
          {...fieldProps('emp-rate', fieldErrors.hourlyRate)}
        />
      </Field>

      <button type="submit" disabled={submitting}>
        {submitting ? 'Adding…' : 'Add employee'}
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
