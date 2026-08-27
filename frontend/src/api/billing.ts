import client from './client';
import type { Bill } from '../types';

export async function getBillForReservation(reservationId: number): Promise<Bill> {
  const { data } = await client.get<Bill>(`/bills/reservation/${reservationId}`);
  return data;
}
