/** Accessibility props linking a control to its error or hint, so screen readers announce them. */
export function fieldProps(id, error, hint) {
  return {
    id,
    'aria-invalid': error ? true : undefined,
    'aria-describedby': error ? `${id}-error` : hint ? `${id}-hint` : undefined,
  }
}
