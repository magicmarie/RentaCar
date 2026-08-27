import { useEffect, useState, type FormEvent } from 'react';
import { getMyProfile, updateMyProfile } from '../../api/customers';
import { extractErrorMessage } from '../../api/client';
import type { CustomerProfile } from '../../types';

export function ProfilePage() {
  const [profile, setProfile] = useState<CustomerProfile | null>(null);
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    getMyProfile()
      .then((data) => {
        setProfile(data);
        setFirstName(data.firstName);
        setLastName(data.lastName);
      })
      .catch((err) => setError(extractErrorMessage(err)));
  }, []);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setMessage(null);
    try {
      const updated = await updateMyProfile(firstName, lastName);
      setProfile(updated);
      setMessage('Profile updated successfully');
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  }

  if (!profile) {
    return <div className="page">{error ? <p className="form-error">{error}</p> : <p>Loading...</p>}</div>;
  }

  return (
    <div className="page">
      <h2>My profile</h2>
      <form className="card auth-form" onSubmit={handleSubmit}>
        {error && <p className="form-error">{error}</p>}
        {message && <p className="form-success">{message}</p>}
        <label>
          First name
          <input value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
        </label>
        <label>
          Last name
          <input value={lastName} onChange={(e) => setLastName(e.target.value)} required />
        </label>
        <label>
          Email (unwritable)
          <input value={profile.email} disabled />
        </label>
        <label>
          Driver's license number (unwritable)
          <input value={profile.driverLicenseNumber} disabled />
        </label>
        <button type="submit" className="btn btn-primary">Save changes</button>
      </form>
    </div>
  );
}
