import type { Bill } from '../types';

export function BillSummaryCard({ bill }: { bill: Bill }) {
  return (
    <div className="card bill-summary">
      <h4>Bill Summary</h4>
      <dl>
        <div><dt>Vehicle</dt><dd>{bill.vehicleMake} {bill.vehicleModel} ({bill.licensePlate})</dd></div>
        <div><dt>Pickup date</dt><dd>{bill.pickupDate}</dd></div>
        <div><dt>Return date</dt><dd>{bill.returnDate}</dd></div>
        <div><dt>Rental days</dt><dd>{bill.rentalDays}</dd></div>
        <div><dt>Daily rate</dt><dd>${bill.dailyRate.toFixed(2)}</dd></div>
        <div className="bill-total"><dt>Total amount</dt><dd>${bill.totalAmount.toFixed(2)}</dd></div>
      </dl>
    </div>
  );
}
