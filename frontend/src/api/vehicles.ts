import client from './client';
import type { Vehicle } from '../types';

export interface VehiclePayload {
  make: string;
  model: string;
  year: number;
  licensePlate: string;
  seatingCapacity: number;
  categoryId: number;
}

export type VehicleUpdatePayload = Omit<VehiclePayload, 'licensePlate'>;

export async function listVehicles(): Promise<Vehicle[]> {
  const { data } = await client.get<Vehicle[]>('/vehicles');
  return data;
}

export async function createVehicle(payload: VehiclePayload): Promise<Vehicle> {
  const { data } = await client.post<Vehicle>('/vehicles', payload);
  return data;
}

export async function updateVehicle(id: number, payload: VehicleUpdatePayload): Promise<Vehicle> {
  const { data } = await client.put<Vehicle>(`/vehicles/${id}`, payload);
  return data;
}

export async function deleteVehicle(id: number): Promise<void> {
  await client.delete(`/vehicles/${id}`);
}
