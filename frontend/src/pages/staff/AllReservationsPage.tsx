import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { cancelReservation, searchReservations } from '../../api/reservations';
import { extractErrorMessage } from '../../api/client';
import { ReservationTable } from '../../components/ReservationTable';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import type { Reservation, ReservationStatus } from '../../types';

const STATUSES: ReservationStatus[] = ['PENDING', 'CONFIRMED', 'CHECKED_OUT', 'COMPLETED', 'CANCELLED'];

export function AllReservationsPage() {
  const navigate = useNavigate();
  const [status, setStatus] = useState<ReservationStatus | ''>('');
  const [query, setQuery] = useState('');
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<Reservation | null>(null);

  async function load() {
    setError(null);
    try {
      const data = await searchReservations(status || undefined, query);
      setReservations(data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  useEffect(() => {
    load();
  }, []);

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    load();
  }

  async function handleCancel() {
    if (!cancelTarget) return;
    try {
      await cancelReservation(cancelTarget.id);
      await load();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setCancelTarget(null);
    }
  }

  return (
    <div className="page">
      <h2>All reservations</h2>

      <form className="card search-form" onSubmit={handleSubmit}>
        <label>
          Status
          <select value={status} onChange={(e) => setStatus(e.target.value as ReservationStatus | '')}>
            <option value="">All statuses</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>{s.replace(/_/g, ' ')}</option>
            ))}
          </select>
        </label>
        <label>
          Customer or plate
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Name, email, or license plate"
          />
        </label>
        <button type="submit" className="btn btn-primary">Search</button>
      </form>

      {error && <p className="form-error">{error}</p>}

      <ReservationTable
        reservations={reservations}
        renderActions={(reservation) => (
          <div className="row-actions">
            {(reservation.status === 'PENDING' || reservation.status === 'CONFIRMED') && (
              <>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => navigate(`/staff/checkout/${reservation.id}`)}
                >
                  Check out
                </button>
                <button type="button" className="btn btn-secondary" onClick={() => setCancelTarget(reservation)}>
                  Cancel
                </button>
              </>
            )}
            {reservation.status === 'CHECKED_OUT' && (
              <button
                type="button"
                className="btn btn-secondary"
                onClick={() => navigate(`/staff/checkin/${reservation.id}`)}
              >
                Process return
              </button>
            )}
          </div>
        )}
      />

      <ConfirmDialog
        open={cancelTarget !== null}
        title="Cancel reservation"
        message={`Cancel reservation #${cancelTarget?.id} on behalf of the customer?`}
        confirmLabel="Cancel reservation"
        onConfirm={handleCancel}
        onCancel={() => setCancelTarget(null)}
      />
    </div>
  );
}
