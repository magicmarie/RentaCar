import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../../api/auth';
import { extractErrorMessage } from '../../api/client';

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      const msg = await forgotPassword(email);
      setMessage(msg);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-page">
      <form className="card auth-form" onSubmit={handleSubmit}>
        <h2>Forgot password</h2>
        {error && <p className="form-error">{error}</p>}
        {message && <p className="form-success">{message}</p>}
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
        </label>
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Sending...' : 'Send reset link'}
        </button>
        <div className="auth-links">
          <Link to="/reset-password">Have a reset token?</Link>
          <Link to="/login">Back to login</Link>
        </div>
      </form>
    </div>
  );
}
