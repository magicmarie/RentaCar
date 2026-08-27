import { useEffect, useState, type FormEvent } from 'react';
import {
  createStaffAccount,
  deactivateStaffAccount,
  listStaffAccounts,
  updateStaffAccount,
} from '../../api/staffAccounts';
import { extractErrorMessage } from '../../api/client';
import { ConfirmDialog } from '../../components/ConfirmDialog';
import type { StaffAccount } from '../../types';

const emptyForm = { firstName: '', lastName: '', email: '', password: '' };

export function StaffAccountsAdminPage() {
  const [staff, setStaff] = useState<StaffAccount[]>([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<StaffAccount | null>(null);

  async function load() {
    try {
      const data = await listStaffAccounts();
      setStaff(data);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  useEffect(() => {
    load();
  }, []);

  function startEdit(account: StaffAccount) {
    setEditingId(account.id);
    setForm({ firstName: account.firstName, lastName: account.lastName, email: account.email, password: '' });
  }

  function resetForm() {
    setEditingId(null);
    setForm(emptyForm);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      if (editingId) {
        await updateStaffAccount(editingId, form.firstName, form.lastName);
      } else {
        await createStaffAccount(form);
      }
      resetForm();
      await load();
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  async function handleDeactivate() {
    if (!deactivateTarget) return;
    try {
      await deactivateStaffAccount(deactivateTarget.id);
      setDeactivateTarget(null);
      await load();
    } catch (err) {
      setError(extractErrorMessage(err));
      setDeactivateTarget(null);
    }
  }

  return (
    <div className="page">
      <h2>Staff accounts</h2>
      {error && <p className="form-error">{error}</p>}

      <form className="card auth-form" onSubmit={handleSubmit}>
        <h3>{editingId ? `Edit staff account #${editingId}` : 'Add a staff account'}</h3>
        <div className="form-row">
          <label>
            First name
            <input value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} required />
          </label>
          <label>
            Last name
            <input value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} required />
          </label>
        </div>
        <label>
          Email {editingId && '(unwritable)'}
          <input
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            disabled={editingId !== null}
            required
          />
        </label>
        {!editingId && (
          <label>
            Initial password
            <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required minLength={6} />
          </label>
        )}
        <div className="row-actions">
          <button type="submit" className="btn btn-primary">{editingId ? 'Save changes' : 'Add staff account'}</button>
          {editingId && <button type="button" className="btn btn-secondary" onClick={resetForm}>Cancel edit</button>}
        </div>
      </form>

      <div className="table-wrapper">
        <table>
          <thead>
            <tr><th>Name</th><th>Email</th><th>Status</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {staff.map((s) => (
              <tr key={s.id}>
                <td>{s.firstName} {s.lastName}</td>
                <td>{s.email}</td>
                <td>{s.active ? 'Active' : 'Deactivated'}</td>
                <td className="row-actions">
                  <button type="button" className="btn btn-secondary" onClick={() => startEdit(s)}>Edit</button>
                  {s.active && (
                    <button type="button" className="btn btn-secondary" onClick={() => setDeactivateTarget(s)}>
                      Deactivate
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <ConfirmDialog
        open={deactivateTarget !== null}
        title="Deactivate staff account"
        message={`Deactivate ${deactivateTarget?.firstName} ${deactivateTarget?.lastName}? They will no longer be able to log in.`}
        confirmLabel="Deactivate"
        onConfirm={handleDeactivate}
        onCancel={() => setDeactivateTarget(null)}
      />
    </div>
  );
}
