import client from './client';
import type { CurrentUser } from '../types';

export interface LoginResponse {
  token: string;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  role: CurrentUser['role'];
}

export async function login(usernameOrEmail: string, password: string): Promise<LoginResponse> {
  const { data } = await client.post<LoginResponse>('/auth/login', { usernameOrEmail, password });
  return data;
}

export interface RegisterCustomerPayload {
  firstName: string;
  lastName: string;
  email: string;
  driverLicenseNumber: string;
  username: string;
  password: string;
}

export async function registerCustomer(payload: RegisterCustomerPayload): Promise<void> {
  await client.post('/auth/register', payload);
}

export async function forgotPassword(email: string): Promise<string> {
  const { data } = await client.post<{ message: string }>('/auth/forgot-password', { email });
  return data.message;
}

export async function resetPassword(token: string, newPassword: string): Promise<string> {
  const { data } = await client.post<{ message: string }>('/auth/reset-password', { token, newPassword });
  return data.message;
}
