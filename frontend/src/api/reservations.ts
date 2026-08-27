import client from './client';
import type { Reservation, ReservationStatus, Vehicle } from '../types';

export async function searchAvailability(startDate: string, endDate: string, categoryId?: number): Promise<Vehicle[]> {
  const { data } = await client.get<Vehicle[]>('/reservations/available', {
    params: { startDate, endDate, categoryId },
  });
  return data;
}

export async function recommendVehicles(
  startDate: string,
  endDate: string,
  passengers?: number,
  budget?: number,
): Promise<Vehicle[]> {
  const { data } = await client.get<Vehicle[]>('/reservations/recommend', {
    params: { startDate, endDate, passengers, budget },
  });
  return data;
}

export async function createReservation(vehicleId: number, startDate: string, endDate: string): Promise<Reservation> {
  const { data } = await client.post<Reservation>('/reservations', { vehicleId, startDate, endDate });
  return data;
}

export async function getMyReservations(): Promise<Reservation[]> {
  const { data } = await client.get<Reservation[]>('/reservations/me');
  return data;
}

export async function searchReservations(status?: ReservationStatus, query?: string): Promise<Reservation[]> {
  const { data } = await client.get<Reservation[]>('/reservations', {
    params: { status, query: query || undefined },
  });
  return data;
}

export async function getReservation(id: number): Promise<Reservation> {
  const { data } = await client.get<Reservation>(`/reservations/${id}`);
  return data;
}

export async function cancelReservation(id: number): Promise<Reservation> {
  const { data } = await client.post<Reservation>(`/reservations/${id}/cancel`);
  return data;
}

export async function checkOutReservation(id: number, pickupDateTime?: string): Promise<Reservation> {
  const { data } = await client.post<Reservation>(`/checkout/${id}`, { pickupDateTime });
  return data;
}

export async function checkInReservation(
  id: number,
  returnDate: string,
  conditionNotes: string,
  maintenanceRequired: boolean,
) {
  const { data } = await client.post(`/checkin/${id}`, { returnDate, conditionNotes, maintenanceRequired });
  return data;
}
