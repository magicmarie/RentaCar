import client from './client';
import type { Category } from '../types';

export async function listCategories(): Promise<Category[]> {
  const { data } = await client.get<Category[]>('/categories');
  return data;
}

export async function createCategory(name: string, dailyRate: number): Promise<Category> {
  const { data } = await client.post<Category>('/categories', { name, dailyRate });
  return data;
}

export async function updateCategoryRate(id: number, dailyRate: number): Promise<Category> {
  const { data } = await client.put<Category>(`/categories/${id}/rate`, { dailyRate });
  return data;
}

export async function deleteCategory(id: number): Promise<void> {
  await client.delete(`/categories/${id}`);
}
