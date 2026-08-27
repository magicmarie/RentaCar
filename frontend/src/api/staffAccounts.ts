import client from './client';
import type { StaffAccount } from '../types';

export interface StaffAccountPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export async function listStaffAccounts(): Promise<StaffAccount[]> {
  const { data } = await client.get<StaffAccount[]>('/staff-accounts');
  return data;
}

export async function createStaffAccount(payload: StaffAccountPayload): Promise<StaffAccount> {
  const { data } = await client.post<StaffAccount>('/staff-accounts', payload);
  return data;
}

export async function updateStaffAccount(id: number, firstName: string, lastName: string): Promise<StaffAccount> {
  const { data } = await client.put<StaffAccount>(`/staff-accounts/${id}`, { firstName, lastName });
  return data;
}

export async function deactivateStaffAccount(id: number): Promise<StaffAccount> {
  const { data } = await client.post<StaffAccount>(`/staff-accounts/${id}/deactivate`);
  return data;
}
