import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { checkOutReservation, getReservation } from '../../api/reservations';
import { extractErrorMessage } from '../../api/client';
import type { Reservation } from '../../types';

function nowForInput(): string {
  const d = new Date();
  d.setSeconds(0, 0);
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  return d.toISOString().slice(0, 16);
}

export function CheckOutPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [reservation, setReservation] = useState<Reservation | null>(null);
  const [pickupDateTime, setPickupDateTime] = useState(nowForInput());
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    getReservation(Number(id)).then(setReservation).catch((err) => setError(extractErrorMessage(err)));
  }, [id]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!id) return;
    setError(null);
    try {
      await checkOutReservation(Number(id), pickupDateTime);
      setMessage('Vehicle checked out successfully');
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="page">
      <h2>Check out reservation #{id}</h2>
      {reservation && (
        <p>{reservation.customerName} — {reservation.vehicleMake} {reservation.vehicleModel} ({reservation.licensePlate})</p>
      )}
      {!message && (
        <form className="card auth-form" onSubmit={handleSubmit}>
          {error && <p className="form-error">{error}</p>}
          <label>
            Pick-up date and time
            <input type="datetime-local" value={pickupDateTime} onChange={(e) => setPickupDateTime(e.target.value)} required />
          </label>
          <button type="submit" className="btn btn-primary">Confirm check-out</button>
          <button type="button" className="btn btn-secondary" onClick={() => navigate('/staff/lookup')}>
            Back to lookup
          </button>
        </form>
      )}

      {message && (
        <div className="card">
          <p className="form-success">{message}</p>
          <button type="button" className="btn btn-primary" onClick={() => navigate('/staff/lookup')}>
            Back to lookup
          </button>
        </div>
      )}
    </div>
  );
}
