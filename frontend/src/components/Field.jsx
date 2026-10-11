// The child control should spread fieldProps(id, error, hint) so the message is announced.
export default function Field({ id, label, error, hint, children }) {
  return (
    <div className={'field' + (error ? ' has-error' : '')}>
      <label htmlFor={id}>{label}</label>
      {children}
      {error && (
        <p id={`${id}-error`} className="field-error">
          {label} {error}
        </p>
      )}
      {!error && hint && (
        <p id={`${id}-hint`} className="field-hint">
          {hint}
        </p>
      )}
    </div>
  )
}
