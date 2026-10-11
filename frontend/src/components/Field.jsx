/**
 * A labeled form control with an optional hint and error message underneath. The control itself
 * is passed as `children` and should use the same `id`, plus the aria props from fieldProps() in fieldProps.js.
 */
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
