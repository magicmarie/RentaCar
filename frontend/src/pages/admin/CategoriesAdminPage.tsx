import { useEffect, useState, type FormEvent } from 'react';
import { createCategory, deleteCategory, listCategories, updateCategoryRate } from '../../api/categories';
import { extractErrorMessage } from '../../api/client';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import type { Category } from '../../types';

export function CategoriesAdminPage() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [name, setName] = useState('');
  const [dailyRate, setDailyRate] = useState('');
  const [rateEdits, setRateEdits] = useState<Record<number, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Category | null>(null);

  async function load() {
    try {
      const data = await listCategories();
      setCategories(data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  useEffect(() => {
    load();
  }, []);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await createCategory(name, Number(dailyRate));
      setName('');
      setDailyRate('');
      await load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleRateUpdate(category: Category) {
    const newRate = rateEdits[category.id];
    if (!newRate) return;
    setError(null);
    try {
      await updateCategoryRate(category.id, Number(newRate));
      await load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleDelete() {
    if (!deleteTarget) return;
    try {
      await deleteCategory(deleteTarget.id);
      setDeleteTarget(null);
      await load();
    } catch (err) {
      setError(extractErrorMessage(err));
      setDeleteTarget(null);
    }
  }

  return (
    <div className="page">
      <h2>Vehicle categories</h2>
      {error && <p className="form-error">{error}</p>}

      <form className="card auth-form" onSubmit={handleCreate}>
        <h3>Add a category</h3>
        <label>
          Name
          <input value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>
          Daily rate ($)
          <input type="number" min={0} step="0.01" value={dailyRate} onChange={(e) => setDailyRate(e.target.value)} required />
        </label>
        <button type="submit" className="btn btn-primary">Add category</button>
      </form>

      <div className="table-wrapper">
        <table>
          <thead>
            <tr><th>Name (unwritable)</th><th>Daily rate</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {categories.map((c) => (
              <tr key={c.id}>
                <td>{c.name}</td>
                <td>
                  <input
                    type="number"
                    min={0}
                    step="0.01"
                    className="inline-input"
                    value={rateEdits[c.id] ?? c.dailyRate}
                    onChange={(e) => setRateEdits({ ...rateEdits, [c.id]: e.target.value })}
                  />
                </td>
                <td className="row-actions">
                  <button type="button" className="btn btn-secondary" onClick={() => handleRateUpdate(c)}>Save rate</button>
                  <button type="button" className="btn btn-secondary" onClick={() => setDeleteTarget(c)}>Delete</button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Delete category"
        message={`Delete category "${deleteTarget?.name}"?`}
        confirmLabel="Delete"
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />
    </div>
  );
}
