import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { extractErrorMessage } from '../../api/client';

const ROLE_HOME: Record<string, string> = {
  ADMIN: '/admin/dashboard',
  STAFF: '/staff/lookup',
  CUSTOMER: '/search',
};

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [usernameOrEmail, setUsernameOrEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const user = await login(usernameOrEmail, password);
      navigate(ROLE_HOME[user.role] ?? '/');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="card auth-form" onSubmit={handleSubmit}>
        <h2>Log in</h2>
        {error && <p className="form-error">{error}</p>}
        <label>
          Username or email
          <input value={usernameOrEmail} onChange={(e) => setUsernameOrEmail(e.target.value)} required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Logging in...' : 'Log in'}
        </button>
        <div className="auth-links">
          <Link to="/forgot-password">Forgot password?</Link>
          <Link to="/register">Create a customer account</Link>
        </div>
      </form>
    </div>
  );
}
