import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { cancelReservation, getReservation } from '../../api/reservations';
import { extractErrorMessage } from '../../api/client';
import { StatusBadge } from '../../components/StatusBadge';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import type { Reservation } from '../../types';

export function ReservationLookupPage() {
  const navigate = useNavigate();
  const [reservationId, setReservationId] = useState('');
  const [reservation, setReservation] = useState<Reservation | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmCancel, setConfirmCancel] = useState(false);

  async function handleLookup(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setReservation(null);
    try {
      const data = await getReservation(Number(reservationId));
      setReservation(data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleCancel() {
    if (!reservation) return;
    try {
      const updated = await cancelReservation(reservation.id);
      setReservation(updated);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setConfirmCancel(false);
    }
  }

  return (
    <div className="page">
      <h2>Reservation lookup</h2>
      <form className="card search-form" onSubmit={handleLookup}>
        <label>
          Reservation ID
          <input value={reservationId} onChange={(e) => setReservationId(e.target.value)} required />
        </label>
        <button type="submit" className="btn btn-primary">Look up</button>
      </form>

      {error && <p className="form-error">{error}</p>}

      {reservation && (
        <div className="card">
          <h3>Reservation #{reservation.id} <StatusBadge status={reservation.status} /></h3>
          <dl>
            <div><dt>Customer</dt><dd>{reservation.customerName}</dd></div>
            <div><dt>Vehicle</dt><dd>{reservation.vehicleMake} {reservation.vehicleModel} ({reservation.licensePlate})</dd></div>
            <div><dt>Dates</dt><dd>{reservation.startDate} to {reservation.endDate}</dd></div>
            {reservation.pickupDateTime && <div><dt>Picked up</dt><dd>{reservation.pickupDateTime}</dd></div>}
          </dl>
          <div className="row-actions">
            {(reservation.status === 'PENDING' || reservation.status === 'CONFIRMED') && (
              <>
                <button type="button" className="btn btn-primary" onClick={() => navigate(`/staff/checkout/${reservation.id}`)}>
                  Check out
                </button>
                <button type="button" className="btn btn-secondary" onClick={() => setConfirmCancel(true)}>
                  Cancel reservation
                </button>
              </>
            )}
            {reservation.status === 'CHECKED_OUT' && (
              <button type="button" className="btn btn-primary" onClick={() => navigate(`/staff/checkin/${reservation.id}`)}>
                Process return
              </button>
            )}
          </div>
        </div>
      )}

      <ConfirmDialog
        open={confirmCancel}
        title="Cancel reservation"
        message={`Cancel reservation #${reservation?.id} on behalf of the customer?`}
        confirmLabel="Cancel reservation"
        onConfirm={handleCancel}
        onCancel={() => setConfirmCancel(false)}
      />
    </div>
  );
}
