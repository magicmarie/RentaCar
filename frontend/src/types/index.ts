export type Role = 'ADMIN' | 'STAFF' | 'CUSTOMER';

export type VehicleStatus = 'AVAILABLE' | 'RESERVED' | 'RENTED' | 'UNDER_MAINTENANCE';

export type ReservationStatus = 'PENDING' | 'CONFIRMED' | 'CHECKED_OUT' | 'COMPLETED' | 'CANCELLED';

export interface CurrentUser {
  token: string;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
}

export interface Category {
  id: number;
  name: string;
  dailyRate: number;
}

export interface Vehicle {
  id: number;
  make: string;
  model: string;
  year: number;
  licensePlate: string;
  seatingCapacity: number;
  categoryId: number;
  categoryName: string;
  dailyRate: number;
  status: VehicleStatus;
}

export interface StaffAccount {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  active: boolean;
}

export interface CustomerProfile {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  username: string;
  driverLicenseNumber: string;
}

export interface Reservation {
  id: number;
  customerId: number;
  customerName: string;
  vehicleId: number;
  vehicleMake: string;
  vehicleModel: string;
  licensePlate: string;
  categoryName: string;
  startDate: string;
  endDate: string;
  status: ReservationStatus;
  pickupDateTime: string | null;
  returnDate: string | null;
  conditionNotes: string | null;
}

export interface Bill {
  id: number;
  reservationId: number;
  vehicleMake: string;
  vehicleModel: string;
  licensePlate: string;
  pickupDate: string;
  returnDate: string;
  rentalDays: number;
  dailyRate: number;
  totalAmount: number;
  generatedAt: string;
}

export interface DashboardData {
  vehicleCountsByStatus: Record<VehicleStatus, number>;
  activeRentals: Reservation[];
  upcomingReservations: Reservation[];
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  message: string;
}
