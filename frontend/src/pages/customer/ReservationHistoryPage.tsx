import { useEffect, useState } from 'react';
import { cancelReservation, getMyReservations } from '../../api/reservations';
import { getBillForReservation } from '../../api/billing';
import { extractErrorMessage } from '../../api/client';
import { ReservationTable } from '../../components/ReservationTable';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import { BillSummaryCard } from '../../components/BillSummaryCard';
import type { Bill, Reservation } from '../../types';

export function ReservationHistoryPage() {
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [cancelTarget, setCancelTarget] = useState<Reservation | null>(null);
  const [bill, setBill] = useState<Bill | null>(null);
  const [billError, setBillError] = useState<string | null>(null);

  async function load() {
    try {
      const data = await getMyReservations();
      setReservations(data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleConfirmCancel() {
    if (!cancelTarget) return;
    try {
      await cancelReservation(cancelTarget.id);
      setCancelTarget(null);
      await load();
    } catch (err) {
      setError(extractErrorMessage(err));
      setCancelTarget(null);
    }
  }

  async function handleViewBill(reservation: Reservation) {
    setBillError(null);
    setBill(null);
    try {
      const data = await getBillForReservation(reservation.id);
      setBill(data);
    } catch (err) {
      setBillError(extractErrorMessage(err));
    }
  }

  return (
    <div className="page">
      <h2>My reservations</h2>
      {error && <p className="form-error">{error}</p>}
      <ReservationTable
        reservations={reservations}
        renderActions={(r) => (
          <div className="row-actions">
            {(r.status === 'PENDING' || r.status === 'CONFIRMED') && (
              <button type="button" className="btn btn-secondary" onClick={() => setCancelTarget(r)}>
                Cancel
              </button>
            )}
            {r.status === 'COMPLETED' && (
              <button type="button" className="btn btn-secondary" onClick={() => handleViewBill(r)}>
                View bill
              </button>
            )}
          </div>
        )}
      />

      {billError && <p className="form-error">{billError}</p>}
      {bill && <BillSummaryCard bill={bill} />}

      <ConfirmDialog
        open={cancelTarget !== null}
        title="Cancel reservation"
        message={`Cancel reservation #${cancelTarget?.id}? This cannot be undone.`}
        confirmLabel="Cancel reservation"
        onConfirm={handleConfirmCancel}
        onCancel={() => setCancelTarget(null)}
      />
    </div>
  );
}
