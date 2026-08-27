import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { checkInReservation, getReservation } from '../../api/reservations';
import { extractErrorMessage } from '../../api/client';
import { BillSummaryCard } from '../../components/BillSummaryCard';
import type { Bill, Reservation } from '../../types';

function today(): string {
  return new Date().toISOString().slice(0, 10);
}

export function CheckInPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [reservation, setReservation] = useState<Reservation | null>(null);
  const [returnDate, setReturnDate] = useState(today());
  const [conditionNotes, setConditionNotes] = useState('');
  const [maintenanceRequired, setMaintenanceRequired] = useState(false);
  const [bill, setBill] = useState<Bill | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    getReservation(Number(id)).then(setReservation).catch((err) => setError(extractErrorMessage(err)));
  }, [id]);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!id) return;
    setError(null);
    try {
      const result = await checkInReservation(Number(id), returnDate, conditionNotes, maintenanceRequired);
      setBill(result as Bill);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="page">
      <h2>Process return — reservation #{id}</h2>
      {reservation && (
        <p>{reservation.customerName} — {reservation.vehicleMake} {reservation.vehicleModel} ({reservation.licensePlate})</p>
      )}

      {!bill && (
        <form className="card auth-form" onSubmit={handleSubmit}>
          {error && <p className="form-error">{error}</p>}
          <label>
            Return date
            <input type="date" value={returnDate} onChange={(e) => setReturnDate(e.target.value)} required />
          </label>
          <label>
            Condition notes
            <textarea value={conditionNotes} onChange={(e) => setConditionNotes(e.target.value)} rows={3} />
          </label>
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={maintenanceRequired}
              onChange={(e) => setMaintenanceRequired(e.target.checked)}
            />
            Vehicle needs maintenance before it can be rented again
          </label>
          <button type="submit" className="btn btn-primary">Confirm return</button>
          <button type="button" className="btn btn-secondary" onClick={() => navigate('/staff/lookup')}>
            Back to lookup
          </button>
        </form>
      )}

      {bill && (
        <>
          <BillSummaryCard bill={bill} />
          <button type="button" className="btn btn-primary" onClick={() => navigate('/staff/lookup')}>
            Back to lookup
          </button>
        </>
      )}
    </div>
  );
}
