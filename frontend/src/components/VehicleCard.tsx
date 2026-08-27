import type { Vehicle } from '../types';

interface VehicleCardProps {
  vehicle: Vehicle;
  action?: { label: string; onClick: () => void };
}

export function VehicleCard({ vehicle, action }: VehicleCardProps) {
  return (
    <div className="card vehicle-card">
      <div className="vehicle-card-header">
        <h4>{vehicle.make} {vehicle.model}</h4>
        <span className="vehicle-card-category">{vehicle.categoryName}</span>
      </div>
      <dl className="vehicle-card-details">
        <div><dt>Year</dt><dd>{vehicle.year}</dd></div>
        <div><dt>Seats</dt><dd>{vehicle.seatingCapacity}</dd></div>
        <div><dt>Plate</dt><dd>{vehicle.licensePlate}</dd></div>
        <div><dt>Daily rate</dt><dd>${vehicle.dailyRate.toFixed(2)}</dd></div>
      </dl>
      {action && (
        <button type="button" className="btn btn-primary" onClick={action.onClick}>
          {action.label}
        </button>
      )}
    </div>
  );
}
