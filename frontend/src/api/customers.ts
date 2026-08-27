import client from './client';
import type { CustomerProfile } from '../types';

export async function getMyProfile(): Promise<CustomerProfile> {
  const { data } = await client.get<CustomerProfile>('/customers/me');
  return data;
}

export async function updateMyProfile(firstName: string, lastName: string): Promise<CustomerProfile> {
  const { data } = await client.put<CustomerProfile>('/customers/me', { firstName, lastName });
  return data;
}
