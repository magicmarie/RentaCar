import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { recommendVehicles, searchAvailability, createReservation } from '../../api/reservations';
import { extractErrorMessage } from '../../api/client';
import { VehicleCard } from '../../components/VehicleCard';
import type { Vehicle } from '../../types';

function defaultDate(offsetDays: number): string {
  const d = new Date();
  d.setDate(d.getDate() + offsetDays);
  return d.toISOString().slice(0, 10);
}

export function SearchVehiclesPage() {
  const navigate = useNavigate();
  const [startDate, setStartDate] = useState(defaultDate(1));
  const [endDate, setEndDate] = useState(defaultDate(3));
  const [passengers, setPassengers] = useState('');
  const [budget, setBudget] = useState('');
  const [useRecommendation, setUseRecommendation] = useState(false);
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSearch(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setMessage(null);
    setLoading(true);
    try {
      const results = useRecommendation
        ? await recommendVehicles(
            startDate,
            endDate,
            passengers ? Number(passengers) : undefined,
            budget ? Number(budget) : undefined,
          )
        : await searchAvailability(startDate, endDate);
      setVehicles(results);
      if (results.length === 0) {
        setMessage('No vehicles match your search.');
      }
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleReserve(vehicleId: number) {
    setError(null);
    try {
      await createReservation(vehicleId, startDate, endDate);
      navigate('/my-reservations');
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  return (
    <div className="page">
      <h2>Find a vehicle</h2>
      <form className="card search-form" onSubmit={handleSearch}>
        <div className="form-row">
          <label>
            Start date
            <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} required />
          </label>
          <label>
            End date
            <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} required />
          </label>
        </div>
        <label className="checkbox-label">
          <input type="checkbox" checked={useRecommendation} onChange={(e) => setUseRecommendation(e.target.checked)} />
          Get a recommendation based on my trip
        </label>
        {useRecommendation && (
          <div className="form-row">
            <label>
              Passengers
              <input type="number" min={1} value={passengers} onChange={(e) => setPassengers(e.target.value)} />
            </label>
            <label>
              Max daily budget ($)
              <input type="number" min={0} value={budget} onChange={(e) => setBudget(e.target.value)} />
            </label>
          </div>
        )}
        {error && <p className="form-error">{error}</p>}
        {message && <p className="form-success">{message}</p>}
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'Searching...' : 'Search'}
        </button>
      </form>

      <div className="vehicle-grid">
        {vehicles.map((vehicle) => (
          <VehicleCard
            key={vehicle.id}
            vehicle={vehicle}
            action={{ label: 'Reserve', onClick: () => handleReserve(vehicle.id) }}
          />
        ))}
      </div>
    </div>
  );
}
