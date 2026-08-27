import { useEffect, useState } from 'react';
import { getDashboard } from '../../api/dashboard';
import { extractErrorMessage } from '../../api/client';
import { ReservationTable } from '../../components/ReservationTable';
import type { DashboardData } from '../../types';

export function DashboardAdminPage() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getDashboard().then(setData).catch((err) => setError(extractErrorMessage(err)));
  }, []);

  if (error) return <div className="page"><p className="form-error">{error}</p></div>;
  if (!data) return <div className="page"><p>Loading...</p></div>;

  return (
    <div className="page">
      <h2>Fleet dashboard</h2>

      <div className="stat-grid">
        {Object.entries(data.vehicleCountsByStatus).map(([status, count]) => (
          <div className="card stat-tile" key={status}>
            <span className="stat-value">{count}</span>
            <span className="stat-label">{status.replace(/_/g, ' ')}</span>
          </div>
        ))}
      </div>

      <h3>Active rentals</h3>
      <ReservationTable reservations={data.activeRentals} />

      <h3>Upcoming reservations</h3>
      <ReservationTable reservations={data.upcomingReservations} />
    </div>
  );
}
