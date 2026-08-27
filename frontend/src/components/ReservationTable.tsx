import type { ReactNode } from 'react';
import type { Reservation } from '../types';
import { StatusBadge } from './StatusBadge';

interface ReservationTableProps {
  reservations: Reservation[];
  renderActions?: (reservation: Reservation) => ReactNode;
}

export function ReservationTable({ reservations, renderActions }: ReservationTableProps) {
  if (reservations.length === 0) {
    return <p className="empty-state">No reservations to show.</p>;
  }

  return (
    <div className="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>Customer</th>
            <th>Vehicle</th>
            <th>Start</th>
            <th>End</th>
            <th>Status</th>
            {renderActions && <th>Actions</th>}
          </tr>
        </thead>
        <tbody>
          {reservations.map((r) => (
            <tr key={r.id}>
              <td>{r.id}</td>
              <td>{r.customerName}</td>
              <td>{r.vehicleMake} {r.vehicleModel} ({r.licensePlate})</td>
              <td>{r.startDate}</td>
              <td>{r.endDate}</td>
              <td><StatusBadge status={r.status} /></td>
              {renderActions && <td>{renderActions(r)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
