import { formatHours, formatMoney } from '../format.js'

export default function EmployeeTotalsTable({ employees, totalHours, totalPay }) {
  if (employees.length === 0) {
    return <p className="muted">No employees yet.</p>
  }

  return (
    <table className="totals">
      <thead>
        <tr>
          <th scope="col">Employee</th>
          <th scope="col">Position</th>
          <th scope="col" className="num">Rate</th>
          <th scope="col" className="num">Shifts</th>
          <th scope="col" className="num">Hours</th>
          <th scope="col" className="num">Est. pay</th>
        </tr>
      </thead>
      <tbody>
        {employees.map((e) => (
          <tr key={e.employeeId}>
            <td>{e.name}</td>
            <td>{e.position}</td>
            <td className="num">{formatMoney(e.hourlyRate)}/h</td>
            <td className="num">{e.shiftCount}</td>
            <td className="num">{formatHours(e.hours)}</td>
            <td className="num">{formatMoney(e.estimatedPay)}</td>
          </tr>
        ))}
      </tbody>
      <tfoot>
        <tr>
          <th scope="row" colSpan={4}>Week total</th>
          <td className="num">{formatHours(totalHours)}</td>
          <td className="num">{formatMoney(totalPay)}</td>
        </tr>
      </tfoot>
    </table>
  )
}
