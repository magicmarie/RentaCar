import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { registerCustomer } from '../../api/auth';
import { extractErrorMessage } from '../../api/client';

const emptyForm = {
  firstName: '',
  lastName: '',
  email: '',
  driverLicenseNumber: '',
  username: '',
  password: '',
};

export function RegisterPage() {
  const navigate = useNavigate();
  const [form, setForm] = useState(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  function update<K extends keyof typeof form>(key: K, value: string) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await registerCustomer(form);
      navigate('/login');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="card auth-form" onSubmit={handleSubmit}>
        <h2>Create a customer account</h2>
        {error && <p className="form-error">{error}</p>}
        <label>
          First name
          <input value={form.firstName} onChange={(e) => update('firstName', e.target.value)} required />
        </label>
        <label>
          Last name
          <input value={form.lastName} onChange={(e) => update('lastName', e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" value={form.email} onChange={(e) => update('email', e.target.value)} required />
        </label>
        <label>
          Driver's license number
          <input value={form.driverLicenseNumber} onChange={(e) => update('driverLicenseNumber', e.target.value)} required />
        </label>
        <label>
          Username
          <input value={form.username} onChange={(e) => update('username', e.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={form.password} onChange={(e) => update('password', e.target.value)} required minLength={6} />
        </label>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Creating account...' : 'Register'}
        </button>
        <div className="auth-links">
          <Link to="/login">Already have an account? Log in</Link>
        </div>
      </form>
    </div>
  );
}
