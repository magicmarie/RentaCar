import { useEffect, useState, type FormEvent } from 'react';
import { createVehicle, deleteVehicle, listVehicles, updateVehicle } from '../../api/vehicles';
import { listCategories } from '../../api/categories';
import { extractErrorMessage } from '../../api/client';
import { StatusBadge } from '../../components/StatusBadge';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import type { Category, Vehicle } from '../../types';

const emptyForm = { make: '', model: '', year: new Date().getFullYear(), licensePlate: '', seatingCapacity: 5, categoryId: '' };

export function VehiclesAdminPage() {
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Vehicle | null>(null);

  async function load() {
    try {
      const [v, c] = await Promise.all([listVehicles(), listCategories()]);
      setVehicles(v);
      setCategories(c);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  useEffect(() => {
    load();
  }, []);

  function startEdit(vehicle: Vehicle) {
    setEditingId(vehicle.id);
    setForm({
      make: vehicle.make,
      model: vehicle.model,
      year: vehicle.year,
      licensePlate: vehicle.licensePlate,
      seatingCapacity: vehicle.seatingCapacity,
      categoryId: String(vehicle.categoryId),
    });
  }

  function resetForm() {
    setEditingId(null);
    setForm(emptyForm);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    const payload = {
      make: form.make,
      model: form.model,
      year: Number(form.year),
      seatingCapacity: Number(form.seatingCapacity),
      categoryId: Number(form.categoryId),
    };
    try {
      if (editingId) {
        await updateVehicle(editingId, payload);
      } else {
        await createVehicle({ ...payload, licensePlate: form.licensePlate });
      }
      resetForm();
      await load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    try {
      await deleteVehicle(deleteTarget.id);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setError(extractErrorMessage(err));
      setDeleteTarget(null);
    }
  }

  return (
    <div className="page">
      <h2>Fleet vehicles</h2>
      {error && <p className="form-error">{error}</p>}

      <form className="card auth-form" onSubmit={handleSubmit}>
        <h3>{editingId ? `Edit vehicle #${editingId}` : 'Add a vehicle'}</h3>
        <div className="form-row">
          <label>
            Make
            <input value={form.make} onChange={(e) => setForm({ ...form, make: e.target.value })} required />
          </label>
          <label>
            Model
            <input value={form.model} onChange={(e) => setForm({ ...form, model: e.target.value })} required />
          </label>
        </div>
        <div className="form-row">
          <label>
            Year
            <input type="number" value={form.year} onChange={(e) => setForm({ ...form, year: Number(e.target.value) })} required />
          </label>
          <label>
            Seats
            <input type="number" min={1} value={form.seatingCapacity} onChange={(e) => setForm({ ...form, seatingCapacity: Number(e.target.value) })} required />
          </label>
        </div>
        <label>
          License plate {editingId && '(unwritable)'}
          <input
            value={form.licensePlate}
            onChange={(e) => setForm({ ...form, licensePlate: e.target.value })}
            disabled={editingId !== null}
            required
          />
        </label>
        <label>
          Category
          <select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} required>
            <option value="" disabled>Select a category</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </label>
        <div className="row-actions">
          <button type="submit" className="btn btn-primary">{editingId ? 'Save changes' : 'Add vehicle'}</button>
          {editingId && <button type="button" className="btn btn-secondary" onClick={resetForm}>Cancel edit</button>}
        </div>
      </form>

      <div className="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Make/Model</th><th>Year</th><th>Plate</th><th>Category</th><th>Rate</th><th>Status</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {vehicles.map((v) => (
              <tr key={v.id}>
                <td>{v.make} {v.model}</td>
                <td>{v.year}</td>
                <td>{v.licensePlate}</td>
                <td>{v.categoryName}</td>
                <td>${v.dailyRate.toFixed(2)}</td>
                <td><StatusBadge status={v.status} /></td>
                <td className="row-actions">
                  <button type="button" className="btn btn-secondary" onClick={() => startEdit(v)}>Edit</button>
                  <button type="button" className="btn btn-secondary" onClick={() => setDeleteTarget(v)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Delete vehicle"
        message={`Delete ${deleteTarget?.make} ${deleteTarget?.model} (${deleteTarget?.licensePlate})?`}
        confirmLabel="Delete"
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
